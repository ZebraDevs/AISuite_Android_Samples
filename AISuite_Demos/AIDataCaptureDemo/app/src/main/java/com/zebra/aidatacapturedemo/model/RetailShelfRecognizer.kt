package com.zebra.aidatacapturedemo.model

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.FeatureExtractor
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.data.toProductData
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.databaseFile
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Replaces the old ModuleRecognizer-based RetailShelfAnalyzer. Mirrors
 * ProductEnrollmentRecognition's architecture exactly — separate Localizer / FeatureExtractor /
 * Recognizer calls instead of the combined native ModuleRecognizer, which leaks its
 * already-allocated Localizer/FeatureExtractor buffers whenever it fails to build a search index
 * (e.g. no products enrolled), with no handle returned to dispose them.
 *
 * Like ProductEnrollmentRecognition, live preview runs the full pipeline (Localizer +
 * FeatureExtractor + Recognizer) on every frame via [recognizeProducts], so recognized product
 * names are visible while scanning, not just after capture — same detection/recognition logic as
 * Enrollment's executeProductRecognition() (cls==1 filter, sort-by-area, MAX_PRODUCTS_PER_FRAME
 * cap, generateDescriptors -> findRecognitions -> toProductData), just reused via a shared
 * private function instead of duplicated. The separated Localizer/FeatureExtractor/Recognizer
 * pipeline (vs. the old combined ModuleRecognizer) is what makes running this on every frame safe
 * from the native-leak risk noted above.
 */
