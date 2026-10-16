// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Log
import com.google.gson.GsonBuilder
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.detector.InvalidInputException
import com.zebra.ai.vision.detector.TextOCR
import com.zebra.ai.vision.entity.ParagraphEntity
import com.zebra.aidatacapturedemo.BuildConfig
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.PROFILING
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Collections.synchronizedList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


/**
 * [TextOCRAnalyzer] class is used to detect all the Optical Character Recognition (OCR) found on the Camera Live Preview
 *
 * @param uiState - Used to read all the UI Current State
 * @param viewModel - Used to write any UI State Changes via [AIDataCaptureDemoViewModel]
 */
class TextOCRAnalyzer(
    val uiState: StateFlow<AIDataCaptureDemoUiState>,
    val viewModel: AIDataCaptureDemoViewModel,
    val context: Context
) {
    private val TAG = "TextOCRAnalyzer"

    companion object {
        private const val MAX_FRAMES_DIR_BYTES = 100L * 1024 * 1024  // 100 MB
    }

    private data class PendingFrame(
        val frameId: String,
        val seq: Int,
        val cameraTimestampNs: Long,
        val timestampMs: Long
    )

    private var textOCR: TextOCR? = null
    private val textOCRSettings = TextOCR.Settings("text-ocr-recognizer")
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    // Written on the main thread in initialize(); read on the ioExecutor thread by every write
    // task (as the stale-task guard) and by ensureSessionInitializedOnIo. @Volatile guarantees
    // cross-thread visibility of session transitions without needing a heavier synchronization.
    @Volatile
    private var sessionDir: File? = null
    // Set to true by the first write on ioExecutor for a given session (via compareAndSet), which
    // triggers lazy mkdirs + session.json write + logWriter open. Reset to false in initialize()
    // so a fresh session starts uninitialized. Ensures empty session folders never appear on disk
    // when the user enters/exits OCR live preview without any frames being processed.
    private val sessionInitialized = AtomicBoolean(false)
    // Cached manifest JSON built at initialize()-time; written to disk lazily by
    // ensureSessionInitializedOnIo() when the first frame or metadata line is about to be written.
    @Volatile
    private var pendingManifestJson: String? = null
    private val frameSeq = AtomicInteger(0)
    @Volatile
    private var logWriter: BufferedWriter? = null
    private val logFlushCounter = AtomicInteger(0)
    private val LOG_FLUSH_INTERVAL = 10

    private data class LatestFrame(val bitmap: Bitmap, val cameraTimestampNs: Long)

    private val pendingFrames = ConcurrentLinkedQueue<PendingFrame>()
    private val latestFrame = AtomicReference<LatestFrame?>(null)
    fun isDiagnosticLoggingEnabled(): Boolean {
        return uiState.value.textOCRSettings.enableDiagnosticLogging
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /** Guards textOCR so initialize()/deinitialize()/executeHighRes() never touch the native
     * object concurrently — doing so crashes natively with no Java stack trace, same risk
     * ProductEnrollmentRecognition/RetailShelfRecognizer already guard against. */
    private val pipelineLock = Mutex()

    private var isRetiring = false
    private var disposeJob: Job? = null

    // Incremented on every initialize()/deinitialize() call. Async init callbacks compare
    // their captured generation against the current one before assigning a native instance,
    // so a stale callback from a superseded init disposes its instance instead of leaking it.
    private val initGeneration = AtomicInteger(0)

    // Futures for native inits still in flight when deinitialize() runs. Awaited by
    // awaitPendingInit() before a new initialize() call starts, so we never have two full
    // native TextOCR instances allocated at once.
    private val pendingFutures = synchronizedList(mutableListOf<CompletableFuture<*>>())

    /**
     * initialize function is used to initialize the TextOCR model with the specified settings
     * and handle any exceptions that may occur during the initialization process.
     * It also updates the UI state to indicate whether the OCR model is ready or not.
     */
    fun initialize() {
        deinitialize()
        isRetiring = false

        val sessionMillis = System.currentTimeMillis()
        frameSeq.set(0)
        logFlushCounter.set(0)
        pendingFrames.clear()
        latestFrame.set(null)
        sessionInitialized.set(false)
        val framesDir = File(context.filesDir, "diagnostic_frames").also { it.mkdirs() }
        // Only remember the intended session path; do NOT create the folder yet. Folder creation +
        // session.json write + logWriter open are deferred to ensureSessionInitializedOnIo(),
        // called from the first frame/metadata write on ioExecutor. Prevents empty session_<ts>
        // folders from accumulating on every navigation into OCR live preview (MCAIS-2765).
        sessionDir = File(framesDir, "session_$sessionMillis")

        // When diagnostic logging is enabled, prune old sessions and close the previous session's
        // writer on the ioExecutor (avoids close/write races with any in-flight logging tasks).
        // When logging is disabled there is nothing to prune or close on behalf of this session.
        if (isDiagnosticLoggingEnabled()) {
            pendingManifestJson = buildSessionManifestJson(sessionMillis)
            ioExecutor.submit {
                try {
                    pruneOldSessions(framesDir)
                } catch (e: Exception) {
                    Log.e(TAG, "Session prune failed: ${e.message}")
                }
                try {
                    logWriter?.flush()
                    logWriter?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Log writer close failed: ${e.message}")
                }
                logWriter = null
            }
        } else {
            pendingManifestJson = null
            logWriter = null
        }

        val myGeneration = initGeneration.get()
        updateOcrModelDemoReady(false)
        attemptInit(myGeneration, allowFallbackRetry = true)
    }

    /**
     * Attempts to create the native TextOCR instance for [myGeneration]. If creation fails
     * because the selected inference type (e.g. GPU) isn't supported on this device, switches
     * the setting to Auto-select and — when [allowFallbackRetry] is true — immediately retries
     * once with that corrected setting, instead of only persisting it for the next app launch
     * and leaving this session's init (and whatever is awaiting isOcrModelDemoReady) stalled
     * until it times out (MCAIS-2846). [allowFallbackRetry] is false on that retry, so a second
     * failure under Auto-select surfaces as a real init error rather than looping.
     */
    private fun attemptInit(myGeneration: Int, allowFallbackRetry: Boolean) {
        try {
            configure()

            val mStart = System.currentTimeMillis()
            val future = TextOCR.getTextOCR(textOCRSettings, executorService)
                .thenAccept { ocrInstance ->
                    if (myGeneration != initGeneration.get()) {
                        Log.w(
                            TAG,
                            "TextOCRAnalyzer was deinitialized before init completed – disposing leaked OCR instance"
                        )
                        ocrInstance.dispose()
                        return@thenAccept
                    }
                    textOCR = ocrInstance
                    updateOcrModelDemoReady(true)
                    Log.e(
                        PROFILING,
                        "TextOCR() obj creation / model loading time = ${System.currentTimeMillis() - mStart} milli sec"
                    )
                    Log.i(TAG, "TextOCR creation success")
                    Log.i(TAG, "Diagnostic logging enabled at init: ${isDiagnosticLoggingEnabled()}")
                }.exceptionally { e ->
                    if (myGeneration != initGeneration.get()) return@exceptionally null
                    Log.e(TAG, "Fatal error: TextOCR creation failed - ${e.message}")
                    val isUnsupportedRuntime =
                        (e.message?.contains("Given runtimes are not available") == true) ||
                                (e.message?.contains("Error creating SNPE object") == true)
                    when {
                        isUnsupportedRuntime && allowFallbackRetry -> {
                            viewModel.updateToastMessage(message = "Selected inference type is not supported on this device. Switching to Auto-select for optimal performance.")
                            viewModel.updateSelectedProcessor(0) //Auto-Select
                            viewModel.saveSettings()
                            attemptInit(myGeneration, allowFallbackRetry = false)
                        }
                        isUnsupportedRuntime -> viewModel.updateRecognizerInitError(
                            "OCR could not be initialized on this device, even with Auto-select."
                        )
                    }
                    null
                }
            pendingFutures.add(future)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "initialize() ran out of memory loading TextOCR model: ${e.message}")
            viewModel.updateRecognizerInitError(
                "Not enough memory to load the model at the current settings. Close other apps and try again, or lower resolution/model input size."
            )
            viewModel.handleMemoryPressureDetected("model init")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: load failed - ${e.message}")
        }
    }

    fun flushLog() {
        if (!isDiagnosticLoggingEnabled()) return
        try {
            ioExecutor.submit {
                try { logWriter?.flush() } catch (e: Exception) { Log.e(TAG, "Log flush failed", e) }
            }.get(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e(TAG, "Log flush interrupted", e)
        } catch (e: TimeoutException) {
            Log.e(TAG, "Log flush timed out after 10s", e)
        } catch (e: Exception) {
            Log.e(TAG, "Log flush task failed", e)
        }
    }

    /** Native dispose is deferred to [disposeJob], which waits for any in-flight
     * executeHighRes() call on this instance to finish first — disposing while a call is
     * still inside native process() crashes natively with no Java stack trace. */
    fun deinitialize() {
        initGeneration.incrementAndGet()
        isRetiring = true
        disposeJob = scope.launch {
            pipelineLock.withLock {
                textOCR?.dispose()
                textOCR = null
            }
        }
        ioExecutor.submit {
            try {
                logWriter?.flush()
                logWriter?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Log writer close failed: ${e.message}")
            } finally {
                logWriter = null
            }
        }
    }

    /**
     * Blocks until any native inits still in flight from a superseded initialize() call have
     * settled, and until this instance's deferred dispose from deinitialize() has settled.
     * Must be called off the main thread, before starting a new initialize() call on this
     * (now-retired) instance, to avoid two full native TextOCR instances being allocated at once.
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
        executorService.shutdown()
        // shutdown() (not shutdownNow()) lets the flush/close task deinitialize() already
        // submitted to this executor finish first — this was never being shut down at all
        // before, leaking one thread per TextOCRAnalyzer instance across usecase switches.
        ioExecutor.shutdown()
        job.cancel()
    }

    fun getDetector(): TextOCR? {
        return textOCR
    }

    /** executeHighRes function is used to perform OCR analysis on a high-resolution bitmap image.
     * It submits the analysis task to an executor service, processes the image data, and updates
     * the UI state with the OCR results.
     * The function also handles exceptions that may occur during the analysis process.
     *
     * @param imageData - The high-resolution bitmap image to be analyzed for OCR.
     */
    fun executeHighRes(imageData: ImageData) {
        executorService.submit {
            runBlocking {
                pipelineLock.withLock {
                    try {
                        if (isRetiring) return@withLock
                        textOCR?.process(imageData)?.thenAccept { result ->
                            if (shouldUseLineLevel(uiState.value)) {
                                onDetectionTextResultLineLevel(result)
                            } else {
                                onDetectionTextResultWordLevel(result)
                            }
                        }
                    } catch (e: OutOfMemoryError) {
                        Log.e(TAG, "executeHighRes ran out of memory: ${e.message}")
                        viewModel.handleMemoryPressureDetected("capture")
                    } catch (e: InvalidInputException) {
                        Log.e(TAG, e.message ?: "InvalidInputException occurred")
                    } catch (e: AIVisionSDKException) {
                        Log.e(TAG, e.message ?: "AIVisionSDKException occurred")
                    } catch (e: Exception) {
                        Log.e(TAG, e.message ?: "Exception occurred during executeHighRes")
                    }
                }
            }
        }
    }

    internal fun captureFrame(bitmap: Bitmap): String? {
        if (!isDiagnosticLoggingEnabled()) return null
        val dir = sessionDir ?: return null
        val manifest = pendingManifestJson ?: return null
        val millis = System.currentTimeMillis()
        val seq = frameSeq.getAndIncrement()
        val filename = "frame_${millis}_${seq}.jpg"
        ioExecutor.submit {
            // Drop the task if a new session has started since this write was queued. Without this
            // guard the task would run ensureSessionInitializedOnIo() against the NEW sessionDir
            // (prematurely creating the new session's folder) and then attempt to write into the
            // OLD `dir` captured above, which was never created and no longer will be.
            if (sessionDir != dir) return@submit
            if (!ensureSessionInitializedOnIo(dir, manifest)) return@submit
            try {
                val file = File(dir, filename)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            } catch (e: Exception) {
                Log.e(TAG, "Frame capture write failed: ${e.message}")
            }
        }
        return filename
    }

    /** Diagnostics don't need full 8MP — downscale to keep memory/IO low when logging is on. */
    private fun downscaleForDiagnostics(src: Bitmap, maxDim: Int = 1280): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val scale = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src, (src.width * scale).toInt(), (src.height * scale).toInt(), true
        )
    }

    internal fun stashLatestBitmap(bitmap: Bitmap, cameraTimestampNs: Long) {
        if (!isDiagnosticLoggingEnabled()) return
        // Store a downscaled copy so we don't retain a full-res (up to 8MP) bitmap per frame.
        val scaled = downscaleForDiagnostics(bitmap)
        val previous = latestFrame.getAndSet(LatestFrame(scaled, cameraTimestampNs))
        previous?.bitmap?.takeIf { !it.isRecycled }?.recycle()
    }

    internal fun captureLatestOcrFrame(): String? {
        if (!isDiagnosticLoggingEnabled()) return null
        val dir = sessionDir ?: return null
        val manifest = pendingManifestJson ?: return null
        val frame = latestFrame.getAndSet(null) ?: return null
        val millis = System.currentTimeMillis()
        val seq = frameSeq.getAndIncrement()
        val cameraTs = frame.cameraTimestampNs
        val filename = "frame_${millis}_${seq}.jpg"
        pendingFrames.offer(PendingFrame(filename, seq, cameraTs, millis))
        ioExecutor.submit {
            // See captureFrame() — drop stale tasks queued before a new session started, so we
            // don't prematurely create the new session's folder just to fail writing into an old
            // one.
            if (sessionDir != dir) return@submit
            if (!ensureSessionInitializedOnIo(dir, manifest)) return@submit
            try {
                val file = File(dir, filename)
                FileOutputStream(file).use { frame.bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            } catch (e: Exception) {
                Log.e(TAG, "Frame capture write failed: ${e.message}")
            }
        }
        return filename
    }

    /**
     * Lazily creates the session directory on disk, writes session.json, and opens the
     * metadata.jsonl BufferedWriter. Runs the setup exactly once per session via a compareAndSet
     * on [sessionInitialized] — subsequent callers observe true and skip straight through.
     *
     * MUST be called from the ioExecutor thread (i.e. inside an ioExecutor.submit { } block) so
     * that state mutations to [logWriter] are visible to other ioExecutor tasks without extra
     * synchronization.
     *
     * Takes [dir] and [manifest] as explicit parameters rather than re-reading [sessionDir] and
     * [pendingManifestJson] internally, so callers pass the values they already snapshotted at
     * task-submission time. This eliminates a TOCTOU race where the caller's stale-task guard
     * would pass but the fields could be re-read here to observe a NEW session's values.
     *
     * On any failure path AFTER the compareAndSet has already claimed initialization, we roll
     * [sessionInitialized] back to false so the next caller can retry setup. Otherwise the flag
     * would stay stuck at true after a partial failure, silently causing every subsequent write
     * to skip re-init and hit missing-dir / null-writer errors.
     *
     * @return true if the session is ready to be written to (either just initialized or was
     *   already initialized). false if the caller should bail out.
     */
    private fun ensureSessionInitializedOnIo(dir: File, manifest: String): Boolean {
        if (!sessionInitialized.compareAndSet(false, true)) return true
        // File.mkdirs() returns false both when the dir already exists AND when creation fails;
        // it does NOT throw on failure. Check existence separately so we only fail on real errors.
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Session dir create failed: ${dir.absolutePath}")
            sessionInitialized.set(false)
            return false
        }
        try {
            File(dir, "session.json").writeText(manifest)
        } catch (e: Exception) {
            Log.e(TAG, "Session manifest write failed: ${e.message}")
            sessionInitialized.set(false)
            return false
        }
        try {
            logWriter = File(dir, "metadata.jsonl").bufferedWriter()
        } catch (e: Exception) {
            Log.e(TAG, "Log writer open failed: ${e.message}")
            logWriter = null
            sessionInitialized.set(false)
            return false
        }
        return true
    }

    private fun pruneOldSessions(framesDir: File) {
        val sessions = framesDir.listFiles { f -> f.isDirectory && f.name.startsWith("session_") }
            ?: return
        val sessionSizes = sessions
            .map { dir -> dir to dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
            .sortedBy { (dir, _) ->
                dir.name.removePrefix("session_").toLongOrNull() ?: dir.lastModified()
            }
        var totalBytes = sessionSizes.sumOf { it.second }
        for ((session, sessionBytes) in sessionSizes) {
            if (totalBytes <= MAX_FRAMES_DIR_BYTES) break
            if (session.deleteRecursively()) {
                totalBytes -= sessionBytes
                Log.i(
                    TAG,
                    "Pruned ${session.name} ($sessionBytes bytes); total now $totalBytes bytes"
                )
            }
        }
    }

    private fun buildSessionManifestJson(sessionMillis: Long): String {
        val manifest = mapOf(
            "device_model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "app_version" to BuildConfig.AI_DataCaptureDemo_Version,
            "ai_sdk_version" to BuildConfig.Zebra_AI_VisionSdk_Version,
            "session_start_ms" to sessionMillis,
            "ocr_settings" to uiState.value.textOCRSettings
        )
        return GsonBuilder().setPrettyPrinting().create().toJson(manifest)
    }

    fun logFrameResults(entityList: List<ParagraphEntity>, isLineLevel: Boolean) {
        if (!isDiagnosticLoggingEnabled()) return
        val pending = pendingFrames.poll() ?: return
        val (frameId, seq, cameraTimestampNs, timestampMs) = pending
        // Snapshot session state at submission time so the ioExecutor task can (a) detect a
        // session change and drop itself, and (b) pass consistent values into
        // ensureSessionInitializedOnIo without any re-reads that could race with initialize().
        val dir = sessionDir ?: return
        val manifest = pendingManifestJson ?: return
        ioExecutor.submit {
            if (sessionDir != dir) return@submit
            if (!ensureSessionInitializedOnIo(dir, manifest)) return@submit
            try {
                val writer = logWriter ?: return@submit
                val detections = JSONArray()
                entityList.forEach { entity ->
                    if (isLineLevel) {
                        for (line in entity.lines) {
                            val bbox = line.complexBBox
                            if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size == 4 && bbox.y.size == 4) {
                                val bboxArray = JSONArray()
                                for (i in 0..3) {
                                    bboxArray.put(JSONArray().put(bbox.x[i]).put(bbox.y[i]))
                                }
                                // LineEntity does not expose allPredictedText directly, so we
                                // derive a line-level confidence as the mean of the top-prediction
                                // confidences of its constituent words. Words without a decode
                                // (allPredictedText missing/empty) are excluded from the mean.
                                val perWordConfidences = line.words
                                    .mapNotNull { it.allPredictedText?.firstOrNull()?.confidence }
                                val lineConfidence = if (perWordConfidences.isEmpty()) null
                                    else perWordConfidences.average()
                                detections.put(
                                    JSONObject().put("text", line.text)
                                        .put("bounding_box", bboxArray)
                                        .put("confidence", lineConfidence ?: JSONObject.NULL)
                                )
                            }
                        }
                    } else {
                        for (line in entity.lines) {
                            for (word in line.words) {
                                val bbox = word.complexBBox
                                if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size == 4 && bbox.y.size == 4) {
                                    val bboxArray = JSONArray()
                                    for (i in 0..3) {
                                        bboxArray.put(JSONArray().put(bbox.x[i]).put(bbox.y[i]))
                                    }
                                    // Confidence lives on the top DecodedText prediction for the word.
                                    // word.text already returns the top prediction's content, so we
                                    // pair it with its matching confidence here.
                                    val confidence = word.allPredictedText?.firstOrNull()?.confidence
                                    detections.put(
                                        JSONObject().put("text", word.text)
                                            .put("bounding_box", bboxArray)
                                            .put("confidence", confidence ?: JSONObject.NULL)
                                    )
                                }
                            }
                        }
                    }
                }
                val record = JSONObject()
                    .put("frame_id", frameId)
                    .put("seq", seq)
                    .put("camera_timestamp_ns", cameraTimestampNs)
                    .put("timestamp_ms", timestampMs)
                    .put("detections", detections)
                writer.write(record.toString())
                writer.newLine()
                if (logFlushCounter.incrementAndGet() % LOG_FLUSH_INTERVAL == 0) {
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Log entry write failed: ${e.message}")
            }
        }
    }

    private fun configure() {
        try {
            if (uiState.value.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                //Swap the values as the presented index is reverse of what model expects
                val processorOrder =
                    when (uiState.value.ocrBarcodeFindSettings.commonSettings.processorSelectedIndex) {
                        0 -> arrayOf(2, 0, 1) // AUTO
                        1 -> arrayOf(2) // DSP
                        2 -> arrayOf(1) // GPU
                        3 -> arrayOf(0) //CPU
                        else -> {
                            arrayOf(2, 0, 1)
                        }
                    }
                textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = processorOrder
                textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = processorOrder

                // SDK rejects exactly 0 at runtime ("decodingTotalProbThreshold must be in (0, 1)
                // range" — AiSuiteSDK error log), despite the 3-3 docs stating [0.0, 1.0] is a
                // valid closed range. Use the smallest practical positive value to keep the
                // "accept every decode, no confidence filtering" intent for OCRBarcodeFind while
                // satisfying the SDK's actual open-interval validation.
                textOCRSettings.decodingTotalProbThreshold = 0.0001F

                textOCRSettings.detectionInferencerOptions.defaultDims.width =
                    uiState.value.ocrBarcodeFindSettings.commonSettings.inputSizeSelected
                textOCRSettings.detectionInferencerOptions.defaultDims.height =
                    uiState.value.ocrBarcodeFindSettings.commonSettings.inputSizeSelected
            } else {
                //Swap the values as the presented index is reverse of what model expects
                val processorOrder =
                    when (uiState.value.textOCRSettings.commonSettings.processorSelectedIndex) {
                        0 -> arrayOf(2, 0, 1) // AUTO
                        1 -> arrayOf(2) // DSP
                        2 -> arrayOf(1) // GPU
                        3 -> arrayOf(0) //CPU
                        else -> {
                            arrayOf(2, 0, 1)
                        }
                    }
                textOCRSettings.detectionInferencerOptions.runtimeProcessorOrder = processorOrder
                textOCRSettings.recognitionInferencerOptions.runtimeProcessorOrder = processorOrder

                textOCRSettings.detectionInferencerOptions.defaultDims.width =
                    uiState.value.textOCRSettings.commonSettings.inputSizeSelected
                textOCRSettings.detectionInferencerOptions.defaultDims.height =
                    uiState.value.textOCRSettings.commonSettings.inputSizeSelected

                //Detection Parameters
                textOCRSettings.heatmapThreshold =
                    uiState.value.textOCRSettings.advancedOCRSetting.heatmapThreshold.toFloat()
                textOCRSettings.boxThreshold =
                    uiState.value.textOCRSettings.advancedOCRSetting.boxThreshold.toFloat()
                textOCRSettings.minBoxArea =
                    uiState.value.textOCRSettings.advancedOCRSetting.minBoxArea.toInt()
                textOCRSettings.minBoxSize =
                    uiState.value.textOCRSettings.advancedOCRSetting.minBoxSize.toInt()
                textOCRSettings.unclipRatio =
                    uiState.value.textOCRSettings.advancedOCRSetting.unclipRatio.toFloat()
                textOCRSettings.minRatioForRotation =
                    uiState.value.textOCRSettings.advancedOCRSetting.minRatioForRotation.toFloat()

                textOCRSettings.decodingMaxWordCombinations =
                    uiState.value.textOCRSettings.advancedOCRSetting.maxWordCombinations.toInt()
                textOCRSettings.decodingTopkIgnoreCutoff =
                    uiState.value.textOCRSettings.advancedOCRSetting.topkIgnoreCutoff.toInt()
                textOCRSettings.decodingTotalProbThreshold =
                    uiState.value.textOCRSettings.advancedOCRSetting.totalProbabilityThreshold.toFloat()

                // OCR Tiling related
                if (uiState.value.textOCRSettings.advancedOCRSetting.enableTiling) {
                    textOCRSettings.tiling.enable = true
                    textOCRSettings.tiling.topCorrelationThr =
                        uiState.value.textOCRSettings.advancedOCRSetting.topCorrelationThreshold.toFloat()
                    textOCRSettings.tiling.mergePointsCutoff =
                        uiState.value.textOCRSettings.advancedOCRSetting.mergePointsCutoff.toInt()
                    textOCRSettings.tiling.splitMarginFactor =
                        uiState.value.textOCRSettings.advancedOCRSetting.splitMarginFactor.toFloat()
                    textOCRSettings.tiling.aspectRatioLowerThr =
                        uiState.value.textOCRSettings.advancedOCRSetting.aspectRatioLowerThreshold.toFloat()
                    textOCRSettings.tiling.aspectRatioUpperThr =
                        uiState.value.textOCRSettings.advancedOCRSetting.aspectRatioUpperThreshold.toFloat()
                    textOCRSettings.tiling.topkMergedPredictions =
                        uiState.value.textOCRSettings.advancedOCRSetting.topKMergedPredictions.toInt()
                } else {
                    textOCRSettings.tiling.enable = false
                }
                if (uiState.value.textOCRSettings.advancedOCRSetting.enableGrouping) {
                    textOCRSettings.grouping.widthDistanceRatio =
                        uiState.value.textOCRSettings.advancedOCRSetting.widthDistanceRatio.toFloat()
                    textOCRSettings.grouping.heightDistanceRatio =
                        uiState.value.textOCRSettings.advancedOCRSetting.heightDistanceRatio.toFloat()
                    textOCRSettings.grouping.centerDistanceRatio =
                        uiState.value.textOCRSettings.advancedOCRSetting.centerDistanceRatio.toFloat()
                    textOCRSettings.grouping.paragraphHeightDistance =
                        uiState.value.textOCRSettings.advancedOCRSetting.paragraphHeightDistance.toFloat()
                    textOCRSettings.grouping.paragraphHeightRatioThreshold =
                        uiState.value.textOCRSettings.advancedOCRSetting.paragraphHeightRatioThreshold.toFloat()
                } else {
                    //Reset to default values
                    textOCRSettings.grouping.widthDistanceRatio = 1.5f
                    textOCRSettings.grouping.heightDistanceRatio = 2.0f
                    textOCRSettings.grouping.centerDistanceRatio = 0.6f
                    textOCRSettings.grouping.paragraphHeightDistance = 1.0f
                    textOCRSettings.grouping.paragraphHeightRatioThreshold = 0.3333f
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: configure failed - ${e.message}")
        }
    }

    private fun updateOcrModelDemoReady(isReady: Boolean) {
        viewModel.updateOcrModelDemoReady(isReady = isReady)
    }

    private fun onDetectionTextResultWordLevel(entityList: List<ParagraphEntity>) {
        val outputOCRResultData = mutableListOf<ResultData>()
        entityList.forEach { entity ->
            val paragraphEntity = entity
            val lines = paragraphEntity.lines
            for (line in lines) {
                for (word in line.words) {
                    val bbox = word.complexBBox

                    if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size == 4 && bbox.y.size == 4) {
                        val minX = bbox.x[0]
                        val maxX = bbox.x[2]
                        val minY = bbox.y[0]
                        val maxY = bbox.y[2]

                        val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                        val decodedValue = word.text
                        outputOCRResultData.add(
                            ResultData(
                                boundingBox = rect,
                                text = decodedValue
                            )
                        )
                    }
                }
            }
        }
        viewModel.updateOcrResultData(
            results = FilterUtils.getOcrFilteredResultData(
                uiState = uiState.value,
                outputOCRResultData = outputOCRResultData
            )
        )
    }

    private fun onDetectionTextResultLineLevel(entityList: List<ParagraphEntity>) {
        val outputOCRResultData = mutableListOf<ResultData>()
        entityList.forEach { entity ->
            val paragraphEntity = entity
            val lines = paragraphEntity.lines
            for (line in lines) {
                val bbox = line.complexBBox

                if (bbox != null && bbox.x != null && bbox.y != null && bbox.x.size == 4 && bbox.y.size == 4) {
                    val minX = bbox.x[0]
                    val maxX = bbox.x[2]
                    val minY = bbox.y[0]
                    val maxY = bbox.y[2]

                    val rect = Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
                    val decodedValue = line.text
                    outputOCRResultData.add(
                        ResultData(
                            boundingBox = rect,
                            text = decodedValue
                        )
                    )
                }
            }
        }
        viewModel.updateOcrResultData(
            results = FilterUtils.getOcrFilteredResultData(
                uiState = uiState.value,
                outputOCRResultData = outputOCRResultData
            )
        )
    }
}

private fun shouldUseLineLevel(ui: AIDataCaptureDemoUiState): Boolean {
    val selectedIds = ui.ocrFilterData.selectedFieldIds
    if (selectedIds.isEmpty()) return false

    val configs = ui.ocrFilterData.fieldFilterConfigs
        .filter { it.fieldId in selectedIds }

    if (configs.isEmpty()) return false
    return configs.any { cfg ->
        (cfg.regexEnabled && cfg.regexData?.detectionLevel == DetectionLevel.LINE) ||
                (cfg.characterMatchEnabled && cfg.characterMatchData?.detectionLevel == DetectionLevel.LINE)
    }
}
