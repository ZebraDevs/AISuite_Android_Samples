package com.zebra.ai.palletchecker.presentation.ui.compose.screens.viewfinder.components

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel
import com.zebra.ai.palletchecker.presentation.ui.compose.components.BarcodeQuantityCanvas
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ContentWithLabel
import com.zebra.ai.palletchecker.presentation.ui.compose.components.PalletROI
import com.zebra.ai.palletchecker.presentation.ui.compose.components.palletBoxCanvasForPIP
import com.zebra.ai.palletchecker.presentation.ui.compose.components.preSnapDebugCanvas
import com.zebra.ai.palletchecker.presentation.ui.compose.components.wandDebugBarcodeCanvas
import com.zebra.ai.palletchecker.presentation.ui.compose.components.wandLiveValidationCanvas
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions
import com.zebra.ai.palletchecker.presentation.ui.theme.Black
import com.zebra.ai.palletchecker.presentation.ui.theme.ThemeDark
import com.zebra.ai.palletchecker.presentation.ui.theme.white
import com.zebra.ai.palletchecker.presentation.viewmodel.AutoSnapPhase
import com.zebra.ai.palletchecker.presentation.viewmodel.AutoSnapUiState
import com.zebra.ai.palletchecker.presentation.viewmodel.PROCESS_TYPE
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.snap
import androidx.compose.runtime.getValue
import com.zebra.ai.palletchecker.presentation.ui.theme.partialReadColor
import com.zebra.ai.palletchecker.presentation.ui.theme.qtyMismatchedColor
import com.zebra.ai.palletchecker.presentation.ui.theme.validBoxColor
import com.zebra.ai.palletchecker.presentation.viewmodel.WandTimerState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp

/**
 * Tracks audit progress during wand mode.
 * Used to show the user how many boxes have been verified vs pending.
 */
data class AuditProgress(
    val verifiedCount: Int = 0,
    val pendingCount: Int = 0,
    val mismatchCount: Int = 0,
    val expectedBoxes: Int = 0
) {
    val totalDetected: Int get() = verifiedCount + pendingCount + mismatchCount
    val isComplete: Boolean get() = pendingCount == 0
}

@Composable
internal fun PalletCameraHost(
    previewView: PreviewView,
    modifier: Modifier = Modifier
) {
    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())
}

