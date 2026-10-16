// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.Descriptor
import com.zebra.ai.vision.detector.FeatureExtractor
import com.zebra.ai.vision.detector.FeatureStorage
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.Localizer
import com.zebra.ai.vision.detector.Recognizer
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.EnrollmentExportArtifact
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Collections.synchronizedList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.TimeSource

/**
 * [ProductEnrollmentRecognition] class is used to perform the product recognition on the Camera Live Preview.
 * It uses the Localizer to detect shelves, labels, peg labels, products which generates
 * bounding boxes for the detections.
 * FeatureExtractor is used to extract features from product detection bounding boxes.
 * Recognizer does a semantic searches to locate matching descriptors, and
 * finds the best fit from feature vectors.
 * FeatureStorage is used save feature descriptors, which is used in conjunction with feature extractor,
 * to enroll new products for product recognition.
 * It  provides the methods to initialize, deinitialize, execute,
 * deleteProductDB, applyProductDB and enrollProductIndex.
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 * @param cacheDir - App Cache Directory Path required for loading the Product.db toenroll & Recognize Retail Products
 */

class ProductEnrollmentRecognition(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel,
    context: Context,
    private val cacheDir: String
) : ImageAnalysis.Analyzer {

    private companion object {
        const val MAX_PRODUCTS_PER_FRAME = 30
        const val TAG = "ProductEnrollmentRecognition"
    }

    private var mIsStopPreviewAnalysisRequested: Boolean = false

    private var localizer: Localizer? = null
    private var extractor: FeatureExtractor? = null
    private var featureStorage: FeatureStorage? = null
    private var recognizer: Recognizer? = null
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // Dedicated single-thread executors per concern,
    // model init runs on its own thread so it
    // never contends with live-frame inference calls for the same thread-pool slot, and
    // native calls stay implicitly serialized without needing extra locking.
    private val initExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val inferenceExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var isRetiring = false
    private var disposeJob: Job? = null

    /** Guards localizer/extractor/featureStorage/recognizer so analyze(),executeHighRes() and
     * the deferred dispose in deinitialize() never touch the native objectsconcurrently —
     * doing so crashes natively with no Java stack trace, which becomes far morelikely at
     * 8MP/high input size where each native call takes long enough for a teardown orcapture
     * to land mid-call. */
    private val pipelineLock = Mutex()

    private val enrollmentExportManager = EnrollmentExportManager(context)

    // Incremented on every initialize()/deinitialize() call. Async init callbacks compare
    // their captured generation against the current one before assigning a native instance,
    // so a stale callback from a superseded init disposes its instance instead of leaking it.
    private val initGeneration = AtomicInteger(0)

    // Futures for native inits that are still in flight when deinitialize() runs. Awaited by
    // awaitPendingInit() before a new initialize() call is allowed to start, so we never have
    // two full sets of native model buffers (localizer/extractor/etc.) allocated at once.
    private val pendingFutures = synchronizedList(mutableListOf<CompletableFuture<*>>())

    /**
     * ProductEnrollmentRecognition workflow takes the input image and runs it through the
     * localizer that generates bounding boxes for the shelf, label, and product detections.
     * We use the product detection bounding boxes, which is passed along with the input image
     * to extract features using the feature extractor. Feature extractor generates feature
     * descriptors which is used to perform semantic search using the recognizer to find
     * the best fitting product from the database.
     * We use only products with greater than 0.8 confidence from product recognition.
     */
    override fun analyze(image: ImageProxy) {
        if (!uiState.value.isRetailShelfModelDemoReady) {
            Log.e(TAG, "ProductEnrollmentRecognition init in progress")
            image.close()
            return
        }
        if (mIsStopPreviewAnalysisRequested || isRetiring ||
            !pipelineLock.tryLock()
        ) {
            image.close()
            return
        }

        scope.launch {
            try {
                val bitmap = viewModel.rotateBitmapForPreview(image)
                image.close()
                if (bitmap != null) {
                    execute(bitmap = bitmap, isCapturedUseCase = false)
                }
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "analyze() ran out of memory, dropping frame:${e.message}")
                viewModel.handleMemoryPressureDetected("live scanning")
            } catch (e: InvalidInputException) {
                Log.e(TAG, e.message ?: "InvalidInputException occurred")
            } catch (e: AIVisionSDKException) {
                Log.e(TAG, e.message ?: "AIVisionSDKException occurred")
            } catch (e: TimeoutException) {
                Log.e(TAG, "Localizer detect() timed out")
            } catch (e: Exception) {
                Log.e(TAG, "analyze() failed: ${e.message}")
            } finally {
                pipelineLock.unlock()
            }
        }
    }

    /**
     * This function is used to initialize the various components that are used to
     * accomplish product recognition, namely localizer, feature extractor and
     * product recognition. feature storage in is used to save new products decriptors.
     *
     * Init steps are staged sequentially (each one only starts once the previous one has
     * settled — success or failure) rather than fired all at once. Firing all four native
     * loads simultaneously against a shared thread pool/DSP was likely a major contributor
     * to the ~25-30s init time seen at high model input size — the DSP is a single physical
     * resource and Localizer/FeatureExtractor both contend for it regardless of how many
     * Java threads are available.
     */
    fun initialize() {
        deinitialize()
        isRetiring = false
        updateRetailShelfModelDemoReady(false)
        try {
            initializeLocalizer()
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "initialize() ran out of memory loading models:${e.message}")
            viewModel.updateRecognizerInitError(
                "Not enough memory to load the model at the current settings. Close other apps and try again, or lower resolution/model input size."
            )
            viewModel.handleMemoryPressureDetected("model init")
        }
    }

    /** Called from the main thread, so this must not block. Native dispose is deferred to
     * [disposeJob], which waits for any in-flight analyze()/executeHighRes() call on this
     * instance to finish first — disposing while a call is still inside native
     * detect()/generateDescriptors()/findRecognitions() crashes natively with no Java stack
     * trace. */
    fun deinitialize() {
        initGeneration.incrementAndGet()
        isRetiring = true
        disposeJob = scope.launch {
            pipelineLock.withLock {
                localizer?.dispose(); localizer = null
                recognizer?.dispose(); recognizer = null
                featureStorage?.dispose(); featureStorage = null
                extractor?.dispose(); extractor = null
            }
        }
    }

    /**
     * Blocks until any native inits still in flight from a superseded initialize() call have
     * settled (their generation-guarded callbacks dispose the late-arriving native instance),
     * and until this instance's deferred dispose from deinitialize() has settled.
     * Must be called off the main thread, before starting a new initialize() call on this
     * (now-retired) instance, to avoid two full native model instances being allocated at once.
     */
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

    /**
     * ProductRecognition workflow takes the input image and runs it through the
     * localizer that generates bounding boxes for the shelf, label, and product detections.
     * WE use the product detection bounding boxes, which is passed along with the input image
     * to extract features using the feature extractor. Feature extractor generates feature
     * descriptors which is used to perform semantic search using the recognizer to find
     * the best fitting product from the database.
     * We use only products with greater than 0.8 confidence from product recognition.
     */
    fun execute(bitmap: Bitmap, isCapturedUseCase: Boolean = false) {
        // Skip updating results for a live-preview frame that was already
        // in-flight when capture was requested — its coordinates are wrong
        // for the high-res captured bitmap.
        if (!isCapturedUseCase && mIsStopPreviewAnalysisRequested) return

        val bboxes = executeRetailShelfLocalization(bitmap)
        if (bboxes != null) {
            executeProductRecognition(
                bitmap = bitmap,
                bboxes = bboxes,
                isCapturedUseCase = isCapturedUseCase
            )
        }
    }

    /**
     * To initialize the RetailShelfLocalizer, we need to set the
     * model name, processor type (CPU, GPU, DSP), and
     * dimensions of the input image
     */
    private fun initializeLocalizer() {
        Log.i(TAG, "initializeLocalizer")
        localizer?.dispose()
        localizer = null

        val myGeneration = initGeneration.get()

        val locSettings = Localizer.Settings("product-and-shelf-recognizer")

        //Swap the values as the presented index is reverse of what model expects
        val processorOrder =
            when (uiState.value.productRecognitionSettings.commonSettings.processorSelectedIndex) {
                0 -> arrayOf(2, 0, 1) // AUTO
                1 -> arrayOf(2) // DSP
                2 -> arrayOf(1) // GPU
                3 -> arrayOf(0) //CPU
                else -> {
                    arrayOf(2, 0, 1)
                }
            }

        locSettings.inferencerOptions.runtimeProcessorOrder = processorOrder

        locSettings.inferencerOptions.defaultDims.width =
            uiState.value.productRecognitionSettings.commonSettings.inputSizeSelected
        locSettings.inferencerOptions.defaultDims.height =
            uiState.value.productRecognitionSettings.commonSettings.inputSizeSelected

        try {
            val future = Localizer.getLocalizer(locSettings, initExecutor)
                .thenAccept { localizerInstance: Localizer ->
                    if (myGeneration != initGeneration.get()) {
                        // A newer initialize()/deinitialize() call superseded this one.
                        // Dispose the late-arriving native instance instead of assigning it.
                        localizerInstance?.dispose()
                        return@thenAccept
                    }
                    localizer = localizerInstance
                    updateRetailShelfModelDemoReady(true)
                    Log.i(TAG, "Localizer init Success")
                }.exceptionally { e: Throwable ->
                    if (myGeneration != initGeneration.get()) return@exceptionally null
                    Log.e(TAG, "Localizer init Failed -> " + e.message)
                    if (e.message?.contains("Given runtimes are not available") ==
                        true
                    ) {
                        viewModel.updateToastMessage(message = "Selected inference type is not supported on this device. Switching to Auto-select for optimal performance.")
                        viewModel.updateSelectedProcessor(0) //Auto-Select
                        viewModel.saveSettings()
                    }
                    null
                }
            pendingFutures.add(future)
            // Staged init: only start loading FeatureStorage once Localizer has settled
            // (success or failure) — avoids all native loads contending for the DSP/thread
            // pool at once.
            future.whenComplete { _, _ ->
                if (myGeneration == initGeneration.get()) {
                    initFeatureStorage()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Localizer init Failed -> " + e.message)
            initFeatureStorage()
        }
    }

    /**
     * To initialize the FeatureExtractor, we need to set the
     * model name, processor type (CPU, GPU, DSP)
     */
    private fun initFeatureExtractor() {
        Log.i(TAG, "initFeatureExtractor")
        extractor?.dispose()
        extractor = null

        val myGeneration = initGeneration.get()

        val extractorSettings =
            FeatureExtractor.Settings("product-and-shelf-recognizer")

        //Swap the values as the presented index is reverse of what model expects
        val processorOrder =
            when (uiState.value.productRecognitionSettings.commonSettings.processorSelectedIndex) {
                0 -> arrayOf(2, 0, 1) // AUTO
                1 -> arrayOf(2) // DSP
                2 -> arrayOf(1) // GPU
                3 -> arrayOf(0) //CPU
                else -> {
                    arrayOf(2, 0, 1)
                }
            }
        extractorSettings.inferencerOptions.runtimeProcessorOrder = processorOrder

        try {
            val future = FeatureExtractor.getFeatureExtractor(
                extractorSettings,
                initExecutor
            ).thenAccept { extractorInstance: FeatureExtractor ->
                if (myGeneration != initGeneration.get()) {
                    extractorInstance?.dispose()
                    return@thenAccept
                }
                extractor = extractorInstance
                updateRetailShelfModelDemoReady(true)
                Log.i(TAG, "Feature Extractor init Success")
            }.exceptionally { e: Throwable ->
                if (myGeneration != initGeneration.get()) return@exceptionally null
                Log.e(TAG, "Feature Extractor init Failed -> " + e.message)
                if (e.message?.contains("Given runtimes are not available") ==
                    true
                ) {
                    viewModel.updateSelectedProcessor(0) //Auto-Select
                    viewModel.saveSettings()
                }
                null
            }
            pendingFutures.add(future)
            // Staged init: only start Recognizer/FeatureStorage's sibling step once
            // FeatureExtractor has settled.
            future.whenComplete { _, _ ->
                if (myGeneration == initGeneration.get()) {
                    initProductRecognition()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Feature Extractor Failed -> " + e.message)
            initProductRecognition()
        } catch (e: RuntimeException) {
            Log.e(TAG, "Feature Extractor Failed -> " + e.message)
            initProductRecognition()
        } catch (e: Exception) {
            Log.e(TAG, "Feature Extractor Failed -> " + e.message)
            initProductRecognition()
        }
    }

    /**
     * To initialize the FeatureStorage, we need to set the
     * database file path where the features are stored and max update N.
     * We could potentially use this database file initialize a FeatureExtractor
     * to enroll more products for recognition
     */
    private fun initFeatureStorage() {
        Log.i(TAG, "initFeatureStorage")

        featureStorage?.dispose()
        featureStorage = null

        val myGeneration = initGeneration.get()

        val dataBaseFile = Paths.get(cacheDir, databaseFile).toString()
        val featureStorageSettings = FeatureStorage.Settings(dataBaseFile)
        featureStorageSettings.maxUpdateN = 5

        try {
            val future = FeatureStorage.getFeatureStorage(
                featureStorageSettings,
                initExecutor
            ).thenAccept { storageInstance: FeatureStorage ->
                if (myGeneration != initGeneration.get()) {
                    storageInstance?.dispose()
                    return@thenAccept
                }
                featureStorage = storageInstance
                updateRetailShelfModelDemoReady(true)
                Log.i(TAG, "Feature Storage init Success")
            }.exceptionally { e: Throwable ->
                if (myGeneration != initGeneration.get()) return@exceptionally null
                Log.e(TAG, "Feature Storage init Failed -> " + e.message)
                null
            }
            pendingFutures.add(future)
            // Staged init: only start FeatureExtractor once FeatureStorage has settled.
            future.whenComplete { _, _ ->
                if (myGeneration == initGeneration.get()) {
                    initFeatureExtractor()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Feature Storage init Failed -> " + e.message)
            initFeatureExtractor()
        } catch (e: RuntimeException) {
            Log.e(TAG, "DB empty -> " + e.message)
            initFeatureExtractor()
        } catch (e: Exception) {
            Log.e(TAG, "Feature Storage init Failed -> " + e.message)
            initFeatureExtractor()
        }
    }

    //
    /** To initialize the Recognizer, we need to set the database file path
     *  and index dimensions
     */
    private fun initProductRecognition(isEnrollmentRequested: Boolean = false) {
        Log.i(TAG, "initProductEnrollmentRecognition")
        recognizer?.dispose()
        recognizer = null

        val myGeneration = initGeneration.get()

        val recognizerSettings = Recognizer.SettingsDb()
        val indexDimensions = 768
        recognizerSettings.dbSource = Paths.get(cacheDir, databaseFile).toString()
        recognizerSettings.indexDimensions = indexDimensions

        try {
            val future = Recognizer.getRecognizer(
                recognizerSettings,
                initExecutor
            ).thenAccept { recognizerInstance: Recognizer ->
                if (myGeneration != initGeneration.get()) {
                    recognizerInstance?.dispose()
                    return@thenAccept
                }
                recognizer = recognizerInstance
                if (isEnrollmentRequested) {
                    updateProductEnrollmentState(state = true)
                } else {
                    updateRetailShelfModelDemoReady(true)
                }
                Log.i(TAG, "Recognizer init Success")
            }.exceptionally { e: Throwable ->
                if (myGeneration != initGeneration.get()) return@exceptionally null
                Log.e(TAG, "Recognizer init Failed -> " + e.message)
                null
            }
            pendingFutures.add(future)
        } catch (e: IOException) {
            Log.e(TAG, "Recognizer init Failed -> " + e.message)
        } catch (e: RuntimeException) {
            updateRetailShelfModelDemoReady(true)
            Log.e(TAG, "DB empty -> " + e.message)
        } catch (e: Exception) {
            Log.e(TAG, "Recognizer init Failed -> " + e.message)
        }
    }

    /**
     * This function is used to delete the product database file. We need to reinitialize
     * FeatureStorage and Recognizer with new empty database file.
     * This will result in no products being recognized.
     */
    fun deleteProductDB() {
        Log.i(TAG, "deleteProductDB")
        FileUtils.deleteProductDBFile()
        initFeatureStorage()
    }

    /**
     * This function is used to apply the product database file.
     * We need to reinitialize FeatureStorage and Recognizer with the new
     * database file. This will result in the products included in the database
     * being recognized.
     */
    fun applyProductDB() {
        Log.i(TAG, "applyProductDB")
        initFeatureStorage()
    }

    /**
     * Confirms [path] is a genuine product database by attempting to open it as a
     * FeatureStorage, bounded to [timeoutMs]. Called on a candidate file BEFORE it's allowed
     * to replace the live products.db — a corrupted/invalid file (e.g. a renamed .txt/.jpg)
     * must never overwrite a working database, and the SDK's native open call has no built-in
     * bound of its own (unlike every other native call in this class, which already uses
     * .get(10, TimeUnit.SECONDS)). Runs on a disposable executor so a pathological file that
     * genuinely hangs the native parser only leaks this one throwaway thread, never the live
     * pipeline's initExecutor.
     */
    fun validateProductDBFile(path: String, timeoutMs: Long = 10_000): Boolean {
        val validationExecutor = Executors.newSingleThreadExecutor()
        return try {
            val settings = FeatureStorage.Settings(path)
            val storage = FeatureStorage.getFeatureStorage(settings, validationExecutor)
                .get(timeoutMs, TimeUnit.MILLISECONDS)
            storage.dispose()
            true
        } catch (e: Exception) {
            Log.e(TAG, "validateProductDBFile: invalid database file -> ${e.message}")
            false
        } finally {
            validationExecutor.shutdown()
        }
    }

    /**
     * This function is used to enroll the products, using their product decriptors
     * into the database. We need to extract the features from the product detection
     * bounding boxes and add them to the feature storage.
     * We then reinitialize the product recognition to include the new products.
     */
    fun enrollProductIndex(productDataList: List<ProductData>) {
        val isEnrollmentTextAvailable = productDataList.filter { it.text.isNotEmpty() }
        if (isEnrollmentTextAvailable.isEmpty()) return

        scope.launch {
            pipelineLock.withLock {
                // A product whose recognizedSku differs from its (possibly user-edited) text
                // was renamed. FeatureStorage has no delete/rename-by-name API, so without this
                // the old name's descriptor entry is never removed, and later scans of that
                // product can nondeterministically match either the old or new name.
                val renames = productDataList
                    .filter { it.recognizedSku.isNotEmpty() && it.recognizedSku != it.text }
                    .associate { it.recognizedSku to it.text }
                if (renames.isNotEmpty()) {
                    migrateRenamedSkus(renames)
                }

                val enrollmentArtifacts = mutableListOf<EnrollmentExportArtifact>()

                for (product in productDataList) {
                    if (product.text.isNotEmpty()) {
                        val arrayOfDescriptor = extractor
                            ?.generateSingleDescriptor(product.crop, inferenceExecutor)
                            ?.get(10, TimeUnit.SECONDS)
                        featureStorage?.addDescriptors(product.text, arrayOfDescriptor, true)

                        if (arrayOfDescriptor != null) {
                            val embedding = arrayOfDescriptor.descriptorInfo
                            val modelVersionInfo = arrayOfDescriptor.modelVersionInfo
                            if (embedding != null) {
                                enrollmentArtifacts += EnrollmentExportArtifact(
                                    sku = product.text,
                                    cropBitmap = product.crop,
                                    embedding = embedding.toList(),
                                    modelVersionInfo = modelVersionInfo ?: ""
                                )
                            }
                        }
                    }
                }

                if (enrollmentArtifacts.isNotEmpty()) {
                    val exportedFolder =
                        enrollmentExportManager.exportEnrollmentSession(enrollmentArtifacts)
                    withContext(Dispatchers.Main) {
                        if (exportedFolder != null) {
                            viewModel.toast("Saved enrollment bundle to Downloads/$exportedFolder")
                        } else {
                            viewModel.toast("Product enrollment saved, but the export bundle could not be created")
                        }
                    }

                    initProductRecognition(isEnrollmentRequested = true)
                }
            }
        }
    }

    /**
     * FeatureStorage exposes no delete- or rename-by-name API — the only mutation available is
     * addDescriptors(name, ...), which always adds under a name and never removes an existing
     * one. So a SKU rename is handled by rebuilding the whole DB: every currently-enrolled
     * class's descriptors are snapshotted via getSupportDescriptorsForProduct(), then every
     * class is re-added under its current name into a fresh DB — renamed classes land under
     * their new name instead of the old one, so the stale old-name entry never reappears.
     *
     * The rebuild is staged entirely in a temp file and only swapped in after every product has
     * been re-added successfully. If a snapshot read, a re-add, or the swap itself fails partway
     * through, the live DB is left completely untouched — a failure here costs one missed
     * rename (the old-name bug persists one more save), never a corrupted or emptied product
     * database.
     *
     * Must be called while holding pipelineLock (from enrollProductIndex), before any new
     * descriptors for this save are added, so the rebuilt storage instance (if the swap
     * succeeds) is what the rest of enrollProductIndex writes into.
     */
    private fun migrateRenamedSkus(renames: Map<String, String>) {
        val storage = featureStorage ?: return
        val existingNames = try {
            storage.getAvailableClassNames()
        } catch (e: Exception) {
            Log.e(TAG, "migrateRenamedSkus: getAvailableClassNames failed -> ${e.message}")
            null
        } ?: return

        // Only classes actually being renamed need migrating; everything else keeps its name.
        if (existingNames.none { renames.containsKey(it) }) return

        // Snapshot every class before touching anything on disk. If any single class can't be
        // read back, abort the whole migration rather than silently dropping it from the
        // rebuilt DB — an unrelated product must never be lost because of someone else's rename.
        val snapshot = mutableMapOf<String, Array<FloatArray>>()
        for (name in existingNames) {
            val descriptors = try {
                storage.getSupportDescriptorsForProduct(name)
            } catch (e: Exception) {
                Log.e(TAG, "migrateRenamedSkus: snapshot of $name failed -> ${e.message}")
                null
            }
            if (descriptors == null) {
                Log.e(TAG, "migrateRenamedSkus: aborting, could not snapshot $name")
                return
            }
            snapshot[name] = descriptors
        }

        // Build the rebuilt DB in a temp file, not the live one.
        val tempDbPath = Paths.get(cacheDir, "$databaseFile.rebuild").toString()
        File(tempDbPath).delete() // clear any leftover temp file from a previous failed attempt

        val tempSettings = FeatureStorage.Settings(tempDbPath)
        tempSettings.maxUpdateN = 5
        val tempStorage = try {
            FeatureStorage.getFeatureStorage(tempSettings, initExecutor).get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "migrateRenamedSkus: creating temp FeatureStorage failed -> ${e.message}")
            null
        }
        if (tempStorage == null) {
            File(tempDbPath).delete()
            return
        }

        var rebuildSucceeded = true
        for ((oldName, descriptorArray) in snapshot) {
            val finalName = renames[oldName] ?: oldName
            try {
                val descriptor = Descriptor().apply { setDescriptorInfo(descriptorArray) }
                tempStorage.addDescriptors(finalName, descriptor, true)
            } catch (e: Exception) {
                Log.e(TAG, "migrateRenamedSkus: re-adding $finalName failed -> ${e.message}")
                rebuildSucceeded = false
                break
            }
        }
        tempStorage.dispose()

        if (!rebuildSucceeded) {
            File(tempDbPath).delete()
            Log.e(TAG, "migrateRenamedSkus: aborting, original DB left untouched")
            return
        }

        // Every product landed in the temp file successfully — safe to swap it in now.
        // Dispose the live handle first so the native layer isn't holding the file open
        // underneath the move.
        featureStorage?.dispose()
        featureStorage = null

        val liveDbPath = Paths.get(cacheDir, databaseFile).toString()
        val swapped = try {
            Files.move(
                Paths.get(tempDbPath),
                Paths.get(liveDbPath),
                StandardCopyOption.REPLACE_EXISTING
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "migrateRenamedSkus: swapping rebuilt DB into place failed -> ${e.message}")
            false
        }
        if (!swapped) {
            File(tempDbPath).delete()
            return
        }

        val liveSettings = FeatureStorage.Settings(liveDbPath)
        liveSettings.maxUpdateN = 5
        featureStorage = try {
            FeatureStorage.getFeatureStorage(liveSettings, initExecutor).get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "migrateRenamedSkus: reopening rebuilt DB failed -> ${e.message}")
            null
        }
    }

    /**
     * This function is used to execute the retail shelf localization.
     * Localizer generates boundingboxes for the shelf, shelf labels, peg labels,
     * and product detections.
     */
    private fun executeRetailShelfLocalization(bitmap: Bitmap?): Array<BBox>? {
        Log.i(TAG, "executeRetailShelfLocalization")
        Log.i(TAG, "Image Width = " + bitmap?.width.toString())
        Log.i(TAG, "Image Height = " + bitmap?.height.toString())
        val timeSource = TimeSource.Monotonic
        val mark = timeSource.markNow()
        val result = try {
            localizer?.detect(bitmap, inferenceExecutor)?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Log.e(TAG, "Localizer detect() timed out")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Localizer detect() failed: ${e.message}")
            null
        }
        val elapsed = timeSource.markNow() - mark
        updateRetailShelfDetectionResult(result)
        return result
    }

    /**
     * This function is used to execute the product recognition.
     * We use the product detection boundingboxes, which is passed along with the
     * input image to extract features using the feature extractor.
     * Feature extractor generates feature descriptors which is used to perform
     * semantic search using the recognizer to find the best fitting product
     * from the database.
     * We use only products with greater than 0.8 confidence from product
    recognition.
     */
    private fun executeProductRecognition(
        bitmap: Bitmap?,
        bboxes: Array<BBox>,
        isCapturedUseCase: Boolean
    ) {
        Log.i(TAG, "executeProductRecognition")
        if (recognizer == null) {

            // When no Product Database is found, the recognizer won't execute, resulted in empty productResultsList.
            // hence let's create a productResultsList assuming product SKU as empty
            if (isCapturedUseCase) {
                updateProductResults(
                    prepareProductDataList(
                        bBoxes = bboxes, bitmap =
                            bitmap!!
                    )
                )
            }
            return
        } // empty db

        val timeSource = TimeSource.Monotonic
        val mark = timeSource.markNow()

        val products: Array<BBox> = bboxes.filter { it.cls == 1 }
            .sortedByDescending { (it.xmax - it.xmin) * (it.ymax - it.ymin) }
            .take(MAX_PRODUCTS_PER_FRAME)
            .toTypedArray()
        Log.i(TAG, "Products - ${products.size}")

        if (products.isNotEmpty()) {
            try {
                val descriptors =
                    extractor?.generateDescriptors(
                        products, bitmap,
                        inferenceExecutor
                    )?.get(10, TimeUnit.SECONDS)

                val elapsed = timeSource.markNow() - mark
                Log.d(TAG, "Extractor - ${elapsed}")
                descriptors?.let {
                    val mark2 = timeSource.markNow()
                    val recognitions =
                        recognizer?.findRecognitions(
                            descriptors,
                            inferenceExecutor
                        )?.get(10, TimeUnit.SECONDS)
                    Log.i(TAG, "Recognitions - ${recognitions?.size}")

                    recognitions?.let { it1 ->
                        toProductData(

                            viewModel.uiState.value.productRecognitionSettings.similarityThreshold / 100f,
                            bitmap!!, products,
                            it1
                        )
                    }?.let { it2 ->
                        updateProductResults(it2)
                    }
                    val elapsed2 = timeSource.markNow() - mark2
                    Log.d(TAG, "Recognizer - ${elapsed2}")
                }
            } catch (e: InvalidInputException) {
                Log.e(TAG, "Exception = ${e.message}")
            } catch (e: TimeoutException) {
                Log.e(TAG, "Extractor/Recognizer call timed out")
            } catch (e: Exception) {
                Log.e(TAG, "executeProductRecognition failed: ${e.message}")
            }
        } else {
            updateProductResults(null)
        }
    }

    private fun prepareProductDataList(
        bBoxes: Array<BBox>,
        bitmap: Bitmap
    ): MutableList<ProductData> {
        val productDataList = mutableListOf<ProductData>()
        bBoxes.filter { it.cls == 1 }
            .sortedByDescending { (it.xmax - it.xmin) * (it.ymax - it.ymin) }
            .take(MAX_PRODUCTS_PER_FRAME)
            .forEach { productBbox ->
                productDataList.add(
                    ProductData(
                        bBox = productBbox,
                        text = "",
                        crop =
                            Bitmap.createBitmap(
                                bitmap,
                                productBbox.xmin.toInt(),
                                productBbox.ymin.toInt(),
                                (productBbox.xmax - productBbox.xmin).toInt(),
                                (productBbox.ymax - productBbox.ymin).toInt()
                            )
                    )
                )
            }
        return productDataList
    }

    private fun updateRetailShelfModelDemoReady(isReady: Boolean) {
        if (isReady) {
            if (localizer != null && featureStorage != null && extractor != null) {
                scope.launch {
                    delay(250)
                    viewModel.updateRetailShelfModelDemoReady(isReady = true)
                }
            }
        } else {
            viewModel.updateRetailShelfModelDemoReady(isReady = false)
        }
    }

    fun updateProductResults(results: MutableList<ProductData>?) {
        viewModel.updateProductRecognitionResult(results = results)
    }

    fun updateProductEnrollmentState(state: Boolean) {
        viewModel.updateProductEnrollmentState(state = state)
    }

    private fun updateRetailShelfDetectionResult(result: Array<BBox?>?) {
        viewModel.updateRetailShelfDetectionResult(results = result)
    }

    fun stopPreviewAnalysis() {
        mIsStopPreviewAnalysisRequested = true
    }

    fun executeHighRes(highResBitmap: Bitmap) {
        scope.launch {
            // Wait for any in-flight live-preview frame (or a pending dispose) to finish before
            // running the capture pipeline — Localizer/FeatureExtractor/Recognizer/FeatureStorage
            // aren't safe to call concurrently from two different frames. At 8MP + High input
            // size each native call takes long enough that the "start scan" button can easily be
            // tapped while a live frame is still processing, or a screen navigation can trigger
            // deinitialize() mid-call.
            pipelineLock.lock()
            try {
                execute(bitmap = highResBitmap, isCapturedUseCase = true)
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "executeHighRes ran out of memory: ${e.message}")
                viewModel.handleMemoryPressureDetected("capture")
            } catch (e: Exception) {
                Log.e(TAG, "executeHighRes failed: ${e.message}")
            } finally {
                pipelineLock.unlock()
            }
        }
    }

    fun startPreviewAnalysis() {
        mIsStopPreviewAnalysisRequested = false
    }
}
