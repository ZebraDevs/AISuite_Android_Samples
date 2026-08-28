package com.zebra.ai.palletchecker.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.zebra.ai.palletchecker.helpers.LOGD
import com.zebra.ai.palletchecker.camera.CameraCallback
import com.zebra.ai.palletchecker.camera.CameraHelper
import com.zebra.ai.palletchecker.domain.Configuration
import com.zebra.ai.palletchecker.domain.Settings
import com.zebra.ai.palletchecker.domain.enums.AutoTriggerMode
import com.zebra.ai.palletchecker.domain.enums.SettingsMode
import com.zebra.ai.palletchecker.helpers.BarcodeProcessHelper
import com.zebra.ai.palletchecker.helpers.LOGE
import com.zebra.ai.palletchecker.helpers.LOGV
import com.zebra.ai.palletchecker.helpers.PalletProcessHelper
import com.zebra.ai.palletchecker.helpers.clearTransformCache
import com.zebra.ai.palletchecker.presentation.model.PalletBox
import com.zebra.ai.palletchecker.helpers.rotateBitmapIfNeeded
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.palletchecker.helpers.ModelsStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CameraViewModel(val app: Application): AndroidViewModel(app) {
    private val TAG = "CameraVM"
    private val snapSettingsUseCase = Settings(app, SettingsMode.SNAP)
    private val wandSettingsUseCase = Settings(app,SettingsMode.WAND)
    private val configUseCase = Configuration(app)
    private var lastProxySize = IntSize(0,0)
    private var lastPreviewSize = IntSize(0, 0)
    var viewRefScale = 1f
        private set
    private var palletHelper = PalletProcessHelper(viewModelScope)
    private var captureImageRotation:Int = 0
    var captureImageFlow = MutableStateFlow<Bitmap?>(null)
    private var barcodeHelper = BarcodeProcessHelper(viewModelScope)
    private var _palletResults = MutableStateFlow<List<PalletBox>>(emptyList())
    private var palletResults : StateFlow<List<PalletBox>> = _palletResults

    private var _barcodeResults = MutableSharedFlow<List<BarcodeEntity>>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    private var barcodeResults : SharedFlow<List<BarcodeEntity>> = _barcodeResults

    private var processType :(()-> PROCESS_TYPE)?=null

    var cameraFreezeState = MutableStateFlow<Boolean>(false)
    lateinit var cameraHelper : CameraHelper

    private var lastBaseRect: androidx.compose.ui.geometry.Rect? = null
    private var stableBaseFrameCount = 0
    private var baseCandidateStartedAtMs = 0L
    private var lastBaseSeenAtMs = 0L
    private var lastAutoSnapAtMs = 0L
    private var captureInFlight = false

    private val minStableFramesForSnap = 2
    private val minSettleMsForSnap = 50L
    private val minStableIou = 0.74f
    private val maxBaseCenterDriftPx = 22f
    private val autoSnapCooldownMs = 100L
    private val baseLostResetMs = 100L
    private val minProductBoxesForSnap = 1

    private val _autoSnapUiState = MutableStateFlow(AutoSnapUiState())
    val autoSnapUiState: StateFlow<AutoSnapUiState> = _autoSnapUiState

    var modelInitState = MutableStateFlow(false)

    fun updateAutoSnapUiState(state: AutoSnapUiState) {
        _autoSnapUiState.value = state
    }

    init {
        LOGV(TAG," On Camera VM Created")
        observeModelInitState()
        observePalletResult()
        observeBarcodeResult()
    }

    private fun observeBarcodeResult() {
        viewModelScope.launch {
            barcodeHelper.barcodeResult.collectLatest {
                _barcodeResults.tryEmit(it)
            }
        }
    }

    private fun observePalletResult() {
        viewModelScope.launch {
            palletHelper.getResults().collectLatest {
                _palletResults.value = it
            }
        }
    }

    fun updateProcessType(getProcessType:()-> PROCESS_TYPE){
        processType = getProcessType
        resetAutoSnapGate()
        clearTransformCache()
        cameraFreezeState.value = false
        if (getProcessType() != PROCESS_TYPE.CAPTURE_PALLET_BOX) {
            _autoSnapUiState.value = AutoSnapUiState(phase = AutoSnapPhase.IDLE, message = "")
        } else {
            val snapSettings = snapSettingsUseCase.getSettings().value
            if (!snapSettings.autoTriggerEnabled) {
                _autoSnapUiState.value = AutoSnapUiState(
                    phase = AutoSnapPhase.SEARCHING,
                    message = "Tap capture button when ready"
                )
            } else {
                val expectedBoxes = configUseCase.getConfig().value.expectedBoxes
                val msg = when (snapSettings.autoTriggerMode) {
                    AutoTriggerMode.PALLET_BASE -> "Searching pallet..."
                    AutoTriggerMode.FIXED_QUANTITY -> "Searching for $expectedBoxes box(es)..."
                    AutoTriggerMode.PERCENTAGE_BASED -> {
                        val needed = computePercentageBoxThreshold(expectedBoxes, snapSettings.percentageThreshold)
                        "Searching for $needed box(es) (${snapSettings.percentageThreshold}% of $expectedBoxes)..."
                    }
                }
                _autoSnapUiState.value = AutoSnapUiState(phase = AutoSnapPhase.SEARCHING, message = msg)
            }
        }
        initCamera()
    }

    private fun initCamera() {
        cameraHelper = CameraHelper(context = app.applicationContext,  settings = {mode-> if(mode == SettingsMode.SNAP) snapSettingsUseCase.getSettings().value else wandSettingsUseCase.getSettings().value}, processType =  processType!!,
            callback = object : CameraCallback{
                override fun onImageCapture(proxy: ImageProxy) {
                    LOGD(TAG,"OnCapture Image ${proxy.width} ${proxy.height}")
                    captureInFlight = false
                    cameraFreezeState.value = false
                    resetAutoSnapGate()
                    val snapSettings = snapSettingsUseCase.getSettings().value
                    if (!snapSettings.autoTriggerEnabled) {
                        _autoSnapUiState.value = AutoSnapUiState(
                            phase = AutoSnapPhase.SEARCHING,
                            message = "Tap capture button when ready"
                        )
                    } else {
                        val expectedBoxes = configUseCase.getConfig().value.expectedBoxes
                        val msg = when (snapSettings.autoTriggerMode) {
                            AutoTriggerMode.PALLET_BASE -> "Searching pallet..."
                            AutoTriggerMode.FIXED_QUANTITY -> "Searching for $expectedBoxes box(es)..."
                            AutoTriggerMode.PERCENTAGE_BASED -> {
                                val needed = computePercentageBoxThreshold(expectedBoxes, snapSettings.percentageThreshold)
                                "Searching for $needed box(es) (${snapSettings.percentageThreshold}% of $expectedBoxes)..."
                            }
                        }
                        _autoSnapUiState.value = AutoSnapUiState(phase = AutoSnapPhase.SEARCHING, message = msg)
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        captureImageFlow.value = proxy.rotateBitmapIfNeeded()
                        captureImageRotation = proxy.imageInfo.rotationDegrees
                        proxy.close()
                    }

                }

                override fun onLivePreview(proxy: ImageProxy) {
                    LOGD(TAG,"onLive Preview  ${proxy.width} ${proxy.height} rotation=${proxy.imageInfo.rotationDegrees}")
                    if(lastProxySize.width ==0 && lastProxySize.height == 0){
                        val rotation = proxy.imageInfo.rotationDegrees
                        lastProxySize = if (rotation == 90 || rotation == 270) {
                            IntSize(proxy.height, proxy.width)  // Swap dimensions
                        } else {
                            IntSize(proxy.width, proxy.height)
                        }
                    }
                    if(!cameraFreezeState.value) {
                        if (processType?.invoke() == PROCESS_TYPE.CONFIGURE_BARCODE) {
                            barcodeHelper.analyze(proxy)
                        } else if(processType?.invoke() == PROCESS_TYPE.LIVE_PALLET_BOX ||
                            processType?.invoke() == PROCESS_TYPE.CAPTURE_PALLET_BOX ) {
                            palletHelper.analyze(proxy,false)
                        } else if(processType?.invoke() == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX){
                            palletHelper.analyze(proxy,true)
                        }
                    } else {
                        // Close the proxy if camera is frozen to avoid memory leaks
                        proxy.close()
                    }
                }

                override fun onResolutionAvailable(width: Int, height: Int, rotationDegrees: Int) {
                    if (lastProxySize.width == 0 && lastProxySize.height == 0) {
                        lastProxySize = if (rotationDegrees == 90 || rotationDegrees == 270) {
                            IntSize(height, width)
                        } else {
                            IntSize(width, height)
                        }
                        LOGD(TAG, "onResolutionAvailable: seeded lastProxySize=${lastProxySize.width}×${lastProxySize.height} (raw=${width}×${height}, rotation=$rotationDegrees)")
                        recomputeViewRefScale()
                    }
                }

                override fun onPreviewResolutionAvailable(width: Int, height: Int, rotationDegrees: Int) {
                    lastPreviewSize = if (rotationDegrees == 90 || rotationDegrees == 270) {
                        IntSize(height, width)
                    } else {
                        IntSize(width, height)
                    }
                    LOGD(TAG, "onPreviewResolutionAvailable: lastPreviewSize=${lastPreviewSize.width}×${lastPreviewSize.height} (raw=${width}×${height}, rotation=$rotationDegrees)")
                    recomputeViewRefScale()
                }
            }
        )
    }
    fun startCamera(previewView: PreviewView, owner: LifecycleOwner, launchType: PROCESS_TYPE) {
        lastProxySize= IntSize(0,0)
        lastPreviewSize = IntSize(0, 0)
        viewRefScale = 1f

        if(launchType == PROCESS_TYPE.CONFIGURE_BARCODE) {
            cameraHelper.setCustomAnalyzer(barcodeHelper.createEntityBarcodeAnalyzer() as ImageAnalysis.Analyzer)
        }else if (launchType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
            cameraHelper.setCustomAnalyzer(palletHelper.createEntityPalletAnalyzer() as ImageAnalysis.Analyzer)
        } else if (launchType == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
            cameraHelper.setCustomAnalyzer(null)
        } else {
            cameraHelper.setCustomAnalyzer(palletHelper.createEntityPalletAnalyzer() as ImageAnalysis.Analyzer)
        }
        cameraHelper.startLiveCamera(previewView,owner, launchType == PROCESS_TYPE.CAPTURE_PALLET_BOX)
    }

    fun takePicture() {
        if (!::cameraHelper.isInitialized) {
            LOGE(TAG, "takePicture ignored: cameraHelper not initialized")
            return
        }
        if (captureInFlight) {
            return
        }
        LOGV(TAG,"Take Picture ${cameraHelper}")
        captureInFlight = true
        lastAutoSnapAtMs = System.currentTimeMillis()
        cameraFreezeState.value = true
        _autoSnapUiState.value = AutoSnapUiState(
            phase = AutoSnapPhase.CAPTURING,
            progress = 1f,
            message = "Capturing snapshot..."
        )
        cameraHelper.captureImage()
    }

    private fun observeModelInitState() {
        viewModelScope.launch {
            ModelsStorage.modelInitiate.collectLatest { state ->
                if(state) {
                    _autoSnapUiState.value = AutoSnapUiState(
                        phase = AutoSnapPhase.CAPTURING,
                        progress = 1f,
                        message = "Model Loaded Successfully..."
                    )
                } else {
                    _autoSnapUiState.value = AutoSnapUiState(
                        phase = AutoSnapPhase.CAPTURING,
                        progress = 1f,
                        message = "Initializing Models. Please wait..."
                    )
                }
                modelInitState.value = state
            }
        }
    }

    fun shouldAutoSnap(palletResults: List<PalletBox>): Boolean {
        if (processType?.invoke() != PROCESS_TYPE.CAPTURE_PALLET_BOX) return false

        val snapSettings = snapSettingsUseCase.getSettings().value
        if (!snapSettings.autoTriggerEnabled) {
            if (!captureInFlight) {
                _autoSnapUiState.value = AutoSnapUiState(
                    phase = AutoSnapPhase.SEARCHING,
                    message = "Tap capture button when ready"
                )
            }
            return false
        }

        val now = System.currentTimeMillis()
        if (captureInFlight) {
            _autoSnapUiState.value = AutoSnapUiState(
                phase = AutoSnapPhase.CAPTURING,
                progress = 1f,
                message = "Capturing snapshot..."
            )
            return false
        }
        if (now - lastAutoSnapAtMs < autoSnapCooldownMs) {
            val remaining = ((autoSnapCooldownMs - (now - lastAutoSnapAtMs)).coerceAtLeast(0L) / 100L) * 100L
            _autoSnapUiState.value = AutoSnapUiState(
                phase = AutoSnapPhase.COOLDOWN,
                progress = 0f,
                message = "Rechecking stability... ${remaining}ms"
            )
            return false
        }

        val productBoxCount = palletResults.count { it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID }
        val expectedBoxes = configUseCase.getConfig().value.expectedBoxes

        return when (snapSettings.autoTriggerMode) {
            AutoTriggerMode.PALLET_BASE -> shouldAutoSnapPalletBase(palletResults, productBoxCount, now)
            AutoTriggerMode.FIXED_QUANTITY -> shouldAutoSnapFixedQuantity(productBoxCount, expectedBoxes, now)
            AutoTriggerMode.PERCENTAGE_BASED -> shouldAutoSnapPercentage(productBoxCount, snapSettings.percentageThreshold, expectedBoxes, now)
        }
    }

    /**
     * Original auto-snap logic: trigger when pallet base (class ID 3) is detected and stable.
     */
    private fun shouldAutoSnapPalletBase(palletResults: List<PalletBox>, productBoxCount: Int, now: Long): Boolean {
        val base = palletResults
            .filter { it.classId == PalletProcessHelper.PALLET_BASE_CLASS_ID }
            .maxByOrNull { rectArea(it.boundingBox) }

        if (base == null || productBoxCount < minProductBoxesForSnap) {
            _autoSnapUiState.value = AutoSnapUiState(
                phase = AutoSnapPhase.SEARCHING,
                progress = 0f,
                message = "Searching pallet..."
            )
            if (now - lastBaseSeenAtMs > baseLostResetMs) {
                resetAutoSnapGate()
            }
            return false
        }

        lastBaseSeenAtMs = now
        if (baseCandidateStartedAtMs == 0L) {
            baseCandidateStartedAtMs = now
        }

        val currentBaseRect = base.boundingBox
        val previousBaseRect = lastBaseRect

        stableBaseFrameCount = if (previousBaseRect != null && isBaseStable(previousBaseRect, currentBaseRect)) {
            stableBaseFrameCount + 1
        } else {
            baseCandidateStartedAtMs = now
            1
        }

        lastBaseRect = currentBaseRect

        val settleMs = now - baseCandidateStartedAtMs
        val frameProgress = (stableBaseFrameCount.toFloat() / minStableFramesForSnap.toFloat()).coerceIn(0f, 1f)
        val settleProgress = (settleMs.toFloat() / minSettleMsForSnap.toFloat()).coerceIn(0f, 1f)
        val progress = kotlin.math.min(frameProgress, settleProgress)

        val ready = stableBaseFrameCount >= minStableFramesForSnap && settleMs >= minSettleMsForSnap
        _autoSnapUiState.value = if (ready) {
            AutoSnapUiState(
                phase = AutoSnapPhase.READY,
                progress = 1f,
                message = "Ready. Taking snap..."
            )
        } else {
            AutoSnapUiState(
                phase = AutoSnapPhase.STABILIZING,
                progress = progress,
                message = "Hold steady..."
            )
        }

        return ready
    }

    /**
     * Auto-snap when a fixed number of product boxes are detected.
     */
    private fun shouldAutoSnapFixedQuantity(productBoxCount: Int, threshold: Int, now: Long): Boolean {
        if (productBoxCount < threshold) {
            _autoSnapUiState.value = AutoSnapUiState(
                phase = AutoSnapPhase.SEARCHING,
                progress = (productBoxCount.toFloat() / threshold.toFloat()).coerceIn(0f, 0.99f),
                message = "Detected $productBoxCount / $threshold box(es)..."
            )
            if (now - lastBaseSeenAtMs > baseLostResetMs) {
                resetAutoSnapGate()
            }
            return false
        }

        lastBaseSeenAtMs = now
        if (baseCandidateStartedAtMs == 0L) baseCandidateStartedAtMs = now
        stableBaseFrameCount++

        val settleMs = now - baseCandidateStartedAtMs
        val ready = stableBaseFrameCount >= minStableFramesForSnap && settleMs >= minSettleMsForSnap
        _autoSnapUiState.value = if (ready) {
            AutoSnapUiState(phase = AutoSnapPhase.READY, progress = 1f, message = "Ready. Taking snap ($productBoxCount boxes)...")
        } else {
            AutoSnapUiState(phase = AutoSnapPhase.STABILIZING, progress = 0.9f, message = "Hold steady... ($productBoxCount boxes)")
        }
        return ready
    }

    /**
     * Auto-snap when a percentage of expected total boxes are detected.
     */
    private fun shouldAutoSnapPercentage(productBoxCount: Int, percentageThreshold: Int, expectedTotal: Int, now: Long): Boolean {
        val needed = computePercentageBoxThreshold(expectedTotal, percentageThreshold)
        val currentPercent = if (expectedTotal > 0) (productBoxCount * 100) / expectedTotal else 0
        if (productBoxCount < needed) {
            _autoSnapUiState.value = AutoSnapUiState(
                phase = AutoSnapPhase.SEARCHING,
                progress = (productBoxCount.toFloat() / needed.toFloat()).coerceIn(0f, 0.99f),
                message = "Detected $productBoxCount / $needed box(es) ($currentPercent% of $expectedTotal)..."
            )
            if (now - lastBaseSeenAtMs > baseLostResetMs) {
                resetAutoSnapGate()
            }
            return false
        }

        lastBaseSeenAtMs = now
        if (baseCandidateStartedAtMs == 0L) baseCandidateStartedAtMs = now
        stableBaseFrameCount++

        val settleMs = now - baseCandidateStartedAtMs
        val ready = stableBaseFrameCount >= minStableFramesForSnap && settleMs >= minSettleMsForSnap
        _autoSnapUiState.value = if (ready) {
            AutoSnapUiState(phase = AutoSnapPhase.READY, progress = 1f, message = "Ready. Taking snap ($currentPercent%)...")
        } else {
            AutoSnapUiState(phase = AutoSnapPhase.STABILIZING, progress = 0.9f, message = "Hold steady... ($currentPercent%)")
        }
        return ready
    }

    private fun isBaseStable(previous: androidx.compose.ui.geometry.Rect, current: androidx.compose.ui.geometry.Rect): Boolean {
        val iou = intersectionOverUnion(previous, current)
        val centerDx = current.center.x - previous.center.x
        val centerDy = current.center.y - previous.center.y
        val drift = kotlin.math.sqrt(centerDx * centerDx + centerDy * centerDy)
        return iou >= minStableIou && drift <= maxBaseCenterDriftPx
    }

    private fun intersectionOverUnion(a: androidx.compose.ui.geometry.Rect, b: androidx.compose.ui.geometry.Rect): Float {
        val left = kotlin.math.max(a.left, b.left)
        val top = kotlin.math.max(a.top, b.top)
        val right = kotlin.math.min(a.right, b.right)
        val bottom = kotlin.math.min(a.bottom, b.bottom)
        if (right <= left || bottom <= top) return 0f

        val inter = (right - left) * (bottom - top)
        val union = rectArea(a) + rectArea(b) - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun rectArea(rect: androidx.compose.ui.geometry.Rect): Float {
        val w = rect.width.coerceAtLeast(0f)
        val h = rect.height.coerceAtLeast(0f)
        return w * h
    }

    private fun resetAutoSnapGate() {
        lastBaseRect = null
        stableBaseFrameCount = 0
        baseCandidateStartedAtMs = 0L
        lastBaseSeenAtMs = 0L
    }

    /**
     * Resets the camera freeze state and capture flag to resume camera operations
     * and autosnap logic when transitioning from locked to unlocked.
     */
    fun resetCaptureLocks() {
        cameraFreezeState.value = false
        captureInFlight = false
    }

    /**
     * Computes the concrete box count threshold for PERCENTAGE_BASED mode.
     * E.g. 80% of 10 boxes → ceil(8.0) = 8 boxes needed.
     */
    private fun computePercentageBoxThreshold(expectedTotal: Int, percentageThreshold: Int): Int {
        if (expectedTotal <= 0) return 1
        return kotlin.math.ceil(expectedTotal * percentageThreshold / 100.0).toInt().coerceAtLeast(1)
    }

    fun getResults() = palletResults
    fun getBarcodeResults() = barcodeResults
    fun getResolutionSize()= lastProxySize //if(processType?.invoke()!= PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) lastProxySize else IntSize(lastProxySize.height,lastProxySize.width)
    fun getSnapSettings() = snapSettingsUseCase.getSettings()

    /**
     * Recomputes [viewRefScale] whenever either the analysis or preview resolution changes.
     * Called from both [onResolutionAvailable] and [onPreviewResolutionAvailable].
     *
     * When ImageAnalysis is at a higher resolution than Preview (e.g. 2688×1512 vs 1920×1080),
     * CameraX's COORDINATE_SYSTEM_VIEW_REFERENCED applies the Preview→View transform
     * to Analysis-resolution coordinates without scaling for the resolution difference.
     * This makes bounding boxes appear (AnalysisRes / PreviewRes) times too large.
     *
     * viewRefScale = PreviewWidth / AnalysisWidth corrects this overshoot.
     */
    private fun recomputeViewRefScale() {
        if (lastProxySize.width > 0 && lastPreviewSize.width > 0) {
            viewRefScale = lastPreviewSize.width.toFloat() / lastProxySize.width.toFloat()
            LOGD(TAG, "recomputeViewRefScale: $viewRefScale " +
                    "(preview=${lastPreviewSize.width}×${lastPreviewSize.height}, " +
                    "analysis=${lastProxySize.width}×${lastProxySize.height})")
        }
    }


    fun stop() {
        if (::cameraHelper.isInitialized) {
            cameraHelper.stop()
        }
        captureInFlight = false
        cameraFreezeState.value = false
        resetAutoSnapGate()
        clearTransformCache()
        clear()
    }

    fun clear() {
        captureImageFlow.value = null
        palletHelper.clear()
        barcodeHelper.clear()
    }

    override fun onCleared() {
        LOGV(TAG,"onCleared ")
        stop()
        clear()

        super.onCleared()
    }
}

enum class PROCESS_TYPE {
    CONFIGURE_BARCODE,
    CAPTURE_PALLET_BOX,
    AUDIT_PARTAIL_PALLET_BOX,
    LIVE_PALLET_BOX
}

enum class AutoSnapPhase {
    IDLE,
    SEARCHING,
    STABILIZING,
    READY,
    CAPTURING,
    COOLDOWN
}

data class AutoSnapUiState(
    val phase: AutoSnapPhase = AutoSnapPhase.IDLE,
    val progress: Float = 0f,
    val message: String = ""
)