@Composable
internal fun PalletCameraOverlay(
    processType: PROCESS_TYPE,
    palletBoxes: List<PBoxUIModel>,
    wandBaselineBoxes: List<PBoxUIModel> = emptyList(),
    palletBoxesForPip: List<PBoxUIModel>,
    captureImageVisible: Boolean,
    pipOffsetX: Float,
    pipOffsetY: Float,
    pipSizeDp: Dp,
    autoSnapUiState: AutoSnapUiState,
    autoTriggerEnabled: Boolean = true,
    onManualCapture: () -> Unit = {},
    onPipClick: () -> Unit,
    pipBitmap: Bitmap?,
    showPip: Boolean = true,
    onShowResults: (() -> Unit)? = null,
    wandTimerState: WandTimerState? = null,
    auditProgress: AuditProgress? = null,
    showAuditProgress: Boolean = true,
    showPreSnapBoundingBoxes: Boolean = false,
    showSpatialMapIndices: Boolean = false,
    showSnapBarcodeLabels: Boolean = false,
    showSnapTrackingIds: Boolean = false,
    showWandBarcodeLabels: Boolean = false,
    showWandTrackingIds: Boolean = false,
    showLiveBoundingBoxes: Boolean = true
) {

    if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX && captureImageVisible && showPip) {
        pipBitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Captured Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(pipSizeDp)
                    .offset { IntOffset(pipOffsetX.toInt(), pipOffsetY.toInt()) }
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, shape = RoundedCornerShape(8.dp))
                    .clickable { onPipClick() }
            )

            Box(
                modifier = Modifier
                    .size(pipSizeDp)
                    .offset { IntOffset(pipOffsetX.toInt(), pipOffsetY.toInt()) }
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                if (palletBoxesForPip.isNotEmpty()) {
                    palletBoxCanvasForPIP(Modifier.fillMaxSize(), palletBoxesForPip)
                }
            }
        }
    } else if (processType == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
        PalletROI(
            modifier = Modifier.fillMaxSize(),
            topReservedSpace = 36.dp
        )

        if (showPreSnapBoundingBoxes && palletBoxes.isNotEmpty()) {
            preSnapDebugCanvas(
                modifier = Modifier.fillMaxSize(),
                boxes = palletBoxes,
                showIndices = showSpatialMapIndices,
                showBarcodeLabels = showSnapBarcodeLabels,
                showTrackingIds = showSnapTrackingIds
            )
        } else if ((showSnapBarcodeLabels || showSnapTrackingIds) && palletBoxes.isNotEmpty()) {
            preSnapDebugCanvas(
                modifier = Modifier.fillMaxSize(),
                boxes = palletBoxes,
                showIndices = false,
                showBarcodeLabels = showSnapBarcodeLabels,
                showTrackingIds = showSnapTrackingIds
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            AutoSnapStatusStrip(
                state = autoSnapUiState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.84f)
            )

            // Show manual capture button when auto-trigger is disabled
            if (!autoTriggerEnabled) {
                ManualCaptureButton(
                    onClick = onManualCapture,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
        }
    }

    if (processType == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {

        if (showLiveBoundingBoxes && palletBoxes.isNotEmpty()) {
            wandLiveValidationCanvas(Modifier.fillMaxSize(), palletBoxes)
        }
        if ((showWandBarcodeLabels || showWandTrackingIds) && palletBoxes.isNotEmpty()) {
            wandDebugBarcodeCanvas(
                modifier = Modifier.fillMaxSize(),
                boxes = palletBoxes,
                showBarcodeLabels = showWandBarcodeLabels,
                showTrackingIds = showWandTrackingIds
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, start = 10.dp, end = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                onShowResults?.let { callback ->
                    Button(
                        onClick = callback,
                        modifier = Modifier.wrapContentWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeDark,
                            disabledContainerColor = Gray
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Show Results", color = White, fontWeight = FontWeight.Bold)
                    }
                }

                wandTimerState?.let { timer ->
                    WandTimerStrip(
                        timer = timer,
                        auditProgress = auditProgress,
                        showAuditProgress = showAuditProgress
                    )
                }
            }
        }

        if (wandTimerState?.isExpired == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = qtyMismatchedColor,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏱  Wand Session Expired",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Time limit reached. Unresolved boxes remain yellow.\nNavigating to final results…",
                            color = White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interim results overlay shown over the live wand viewfinder.
 * Displays every box seen so far with its current validation state and decoded barcodes.
 */
@Composable
internal fun InterimResultsOverlay(
    palletBoxes: List<PBoxUIModel>,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(white.copy(alpha = 0.96f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Interim Results",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "✕  Close",
                    color = White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.size(8.dp)) }

                items(palletBoxes) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.menuOverlayCardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = white),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.largeElevation)
                    ) {
                        ContentWithLabel("Box ${item.id}", item.validation) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            ) {
                                val decodedBarcodes = item.palletBarcodes.filter { it.data.isNotEmpty() }
                                decodedBarcodes.forEach { barcode ->
                                    Text(barcode.data, color = Black)
                                }
                                if (decodedBarcodes.isEmpty()) {
                                    Text(
                                        text = when (item.validation) {
                                            BOX_VALIDATION.NOT_DETECTED -> "No barcodes decoded yet"
                                            else -> "—"
                                        },
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun AutoSnapStatusStrip(
    state: AutoSnapUiState,
    modifier: Modifier = Modifier
) {
    if (state.phase == AutoSnapPhase.IDLE || state.message.isBlank()) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC111111)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Manual capture button shown when auto-trigger is disabled.
 * Rendered as a prominent circular shutter-button at the bottom center of the viewfinder.
 */
@Composable
private fun ManualCaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        containerColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

/**
 * A minimalistic circular countdown timer shown at the bottom-right of the wand viewfinder.
 *
 * - Small circular dial that starts solid filled and empties anti-clockwise as time progresses
 * - Shows remaining seconds in the center
 * - Color transitions green → amber → red as time runs low
 * - Pulses when ≤ 10 seconds remain
 */
@Composable
internal fun WandTimerStrip(
    timer: WandTimerState,
    modifier: Modifier = Modifier,
    auditProgress: AuditProgress? = null,
    showAuditProgress: Boolean = true
) {
    val fraction = timer.progress
    val isLow = timer.remainingSeconds <= 10
    val isMedium = timer.remainingSeconds <= 20

    val timerColor = when {
        isLow -> qtyMismatchedColor
        isMedium -> partialReadColor
        else -> validBoxColor
    }

    // Pulse alpha when low
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isLow) 0.5f else 1f,
        animationSpec = if (isLow) infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ) else snap(),
        label = "timerPulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAuditProgress && auditProgress != null) {
            CompactAuditProgress(progress = auditProgress)
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        // Circular timer dial (right side)
        CircularTimerDial(
            progress = fraction,
            remainingSeconds = timer.remainingSeconds,
            isExpired = timer.isExpired,
            color = timerColor,
            pulseAlpha = pulseAlpha
        )
    }
}

/**
 * Compact audit progress indicator showing verified/pending counts in a small pill.
 */
@Composable
private fun CompactAuditProgress(
    progress: AuditProgress,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(validBoxColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${progress.verifiedCount}",
                    color = White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(partialReadColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${progress.pendingCount}",
                    color = White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            if (progress.mismatchCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(qtyMismatchedColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${progress.mismatchCount}",
                        color = White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Circular dial timer that shows progress as an arc that empties anti-clockwise.
 */
@Composable
private fun CircularTimerDial(
    progress: Float,  // 1.0 = full, 0.0 = empty
    remainingSeconds: Int,
    isExpired: Boolean,
    color: Color,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val dialSize = 48.dp
    val strokeWidth = 4.dp

    Box(
        modifier = modifier.size(dialSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = (size.minDimension - strokeWidth.toPx()) / 2,
                style = Stroke(width = strokeWidth.toPx())
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 360f * progress
            drawArc(
                color = color.copy(alpha = pulseAlpha),
                startAngle = -90f,  // Start from top
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(
                    size.width - strokeWidth.toPx(),
                    size.height - strokeWidth.toPx()
                ),
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2)
            )
        }

        Text(
            text = if (isExpired) "!" else "$remainingSeconds",
            color = color.copy(alpha = pulseAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