class RetailShelfRecognizer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel,
    private val cacheDir: String
) : ImageAnalysis.Analyzer {

    private companion object {
        const val MAX_PRODUCTS_PER_FRAME = 30
        const val TAG = "RetailShelfRecognizer"
    }

    private var localizer: Localizer? = null
    private var extractor: FeatureExtractor? = null
    private var recognizer: Recognizer? = null

    private var mIsStopPreviewAnalysisRequested = false
    private var isRetiring = false
    private var disposeJob: Job? = null

    /** Guards localizer/extractor/recognizer so analyze(), executeHighRes() and the deferred
     * dispose in deinitialize() never touch the native objects concurrently — doing so crashes
     * natively with no Java stack trace, which becomes far more likely at 8MP/high input size
     * where each native call takes long enough for a teardown or capture to land mid-call. */
    private val pipelineLock = Mutex()

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // Dedicated single-thread executors per concern, model init
    // runs on its own thread so it never contends with live-frame inference for a shared pool
    // slot, and native calls stay implicitly serialized without extra locking.
    private val initExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val inferenceExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val initGeneration = AtomicInteger(0)
    private val pendingFutures =
        java.util.Collections.synchronizedList(mutableListOf<CompletableFuture<*>>())

    private fun isProductDatabaseAvailable(): Boolean {
        val dbFile = File(cacheDir, databaseFile)
        return dbFile.exists() && dbFile.length() > 0
    }

    /**
     * Init steps are staged sequentially (each one only starts once the previous one has
     * settled) rather than fired all at once, avoiding DSP/thread-pool contention between
     * Localizer and FeatureExtractor.
     */
    fun initialize() {
        val myGeneration = initGeneration.incrementAndGet()

        localizer?.dispose(); localizer = null
        extractor?.dispose(); extractor = null
        recognizer?.dispose(); recognizer = null
        updateRetailShelfModelDemoReady(false)

        if (!isProductDatabaseAvailable()) {
            viewModel.updateRecognizerInitError("No products enrolled. Enroll products using Product & Shelf Enrollment")
            return
        }

        try {
            initLocalizer(myGeneration)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "initialize() ran out of memory loading models:${e.message}")
            viewModel.updateRecognizerInitError(
                "Not enough memory to load the model at the current settings. Close other apps and try again, or lower resolution/model input size."
            )
            viewModel.handleMemoryPressureDetected("model init")
        }
    }

    private fun processorOrder(): Array<Int> =
        when (uiState.value.retailShelfSettings.commonSettings.processorSelectedIndex) {
            0 -> arrayOf(2, 0, 1) // AUTO
            1 -> arrayOf(2) // DSP
            2 -> arrayOf(1) // GPU
            3 -> arrayOf(0) // CPU
            else -> arrayOf(2, 0, 1)
        }

    private fun initLocalizer(myGeneration: Int) {
        val settings = Localizer.Settings("product-and-shelf-recognizer")
        settings.inferencerOptions.runtimeProcessorOrder = processorOrder()
        settings.inferencerOptions.defaultDims.width =
            uiState.value.retailShelfSettings.commonSettings.inputSizeSelected
        settings.inferencerOptions.defaultDims.height =
            uiState.value.retailShelfSettings.commonSettings.inputSizeSelected
        try {
            val future = Localizer.getLocalizer(settings, initExecutor)
                .thenAccept { instance ->
                    if (myGeneration != initGeneration.get()) {
                        instance?.dispose()
                        return@thenAccept
                    }
                    localizer = instance
                    // NOTE: readiness is intentionally NOT flipped here. For the Recognizer
                    // usecase we must not allow scanning until the Recognizer itself succeeds —
                    // otherwise, with no products enrolled, the localizer loads, Start enables,
                    // and the user scans before the recognizer fails. Ready is flipped in
                    // initRecognizer()'s success callback (the last staged step) instead.
                    Log.i(TAG, "Localizer init Success")
                }.exceptionally { e ->
                    if (myGeneration != initGeneration.get()) return@exceptionally null
                    Log.e(TAG, "Localizer init Failed -> ${e.message}")
                    if (e.message?.contains("Given runtimes are not available") ==
                        true
                    ) {
                        viewModel.updateToastMessage(
                            message = "Selected inference type is not supported onthis device . Switching to Auto -selectfor optimal performance ."
                        )
                        viewModel.updateSelectedProcessor(0)
                        viewModel.saveSettings()
                    }
                    null
                }
            pendingFutures.add(future)
            // Staged init: only start FeatureExtractor once Localizer has settled.
            future.whenComplete { _, _ ->
                if (myGeneration == initGeneration.get()) {
                    initFeatureExtractor(myGeneration)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Localizer init Failed -> ${e.message}")
            initFeatureExtractor(myGeneration)
        } catch (e: RuntimeException) {
            Log.e(TAG, "DB empty -> ${e.message}")
            initFeatureExtractor(myGeneration)
        } catch (e: Exception) {
            Log.e(TAG, "Localizer init Failed -> ${e.message}")
            initFeatureExtractor(myGeneration)
        }
    }

    private fun initFeatureExtractor(myGeneration: Int) {
        val settings = FeatureExtractor.Settings("product-and-shelf-recognizer")
        settings.inferencerOptions.runtimeProcessorOrder = processorOrder()
        try {
            val future = FeatureExtractor.getFeatureExtractor(
                settings,
                initExecutor
            ).thenAccept { instance ->
                if (myGeneration != initGeneration.get()) {
                    instance?.dispose()
                    return@thenAccept
                }
                extractor = instance
                Log.i(TAG, "FeatureExtractor init Success")
            }.exceptionally { e ->
                if (myGeneration != initGeneration.get()) return@exceptionally null
                Log.e(TAG, "FeatureExtractor init Failed -> ${e.message}")
                null
            }
            pendingFutures.add(future)
            // Staged init: only start Recognizer once FeatureExtractor has settled.
            future.whenComplete { _, _ ->
                if (myGeneration == initGeneration.get()) {
                    initRecognizer(myGeneration)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "FeatureExtractor init Failed -> ${e.message}")
            initRecognizer(myGeneration)
        } catch (e: RuntimeException) {
            Log.e(TAG, "DB empty -> ${e.message}")
            initRecognizer(myGeneration)
        } catch (e: Exception) {
            Log.e(TAG, "FeatureExtractor init Failed -> ${e.message}")
            initRecognizer(myGeneration)
        }
    }

    private fun initRecognizer(myGeneration: Int) {
        val settings = Recognizer.SettingsDb()
        settings.dbSource = Paths.get(cacheDir, databaseFile).toString()
        settings.indexDimensions = 768
        try {
            val future = Recognizer.getRecognizer(settings, initExecutor)
                .thenAccept { instance ->
                    if (myGeneration != initGeneration.get()) {
                        instance?.dispose()
                        return@thenAccept
                    }
                    recognizer = instance
                    // Only now — with localizer + extractor + recognizer all loaded and products
                    // present — is the usecase truly ready to scan.
                    updateRetailShelfModelDemoReady(true)
                    Log.i(TAG, "Recognizer init Success")
                }.exceptionally { e ->
                    if (myGeneration != initGeneration.get()) return@exceptionally null
                    Log.e(TAG, "Recognizer init Failed -> ${e.message}")
                    if ((e.message?.contains("No DB product data available to build a search index!") == true) ||
                        (e.message?.contains("Cannot open DB file") == true)
                    ) {
                        viewModel.updateRecognizerInitError("No products enrolled. Enroll products using Product & Shelf Enrollment")
                    }
                    null
                }
            pendingFutures.add(future)
        } catch (e: IOException) {
            Log.e(TAG, "Recognizer init Failed -> ${e.message}")
        } catch (e: RuntimeException) {
            Log.e(TAG, "DB empty -> ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Recognizer init Failed -> ${e.message}")
        }
    }

    /** Called from the main thread (e.g. use-case toggle / applySettings), so this must not
     * block. Native dispose is deferred to [disposeJob], which waits for any in-flight
     * analyze()/executeHighRes() call on this instance to finish first — disposing while a call
     * is still inside native detect()/generateDescriptors()/findRecognitions() crashes natively
     * with no Java stack trace. */
    fun deinitialize() {
        initGeneration.incrementAndGet()
        isRetiring = true
        disposeJob = scope.launch {
            pipelineLock.withLock {
                localizer?.dispose(); localizer = null
                extractor?.dispose(); extractor = null
                recognizer?.dispose(); recognizer = null
            }
        }
    }

    /** Blocks until in-flight native inits from a superseded initialize() call, and this
     * instance's deferred dispose from deinitialize(), have settled.
     * Call off the main thread before starting a new initialize() on this retired instance. */
    fun awaitPendingInit() {
        val toAwait: List<CompletableFuture<*>>
        synchronized(pendingFutures) {
            toAwait = pendingFutures.toList()
            pendingFutures.clear()
        }
        toAwait.forEach { future ->
            try {
                future.get(10, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.w(TAG, "Timed out waiting for a superseded init to settle:${e.message}")
            }
        }
        disposeJob?.let { pendingDispose ->
            try {
                runBlocking {
                    withTimeoutOrNull(10_000) { pendingDispose.join() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Timed out waiting for deferred dispose to settle:${e.message}")
            }
        }
        // This instance is now fully retired — nothing else will touch its native objects
        // or executors, so it's safe to release them and the coroutine job.
        initExecutor.shutdown()
        inferenceExecutor.shutdown()
        job.cancel()
    }

    private fun updateRetailShelfModelDemoReady(isReady: Boolean) {
        if (isReady) {
            // Require the FULL pipeline — localizer + extractor + recognizer — before allowing
            // scan. If no products are enrolled, the recognizer fails and this stays false, so
            // Start stays disabled and the "No products enrolled" error is surfaced instead.
            if (localizer != null && extractor != null && recognizer != null) {
                scope.launch {
                    delay(250)
                    viewModel.updateRetailShelfModelDemoReady(isReady = true)
                }
            }
        } else {
            viewModel.updateRetailShelfModelDemoReady(isReady = false)
        }
    }

    override fun analyze(image: ImageProxy) {
        if (!uiState.value.isRetailShelfModelDemoReady ||
            mIsStopPreviewAnalysisRequested || isRetiring || !pipelineLock.tryLock()
        ) {
            image.close()
            return
        }
        scope.launch {
            try {
                val bitmap = viewModel.rotateBitmapForPreview(image)
                image.close()
                if (bitmap != null) {
                    recognizeProducts(bitmap)?.let { viewModel.updateProductRecognitionResult(it) }
                }
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "analyze() ran out of memory, dropping frame:${e.message}")
                viewModel.handleMemoryPressureDetected("live scanning")
            } catch (e: InvalidInputException) {
                Log.e(TAG, e.message ?: "InvalidInputException occurred")
            } catch (e: AIVisionSDKException) {
                Log.e(TAG, e.message ?: "AIVisionSDKException occurred")
            } catch (e: TimeoutException) {
                Log.e(TAG, "analyze() timed out")
            } catch (e: Exception) {
                Log.e(TAG, "analyze() failed: ${e.message}")
            } finally {
                pipelineLock.unlock()
            }
        }
    }

    fun executeHighRes(highResBitmap: Bitmap) {
        scope.launch {
            // Wait for any in-flight live-preview frame (or a pending dispose) to finish before
            // running the capture pipeline — Localizer/FeatureExtractor/Recognizer aren't safe to
            // call concurrently from two different frames. At 8MP + High input size each call
            // takes long enough that the capture button can easily be tapped while a live frame
            // is still processing.
            pipelineLock.lock()
            try {
                recognizeProducts(highResBitmap)?.let { viewModel.updateProductRecognitionResult(it) }
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "executeHighRes ran out of memory: ${e.message}")
                viewModel.handleMemoryPressureDetected("capture")
            } catch (e: TimeoutException) {
                Log.e(TAG, "executeHighRes timed out: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "executeHighRes failed: ${e.message}")
            } finally {
                pipelineLock.unlock()
            }
        }
    }

    /**
     * Runs the full Localizer -> FeatureExtractor -> Recognizer pipeline on [bitmap] and returns
     * the recognized products, or null if the Localizer call itself didn't return (e.g. torn down
     * mid-call). Shared by both the live-preview frame path and the high-res capture path, which
     * differ only in which bitmap they pass and whether the pipeline lock is try-locked or
     * blocking — the recognition steps themselves are identical either way, matching
     * ProductEnrollmentRecognition's executeProductRecognition().
     */
    private fun recognizeProducts(bitmap: Bitmap): MutableList<ProductData>? {
        val bboxes = localizer?.detect(bitmap, inferenceExecutor)?.get(10, TimeUnit.SECONDS)
            ?: return null
        viewModel.updateRetailShelfDetectionResult(bboxes)

        val products = bboxes.filter { it.cls == 1 }
            .sortedByDescending { (it.xmax - it.xmin) * (it.ymax - it.ymin) }
            .take(MAX_PRODUCTS_PER_FRAME)
            .toTypedArray()

        if (products.isEmpty()) return mutableListOf()

        val descriptors = extractor?.generateDescriptors(products, bitmap, inferenceExecutor)
            ?.get(10, TimeUnit.SECONDS)
        val recognitions = descriptors?.let {
            recognizer?.findRecognitions(it, inferenceExecutor)?.get(10, TimeUnit.SECONDS)
        } ?: return null

        val similarityThreshold = uiState.value.retailShelfSettings.similarityThreshold / 100f
        return toProductData(similarityThreshold, bitmap, products, recognitions)
    }

    fun stopPreviewAnalysis() {
        mIsStopPreviewAnalysisRequested = true
    }

    fun startPreviewAnalysis() {
        mIsStopPreviewAnalysisRequested = false
    }

    fun isPreviewAnalysisStopped(): Boolean = mIsStopPreviewAnalysisRequested
}
