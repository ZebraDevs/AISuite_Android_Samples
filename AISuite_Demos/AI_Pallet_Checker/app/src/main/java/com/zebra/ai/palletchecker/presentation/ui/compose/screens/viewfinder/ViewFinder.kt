package com.zebra.ai.palletchecker.presentation.ui.compose.screens.viewfinder


import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.palletchecker.helpers.LOGI
import com.zebra.ai.palletchecker.helpers.PalletProcessHelper
import com.zebra.ai.palletchecker.helpers.ScaleType
import com.zebra.ai.palletchecker.helpers.SpatialBoxNode
import com.zebra.ai.palletchecker.helpers.toPalletBoxUiModel
import com.zebra.ai.palletchecker.helpers.toPx
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel
import com.zebra.ai.palletchecker.presentation.model.PalletBox
import com.zebra.ai.palletchecker.presentation.model.SessionMode
import com.zebra.ai.palletchecker.presentation.ui.compose.components.LocalModelStoreOwner
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.viewfinder.components.AuditProgress
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.viewfinder.components.InterimResultsOverlay
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.viewfinder.components.PalletCameraHost
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.viewfinder.components.PalletCameraOverlay
import com.zebra.ai.palletchecker.presentation.viewmodel.AutoSnapPhase
import com.zebra.ai.palletchecker.presentation.viewmodel.AutoSnapUiState
import com.zebra.ai.palletchecker.presentation.viewmodel.BarcodeQuantityResult
import com.zebra.ai.palletchecker.presentation.viewmodel.CameraViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.ConfigureViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.MainViewModel
import com.zebra.ai.palletchecker.presentation.viewmodel.PROCESS_TYPE
import com.zebra.ai.palletchecker.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest


@OptIn(ExperimentalGetImage::class)
@Composable
fun ViewFinder(
    settingsViewModel: SettingsViewModel,
    configViewModel: ConfigureViewModel,
    onBackPress: () -> Unit = {},
    processType: PROCESS_TYPE = PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX,
    onResult: (processType: PROCESS_TYPE) -> Unit,
    cameraModel: CameraViewModel = viewModel()
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val act = LocalActivity.current
    val owner = LocalModelStoreOwner.current
    val viewmodel: MainViewModel = viewModel(viewModelStoreOwner = owner.viewModelStoreOwner!!)

    val captureImage = cameraModel.captureImageFlow.collectAsState()
    val autoSnapUiState = cameraModel.autoSnapUiState.collectAsState()
    val palletSessionState = viewmodel.getPalletSession().collectAsState()
    val pipValidationState = viewmodel.pipValidationState.collectAsState()
    val settings = settingsViewModel.settings.collectAsState()
    val wandSettings = settingsViewModel.wandSettings.collectAsState()
    val snapSettings = cameraModel.getSnapSettings().collectAsState()
    val config = configViewModel.config.collectAsState()

    val pipImgSize = 120.dp.toPx()

    var showConfigDialog by remember { mutableStateOf(false) }
    var lastSelectedBarcode by remember { mutableStateOf("") }
    var showInterimResults by remember { mutableStateOf(false) }

    var auditDoneHandled by remember { mutableStateOf(false) }

    var isImageVisible by remember { mutableStateOf(false) }
    var palletBoxes = remember { mutableStateListOf<PBoxUIModel>() }        // live pending (yellow) wand boxes
    var wandBaselineBoxes = remember { mutableStateListOf<PBoxUIModel>() }  // full snap-state baseline shown behind
    var palletBoxesForPip = remember { mutableStateListOf<PBoxUIModel>() }
    var barcodeDrawResult = remember { mutableStateListOf<BarcodeQuantityResult>() }
    var modelInitState = cameraModel.modelInitState.collectAsState()

    val wandBarcodeValidationCache = remember { mutableMapOf<String, BOX_VALIDATION>() }
    var previewViewSize by remember { mutableStateOf(IntSize(0, 0)) }

    val previewView = remember {
        PreviewView(context).also { pv ->
            pv.scaleType = PreviewView.ScaleType.FIT_CENTER
            pv.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                val w = right - left
                val h = bottom - top
                if (w > 0 && h > 0) {
                    previewViewSize = IntSize(w, h)
                }
            }
        }
    }

    val wandTimerState = viewmodel.wandTimerState.collectAsState()

    // Computes audit progress directly from spatial map nodes rather than multi-key pipValidationState entries to prevent inflated counts.
    // Uses pipValidationState solely to trigger recomposition, ensuring stats remain perfectly synchronized with actual session completion.
    val auditProgress = remember(pipValidationState.value) {
        if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
            val nodes = palletSessionState.value.spatialMap?.nodes ?: emptyList()
            var verified = 0
            var pending = 0
            var mismatch = 0
            nodes.forEach { node ->
                when {
                    node.isAuditCompleted && node.wandValidation == BOX_VALIDATION.VERIFIED -> verified++
                    node.isAuditCompleted && node.wandValidation == BOX_VALIDATION.MISMATCH_QTY -> mismatch++
                    else -> pending++
                }
            }
            AuditProgress(
                verifiedCount = verified,
                pendingCount = pending,
                mismatchCount = mismatch,
                expectedBoxes = config.value.expectedBoxes
            )
        } else {
            null
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        cameraModel.resetCaptureLocks()
    }

    LaunchedEffect(processType) {

        if (processType == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
            viewmodel.updateSkuPrefix(config.value.listOfConfig)
            viewmodel.startSession(expectedBoxes = config.value.expectedBoxes)
        } else if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
            auditDoneHandled = false  // Reset guard on wand session entry
            wandBarcodeValidationCache.clear()  // Reset validation cache for new wand session
            viewmodel.updateSkuPrefix(config.value.listOfConfig)
            // Only show the PIP image for PALLET_FINITE mode (not hybrid wand)
            if (palletSessionState.value.sessionMode == SessionMode.PALLET_FINITE
                && !palletSessionState.value.isHybridWand) {
                isImageVisible = true
            }
        }
    }

    LaunchedEffect(processType) {
        cameraModel.updateProcessType { processType }
    }

    // Start wand timer when entering wand mode; cancel on dispose
    LaunchedEffect(processType) {
        if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
            viewmodel.startWandTimer()
        }
    }

    // Timer expiry — stop camera, finalise session, navigate to results
    LaunchedEffect(wandTimerState.value.isExpired) {
        if (wandTimerState.value.isExpired &&
            processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
            auditDoneHandled = true  // Prevent collectLatest from also navigating
            cameraModel.stop()
            viewmodel.expireWandSession()
            onResult(processType)
        }
    }

    LaunchedEffect(modelInitState.value) {
        if(!modelInitState.value) return@LaunchedEffect
        if (processType == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
            cameraModel.getResults().collectLatest { palletResults ->
                palletBoxes.clear()

                // Collect boxes for debug display if debug mode is enabled
                if (settings.value.debugSettings.debugModeEnabled &&
                    settings.value.debugSettings.showPreSnapBoundingBoxes) {
                    palletResults.forEach { data ->
                        val p = data.toPalletBoxUiModel(
                            cameraModel.getResolutionSize(),
                            IntSize(previewView.width, previewView.height),
                            scaleType = ScaleType.FIT_CENTER,  // Must match PreviewView.ScaleType.FIT_CENTER
                            appConfig = config.value.listOfConfig
                        )
                        if (p.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID) {
                            palletBoxes.add(p)
                        }
                    }
                }

                val shouldTriggerAutoSnap = cameraModel.shouldAutoSnap(palletResults)

                if (shouldTriggerAutoSnap) {
                    cameraModel.takePicture()
                }
            }
        } else {
            cameraModel.getResults().collectLatest { palletResults ->
                palletBoxes.clear()

                // Build UI models for all live boxes
                val liveBoxes = palletResults.mapNotNull { data ->
                    val p = data.toPalletBoxUiModel(
                            cameraModel.getResolutionSize(),
                            IntSize(previewView.width, previewView.height),
                            isFromAnalyzer = false,  // EntityTrackerAnalyzer returns VIEW_REFERENCED coords
                            scaleType = ScaleType.FIT_CENTER,
                            appConfig = config.value.listOfConfig,
                            viewRefScale = cameraModel.viewRefScale
                        )
                    if (p.classId == PalletProcessHelper.PALLET_BASE_CLASS_ID) null else p
                }

                if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
                    // ══════════════════════════════════════════════════════════════════
                    // WAND MODE: Two-pass resolution with neighbour-anchored fallback
                    // ══════════════════════════════════════════════════════════════════
                    val sessionMode = palletSessionState.value.sessionMode
                    val expectedBoxes = config.value.expectedBoxes
                    val liveViewSize = IntSize(previewView.width, previewView.height)

                    // Dynamic node addition (runs before resolution)
                    for (p in liveBoxes) {
                        if (sessionMode == SessionMode.SHELF_CONTINUOUS) {
                            val isNewBox = viewmodel.addShelfNodeFromWand(p, config.value.listOfConfig)
                            if (isNewBox) {
                                LOGI("WandShelf", "New shelf box discovered — timer reset")
                                viewmodel.startWandTimer()
                            }
                        } else if (sessionMode == SessionMode.PALLET_FINITE) {
                            val isNewBox = viewmodel.addPalletFiniteNodeFromWand(p, config.value.listOfConfig)
                            if (isNewBox) {
                                LOGI("WandPallet", "New box discovered in PALLET_FINITE: ${p.stableKey}")
                            }
                        }
                    }

                    val matched = mutableListOf<Pair<PBoxUIModel, SpatialBoxNode>>()

                    for (p in liveBoxes) {
                        // Only attempt resolution if at least one barcode is decoded
                        val hasDecodedBarcode = p.palletBarcodes.any { it.data.isNotEmpty() }
                        if (!hasDecodedBarcode) {
                            LOGI("WandResolve", "Skipping box '${p.stableKey}' — no decoded barcode, cannot reliably identify")
                            continue
                        }

                        // Resolve strictly by barcode lookup only
                        val node = viewmodel.resolveWandBoxToNode(p)
                        if (node != null) {
                            matched.add(p to node)
                            LOGI("WandResolve", "✓ Barcode match: '${p.stableKey}' → node[${node.spatialIndex}] key='${node.stableKey.hashCode()}'")
                        } else {
                            LOGI("WandResolve", "✗ No barcode match for '${p.stableKey}' — barcodes not registered in spatial map")
                        }
                    }

                    LOGI("WandResolve", "Barcode-only resolution: ${liveBoxes.size} live boxes → ${matched.size} matched")

                    // Processes all matched boxes in the current frame to accumulate and save their wand data.
                    // Defers the isAuditDone check until after the loop, preventing subsequent boxes from being skipped when completion is triggered.
                    var anyBoxReachedTerminal = false

                    for ((p, effectiveNode) in matched) {
                        val livePrimaryBarcode = p.palletBarcodes
                            .firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data


                        // ── PIP validation lookup ─────────────────────────────────────
                        // Look up PIP validation using the effective node's stableKey
                        // AND the live box's decoded primary barcode. The primary barcode
                        // is the most authoritative identity — it ensures the live box
                        // shows the correct snap-phase colour even when spatial matching
                        // (Tier 2.5/3) resolves to a different node than the barcode implies.
                        val pipMap = viewmodel.pipValidationState.value
                        val pipByNodeKey = pipMap[effectiveNode.stableKey]

                        // Try lookup by the live box's primary (PRODUCT_SKU) barcode value
                        val pipByBarcode = if (!livePrimaryBarcode.isNullOrEmpty()) {
                            pipMap[livePrimaryBarcode]
                                ?: pipMap["barcode:$livePrimaryBarcode"]
                        } else null

                        // Pick the best (most authoritative) PIP validation:
                        // 1. Barcode-based lookup (identity match — highest confidence)
                        // 2. Effective node's stableKey lookup
                        // Barcode lookup takes priority because it directly represents
                        // the physical box identity from the snap phase.
                        val currentPipValidation = pipByBarcode ?: pipByNodeKey

                        // Completed boxes: show final colour, skip wand processing
                        if (effectiveNode.isAuditCompleted) {
                            val displayValidation = currentPipValidation ?: effectiveNode.wandValidation
                            if (!livePrimaryBarcode.isNullOrEmpty()) {
                                wandBarcodeValidationCache[livePrimaryBarcode] = displayValidation
                            }
                            palletBoxes.add(p.copy(validation = displayValidation))
                            continue
                        }

                        // No audit needed (already GREEN in snap)
                        if (!effectiveNode.isAuditRequired) {
                            val displayValidation = currentPipValidation ?: effectiveNode.snapValidation
                            if (!livePrimaryBarcode.isNullOrEmpty()) {
                                wandBarcodeValidationCache[livePrimaryBarcode] = displayValidation
                            }
                            palletBoxes.add(p.copy(validation = displayValidation))
                            continue
                        }

                        // Show the live bounding box sourced from PIP / snap state
                        val liveDisplayValidation = currentPipValidation ?: effectiveNode.snapValidation
                        if (!livePrimaryBarcode.isNullOrEmpty()) {
                            wandBarcodeValidationCache[livePrimaryBarcode] = liveDisplayValidation
                        }
                        palletBoxes.add(p.copy(validation = liveDisplayValidation))

                        val liveLocalizedCount = p.palletBarcodes.size
                        val wandDecodedCount = p.palletBarcodes.count { it.data.isNotEmpty() }

                        LOGI("WandDebug", "Live box stableKey='${p.stableKey}' effectiveNode=${effectiveNode.spatialIndex}")

                        // ── WandDiag: PIP association trace ──
                        val liveBcSummary = p.palletBarcodes.filter { it.data.isNotEmpty() }.map { "'${it.data}'(main=${it.isMainBarcode})" }
                        LOGI("WandDiag", "PIP liveBox='${p.stableKey.hashCode()}' → effNode[${effectiveNode.spatialIndex}] key='${effectiveNode.stableKey.hashCode()}' | pipByBc=$pipByBarcode pipByKey=$pipByNodeKey display=$liveDisplayValidation | liveBarcodes=${liveBcSummary.hashCode()}")

                        val hasReliableMatch = if (sessionMode == SessionMode.SHELF_CONTINUOUS) {
                            true
                        } else {
                            wandDecodedCount > 0
                        }

                        if (hasReliableMatch) {
                            val result = viewmodel.updateWandNodeIncrementally(
                                node = effectiveNode,
                                liveBox = p,
                                liveLocalizedCount = liveLocalizedCount,
                                appConfig = config.value.listOfConfig
                            )

                            LOGI("WandDebug", "  Incremental result: $result")

                            if (result == BOX_VALIDATION.VERIFIED || result == BOX_VALIDATION.MISMATCH_QTY) {
                                anyBoxReachedTerminal = true
                            }
                        }
                    }

                    // ── Unresolved live boxes: use cached validation by primary barcode ──
                    // Boxes that have a decoded barcode but couldn't be resolved to a
                    // spatial node this frame. Show them at their last-known validation
                    // so they don't flicker — but ONLY if identified by primary barcode.
                    val matchedStableKeys = matched.map { it.first.stableKey }.toSet()
                    for (p in liveBoxes) {
                        if (p.stableKey in matchedStableKeys) continue  // already processed
                        val primaryBarcode = p.palletBarcodes
                            .firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
                        if (!primaryBarcode.isNullOrEmpty()) {
                            val cachedValidation = wandBarcodeValidationCache[primaryBarcode]
                            if (cachedValidation != null) {
                                palletBoxes.add(p.copy(validation = cachedValidation))
                            }
                        }
                    }

                    // ── Deferred audit-done check ──────────────────────────────────
                    // Only check AFTER all matched boxes have been processed, so that
                    // every box's wand data is accumulated before finalisation.
                    // The auditDoneHandled guard prevents double-navigation from
                    // subsequent frames that arrive before cameraModel.stop() takes effect.
                    if (anyBoxReachedTerminal && !auditDoneHandled) {
                        if (viewmodel.isAuditDone(expectedBoxes)) {
                            auditDoneHandled = true
                            cameraModel.stop()
                            viewmodel.cancelWandTimer()
                            viewmodel.finaliseWandSession()
                            onResult(processType)
                        }
                    }
                    // ══════════════════════════════════════════════════════════════════
                } else {
                    // Non-wand live preview: just display all boxes
                    for (p in liveBoxes) {
                        if (p.palletBarcodes.isNotEmpty()) {
                            palletBoxes.add(p)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(palletSessionState.value, pipValidationState.value, previewViewSize) {
        if (processType != PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) return@LaunchedEffect
        wandBaselineBoxes.clear()
        val bitmap = palletSessionState.value.storedPalletCaptureImage ?: return@LaunchedEffect
        val sourceSize = IntSize(bitmap.width, bitmap.height)
        val viewSize = previewViewSize.takeIf { it.width > 0 && it.height > 0 } ?: return@LaunchedEffect
        val pipMap = pipValidationState.value

        LOGI("WandBaseline", "Rendering baseline overlay: ${pipMap.size} entries in pipValidationState, ${palletSessionState.value.storedPalletDetails.size} boxes")

        var baselineUpgradedCount = 0
        for (pBox in palletSessionState.value.storedPalletDetails) {
            if (pBox.classId == PalletProcessHelper.PALLET_BASE_CLASS_ID) continue

            val uiBox = pBox.toPalletBoxUiModel(
                sourceSize, viewSize,
                scaleType = ScaleType.FIT_CENTER,
                appConfig = emptyList()
            )

            val primaryBarcode = pBox.barcodeList
                .firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
            val pipByKey = pipMap[pBox.stableKey]
            val pipByBarcode = if (!primaryBarcode.isNullOrEmpty()) {
                pipMap[primaryBarcode] ?: pipMap["barcode:$primaryBarcode"]
            } else null
            val authoritative = pipByBarcode ?: pipByKey ?: pBox.validation
            if (authoritative != pBox.validation) {
                baselineUpgradedCount++
            }
            wandBaselineBoxes.add(uiBox.copy(validation = authoritative))
        }
        LOGI("WandBaseline", "Baseline boxes with upgraded validation: $baselineUpgradedCount")
    }

    // PiP thumbnail — only meaningful in PALLET_FINITE mode (pre-built map).
    // In SHELF_CONTINUOUS mode, the PIP snap is not shown (isImageVisible stays false).
    LaunchedEffect(palletSessionState.value, pipValidationState.value, previewViewSize) {
        palletBoxesForPip.clear()

        // Only populate for PALLET_FINITE
        if (palletSessionState.value.sessionMode != SessionMode.PALLET_FINITE) return@LaunchedEffect

        val bitmap = palletSessionState.value.storedPalletCaptureImage ?: return@LaunchedEffect
        val sourceSize = IntSize(bitmap.width, bitmap.height)
        val pipSizeInt = IntSize(pipImgSize.toInt(), pipImgSize.toInt())
        val pipMap = pipValidationState.value

        LOGI("PIP", "Re-rendering overlay: ${pipMap.size} entries in pipValidationState, ${palletSessionState.value.storedPalletDetails.size} boxes in storedPalletDetails")

        var upgradedCount = 0
        for (pBox in palletSessionState.value.storedPalletDetails) {
            if (pBox.classId == PalletProcessHelper.PALLET_BASE_CLASS_ID) continue
            val pBoxUiModel = pBox.toPalletBoxUiModel(
                sourceSize, pipSizeInt,
                scaleType = ScaleType.FIT_CENTER,
                appConfig = emptyList()
            )
            val primaryBarcode = pBox.barcodeList
                .firstOrNull { it.isMainBarcode && it.data.isNotEmpty() }?.data
            val pipByKey = pipMap[pBox.stableKey]
            val pipByBarcode = if (!primaryBarcode.isNullOrEmpty()) {
                pipMap[primaryBarcode] ?: pipMap["barcode:$primaryBarcode"]
            } else null
            val authoritative = pipByBarcode ?: pipByKey ?: pBox.validation
            if (authoritative != pBox.validation) {
                LOGI("PIP", "Box[${pBox.stableKey.hashCode()}]: validation upgraded from ${pBox.validation} → $authoritative (barcode=${primaryBarcode.hashCode()})")
                upgradedCount++
            }
            palletBoxesForPip.add(pBoxUiModel.copy(validation = authoritative))
        }
        if (upgradedCount > 0) {
            LOGI("PIP", "Total upgraded boxes in this render: $upgradedCount")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Multi-snap retry: keep capturing until all primary barcodes are found
    // or the configured retry count is reached. Each snap's results are merged
    // with prior attempts so cumulative barcode knowledge grows across retries.
    // When multi-snap is disabled in settings, maxSnapAttempts=1 (single snap).
    // ═══════════════════════════════════════════════════════════════════════
    val maxSnapAttempts = if (snapSettings.value.multiSnapEnabled) {
        snapSettings.value.multiSnapRetryCount.coerceAtLeast(1)
    } else {
        1  // Disabled: single snap only
    }
    var snapAttempt by remember { mutableStateOf(0) }
    var previousSnapResults by remember { mutableStateOf<List<PalletBox>?>(null) }
    var snapRetryInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(captureImage.value) {
        if (processType == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
            val bitmap = captureImage.value ?: return@LaunchedEffect

            snapRetryInProgress = true
            val currentAttempt = snapAttempt + 1
            snapAttempt = currentAttempt

            LOGI("MultiSnap", "═══ Snap attempt $currentAttempt/$maxSnapAttempts ═══")

            // Store the bitmap in the session (used by ResultsScreen for display)
            viewmodel.captureImageToSession(bitmap, 0)

            // Process this snap and check completeness, merging with prior results
            val result = viewmodel.processSnapAndCheckComplete(bitmap, previousSnapResults)

            LOGI("MultiSnap", "  Result: ${result.primaryFoundCount}/${result.productBoxCount} boxes have primary barcode, allFound=${result.allPrimaryFound}")

            // Note: Snap phase no longer marks boxes as MISMATCH_QTY (red).
            // Boxes that don't satisfy all criteria are kept as PARTIAL_DETECTION (yellow)
            // and deferred to the wand phase for final determination.
            // The retry logic below still checks for red as a safety net, but in practice
            // hasRedBoxes will always be false during snap.
            val redProductBoxCount = result.results.count {
                it.classId != PalletProcessHelper.PALLET_BASE_CLASS_ID &&
                it.validation == BOX_VALIDATION.MISMATCH_QTY
            }
            val hasRedBoxes = redProductBoxCount > 0
            LOGI("MultiSnap", "  RedBoxCheck: redProductBoxes=$redProductBoxCount hasRedBoxes=$hasRedBoxes")

            if ((result.allPrimaryFound && !hasRedBoxes) || currentAttempt >= maxSnapAttempts) {
                // ✓ All primaries found and no red boxes, or max retries reached — proceed to results
                if (currentAttempt >= maxSnapAttempts && (!result.allPrimaryFound || hasRedBoxes)) {
                    LOGI("MultiSnap", "  ⚠ Max retries ($maxSnapAttempts) reached — proceeding anyway " +
                        "(primaryMissing=${result.productBoxCount - result.primaryFoundCount} redBoxes=$redProductBoxCount)")
                } else {
                    LOGI("MultiSnap", "  ✓ All ${result.productBoxCount} boxes verified (green) after $currentAttempt attempt(s)")
                }
                snapRetryInProgress = false
                snapAttempt = 0
                previousSnapResults = null
                onResult(processType)
            } else {
                // ✗ Some boxes missing primary barcode or still red — retry with another snap
                val missing = result.productBoxCount - result.primaryFoundCount
                LOGI("MultiSnap", "  ↻ retry needed (missingPrimary=$missing redBoxes=$redProductBoxCount) — will re-snap (attempt ${currentAttempt + 1}/$maxSnapAttempts)")

                // Save current results for merging into the next attempt
                previousSnapResults = result.results

                // Unfreeze the camera and trigger another capture after a brief delay
                // to allow the camera to stabilize (slight scene movement helps decode)
                cameraModel.cameraFreezeState.value = false

                // Update the auto-snap UI to show retry status
                val retryReason = when {
                    hasRedBoxes && result.allPrimaryFound -> "criteria incomplete on $redProductBoxCount box(es)"
                    else -> "${result.primaryFoundCount}/${result.productBoxCount} barcodes found"
                }
                cameraModel.updateAutoSnapUiState(AutoSnapUiState(
                    phase = AutoSnapPhase.STABILIZING,
                    progress = currentAttempt.toFloat() / maxSnapAttempts,
                    message = "Re-scanning... $retryReason (attempt $currentAttempt/$maxSnapAttempts)"
                ))

                // Brief delay for camera to stabilize, then re-capture
                delay(300L)
                snapRetryInProgress = false

                // Trigger another capture
                cameraModel.takePicture()
            }
        }
    }

    BackHandler(enabled = true) {
        onBackPress()
    }

    LaunchedEffect(Unit) {
        cameraModel.startCamera(previewView, lifecycleOwner, launchType = processType)
    }

    LaunchedEffect(Unit) {
        act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) {
        onDispose {
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cameraModel.stop()
            if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
                viewmodel.cancelWandTimer()
            }
        }
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp.toPx()
    val showWidth = 120.dp.toPx()
    val padding = 4.dp.toPx()

    val pipOffset by animateOffsetAsState(
        targetValue = if (isImageVisible) Offset(
            screenWidth - showWidth - padding,
            padding
        ) else Offset(0f, 0f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    val pipSize by animateDpAsState(
        targetValue = if (isImageVisible) 120.dp else 400.dp,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    // PIP is only shown in PALLET_FINITE mode
    val isPalletFinite = palletSessionState.value.sessionMode == SessionMode.PALLET_FINITE

    Box(
        modifier = Modifier
            .fillMaxSize()
            //.padding(paddingValues)
    ) {

        PalletCameraHost(previewView = previewView, modifier = Modifier.fillMaxSize())

        PalletCameraOverlay(
            processType = processType,
            palletBoxes = palletBoxes,
            wandBaselineBoxes = wandBaselineBoxes,
            palletBoxesForPip = palletBoxesForPip,
            captureImageVisible = settings.value.livePipThumbnailEnabled,
            pipOffsetX = pipOffset.x,
            pipOffsetY = pipOffset.y,
            pipSizeDp = pipSize,
            autoSnapUiState = if (processType == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
                autoSnapUiState.value
            } else {
                AutoSnapUiState()
            },
            autoTriggerEnabled = snapSettings.value.autoTriggerEnabled,
            onManualCapture = { cameraModel.takePicture() },
            onPipClick = { isImageVisible = false },
            pipBitmap = if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX && isPalletFinite) {
                palletSessionState.value.storedPalletCaptureImage
            } else {
                null
            },
            // PIP is only shown in PALLET_FINITE mode (shelf mode has no pre-built snap map)
            showPip = isPalletFinite && wandSettings.value.livePipThumbnailEnabled,
            onShowResults = if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
                { showInterimResults = true }
            } else null,
            wandTimerState = if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX)
                wandTimerState.value else null,
            auditProgress = auditProgress,
            showAuditProgress = wandSettings.value.showAuditProgress,
            showPreSnapBoundingBoxes = settings.value.debugSettings.debugModeEnabled &&
                settings.value.debugSettings.showPreSnapBoundingBoxes,
            showSpatialMapIndices = settings.value.debugSettings.debugModeEnabled &&
                settings.value.debugSettings.showSpatialMapIndices,
            showSnapBarcodeLabels = settings.value.debugSettings.debugModeEnabled &&
                settings.value.debugSettings.showSnapBarcodeLabels,
            showSnapTrackingIds = settings.value.debugSettings.debugModeEnabled &&
                settings.value.debugSettings.showSnapTrackingIds,
            showWandBarcodeLabels = wandSettings.value.debugSettings.debugModeEnabled &&
                wandSettings.value.debugSettings.showWandBarcodeLabels,
            showWandTrackingIds = wandSettings.value.debugSettings.debugModeEnabled &&
                wandSettings.value.debugSettings.showWandTrackingIds,
            showLiveBoundingBoxes = wandSettings.value.showLiveBoundingBoxes
        )

        // Interim results panel
        if (showInterimResults && processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
            val spatialMap = palletSessionState.value.spatialMap
            val interimBoxes = palletSessionState.value.partialDetection.map { uiBox ->
                val node = spatialMap?.resolveNode(uiBox)
                if (node != null && node.isAuditCompleted) {
                    uiBox.copy(validation = node.wandValidation)
                } else {
                    uiBox
                }
            }
            InterimResultsOverlay(
                palletBoxes = interimBoxes,
                onDismiss = { showInterimResults = false }
            )
        }
    }
}
