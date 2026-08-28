package com.zebra.ai.palletchecker.presentation.ui.compose.components

import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.helpers.TouchEvent
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.model.PBoxUIModel
import com.zebra.ai.palletchecker.presentation.ui.theme.BarcodeDetectedFill
import com.zebra.ai.palletchecker.presentation.ui.theme.ClassId2Color
import com.zebra.ai.palletchecker.presentation.ui.theme.ClassId3Color
import com.zebra.ai.palletchecker.presentation.ui.theme.NotDetectedMainLblColor
import com.zebra.ai.palletchecker.presentation.ui.theme.OtherClassId
import com.zebra.ai.palletchecker.presentation.ui.theme.labelColor
import com.zebra.ai.palletchecker.presentation.ui.theme.partialReadColor
import com.zebra.ai.palletchecker.presentation.ui.theme.qtyMismatchedColor
import com.zebra.ai.palletchecker.presentation.ui.theme.validBoxColor
import com.zebra.ai.palletchecker.presentation.viewmodel.BarcodeQuantityResult
import com.zebra.ai.palletchecker.presentation.viewmodel.PROCESS_TYPE
import kotlin.math.roundToInt

val contentPadding = 20f

@OptIn(ExperimentalTextApi::class)
@Composable
fun BarcodeQuantityCanvas(
    modifier: Modifier = Modifier,
    results: List<BarcodeQuantityResult>,
    onTouch: (String) -> Unit = {}
) {
    val textMeasure = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .TouchEvent({ offset ->
                val selectedBarcode = results.indices.mapNotNull {
                    if (results[it].rect.contains(offset)) results[it].barcode else null
                }

                if (selectedBarcode.isNotEmpty())
                    onTouch(selectedBarcode[0])

            })
    ) {


        for (box in results.indices) {
            if (results[box].qty == 0) {
                StrokeRect(
                    results[box].rect,
                    if (results[box].isDetected) Color.Green else Color.Blue,
                    5f
                )
            } else {
                FillRectText(
                    "${results[box].qty}",
                    results[box].rect,
                    BarcodeDetectedFill,
                    textMeasure
                )
            }
        }

    }
}

/**
 * Draws LIVE wand bounding boxes with validation icons (✓ / ! / ✗).
 *
 * Similar to [palletBoxCanvas] on the snap results screen but designed for the live
 * wand viewfinder. Each box is drawn with:
 *  - A colour-coded stroke matching its validation state
 *  - A centred icon: checkmark (green/verified), warning (yellow/partial), cross (red/mismatch)
 *
 * Boxes whose validation hasn't been determined yet (NOT_DETECTED) are drawn with a
 * thin neutral stroke and no icon so they don't distract from resolved boxes.
 */
@Composable
fun wandLiveValidationCanvas(
    modifier: Modifier = Modifier,
    list: List<PBoxUIModel>
) {
    val localVectors = LocalVectorPainter.current
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        for (box in list) {
            val color = when (box.validation) {
                BOX_VALIDATION.VERIFIED -> validBoxColor
                BOX_VALIDATION.PARTIAL_DETECTION -> partialReadColor
                BOX_VALIDATION.MISMATCH_QTY -> qtyMismatchedColor
                BOX_VALIDATION.NOT_DETECTED -> NotDetectedMainLblColor
            }

            // Bounding box stroke
            StrokeRect(box.boundingBox, color, 10f)

            // Validation icon — only for resolved states
            if (box.validation == BOX_VALIDATION.VERIFIED ||
                box.validation == BOX_VALIDATION.MISMATCH_QTY ||
                box.validation == BOX_VALIDATION.PARTIAL_DETECTION
            ) {

                val rawSize = box.boundingBox.height * 0.4f
                val iconSize = rawSize.coerceIn(24f, 80f)
                val iconOffset = Offset(
                    box.boundingBox.center.x - iconSize / 2f,
                    box.boundingBox.center.y - iconSize / 2f
                )
                drawValidationIcon(iconOffset,Size(iconSize, iconSize), vectors = localVectors, density = density, box = box)
            }
        }
    }
}

val debugPrimaryBarcodeColor = Color(0xFF00E676)
val debugQtyBarcodeColor = Color(0xFF40C4FF)
val debugOtherBarcodeColor = Color(0xFFB0BEC5)
val debugPrimaryBadgeBg = Color(0xCC00C853)
val debugQtyBadgeBg = Color(0xCC0288D1)
val debugOtherBadgeBg = Color(0xCC455A64)

val debugTrackIdColor = Color(0xFFFFD740)

val debugTrackIdBadgeBg = Color(0xCCBF6000)

/**
 * Truncates a string to [maxLen] characters, appending "…" if truncated.
 */
private fun truncateBarcode(data: String, maxLen: Int = 20): String =
    if (data.length <= maxLen) data else data.take(maxLen) + "…"

/**
 * DEBUG MODE: Draws pre-snap bounding boxes during live preview phase.
 * Shows all detected boxes from the pallet and box model with cyan dashed-style borders.
 * Optionally shows spatial index numbers on each box for debugging the spatial map formation.
 * Optionally shows barcode data labels on each box with color differentiation for primary barcodes.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun preSnapDebugCanvas(
    modifier: Modifier = Modifier,
    boxes: List<PBoxUIModel>,
    showIndices: Boolean = false,
    showBarcodeLabels: Boolean = false,
    showTrackingIds: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()
    val debugColor = Color.Cyan
    val debugStrokeWidth = 4f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        boxes.forEachIndexed { index, box ->
            drawRect(
                color = debugColor,
                style = Stroke(width = debugStrokeWidth),
                topLeft = box.boundingBox.topLeft,
                size = box.boundingBox.size
            )

            if (showIndices) {
                val indexText = "$index"
                val textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    background = debugColor.copy(alpha = 0.8f)
                )
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(indexText),
                    style = textStyle
                )

                val badgePadding = 4f
                val badgeX = box.boundingBox.left + badgePadding
                val badgeY = box.boundingBox.top + badgePadding

                drawRect(
                    color = debugColor.copy(alpha = 0.85f),
                    topLeft = Offset(badgeX, badgeY),
                    size = Size(
                        textLayoutResult.size.width + 8f,
                        textLayoutResult.size.height + 4f
                    )
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = indexText,
                    topLeft = Offset(badgeX + 4f, badgeY + 2f),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (showBarcodeLabels) {
                drawBarcodeLabelOverlays(
                    box = box,
                    textMeasurer = textMeasurer,
                    yStartOffset = if (showIndices) 28f else 0f
                )
            }

            if (showTrackingIds) {
                drawTrackingIdOverlay(box = box, textMeasurer = textMeasurer)
            }
        }
    }
}

/**
 * DEBUG MODE: Draws barcode data labels on wand-mode live bounding boxes.
 * Each box's associated barcodes are shown as text labels with color differentiation
 * for primary/unique barcodes (green) vs other barcodes (grey).
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun wandDebugBarcodeCanvas(
    modifier: Modifier = Modifier,
    boxes: List<PBoxUIModel>,
    showBarcodeLabels: Boolean = true,
    showTrackingIds: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        boxes.forEach { box ->
            if (showBarcodeLabels) {
                drawBarcodeLabelOverlays(
                    box = box,
                    textMeasurer = textMeasurer,
                    yStartOffset = 0f
                )
            }
            if (showTrackingIds) {
                drawTrackingIdOverlay(box = box, textMeasurer = textMeasurer)
            }
        }
    }
}

/**
 * Draws live viewfinder bounding boxes styled with state-specific strokes and centered validation icons (✓/!/✗).
 * Unvalidated boxes (NOT_DETECTED) use a thin, neutral stroke without icons to prevent visual distraction.
 * Provides real-time feedback matching the static pallet box decorations on the snap results screen.
 */
private fun DrawScope.drawBarcodeLabelOverlays(
    box: PBoxUIModel,
    textMeasurer: TextMeasurer,
    yStartOffset: Float = 0f
) {
    val barcodes = box.palletBarcodes.filter { it.data.isNotEmpty() }
    if (barcodes.isEmpty()) return

    val labelPadding = 4f
    val labelSpacing = 2f
    var currentY = box.boundingBox.top + labelPadding + yStartOffset

    barcodes.forEach { barcode ->
        val isPrimary = barcode.isMainBarcode
        val isQty = !isPrimary && barcode.isQtyBarcode
        val labelText = truncateBarcode(barcode.data)
        val prefix = when {
            isPrimary -> "★ "
            isQty -> "◆ "
            else -> ""
        }
        val displayText = "$prefix$labelText"

        val textColor = when {
            isPrimary -> debugPrimaryBarcodeColor
            isQty -> debugQtyBarcodeColor
            else -> debugOtherBarcodeColor
        }
        val badgeBg = when {
            isPrimary -> debugPrimaryBadgeBg
            isQty -> debugQtyBadgeBg
            else -> debugOtherBadgeBg
        }
        val fontWeight = if (isPrimary || isQty) FontWeight.Bold else FontWeight.Normal

        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(displayText),
            style = TextStyle(
                color = textColor,
                fontSize = 10.sp,
                fontWeight = fontWeight
            )
        )

        val badgeX = box.boundingBox.left + labelPadding
        val badgeWidth = textLayoutResult.size.width + 8f
        val badgeHeight = textLayoutResult.size.height + 4f

        drawRect(
            color = badgeBg,
            topLeft = Offset(badgeX, currentY),
            size = Size(badgeWidth, badgeHeight)
        )

        drawText(
            textMeasurer = textMeasurer,
            text = displayText,
            topLeft = Offset(badgeX + 4f, currentY + 2f),
            style = TextStyle(
                color = textColor,
                fontSize = 10.sp,
                fontWeight = fontWeight
            )
        )

        currentY += badgeHeight + labelSpacing
    }
}

/**
 * DEBUG MODE: Draws the tracking ID (stableKey / trackId) at the TOP-RIGHT corner of a box.
 * Uses an amber badge to visually distinguish it from barcode data labels (top-left, green/grey).
 * Shows the stableKey if non-empty, otherwise shows the numeric trackId.
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawTrackingIdOverlay(
    box: PBoxUIModel,
    textMeasurer: TextMeasurer
) {
    val label = when {
        box.stableKey.isNotEmpty() -> "🔑 ${box.stableKey}"
        box.trackId >= 0L -> "🔑 #${box.trackId}"
        else -> return
    }

    val labelPadding = 4f

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(label),
        style = TextStyle(
            color = debugTrackIdColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    )

    val badgeWidth = textLayoutResult.size.width + 8f
    val badgeHeight = textLayoutResult.size.height + 4f

    val badgeX = box.boundingBox.right - badgeWidth - labelPadding
    val badgeY = box.boundingBox.top + labelPadding


    drawRect(
        color = debugTrackIdBadgeBg,
        topLeft = Offset(badgeX, badgeY),
        size = Size(badgeWidth, badgeHeight)
    )

    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(badgeX + 4f, badgeY + 2f),
        style = TextStyle(
            color = debugTrackIdColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
fun palletBoxCanvas(
    modifier: Modifier = Modifier,
    list: List<PBoxUIModel>,
    processType: PROCESS_TYPE = PROCESS_TYPE.CAPTURE_PALLET_BOX
) {
    val enableBarcodeAndLabelsUI = false
    val localVectors = LocalVectorPainter.current
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)

    ) {

        for (box in list) {
            if (processType == PROCESS_TYPE.CAPTURE_PALLET_BOX && enableBarcodeAndLabelsUI) {
                for (labels in box.palletLabels) {
                    StrokeRect(labels.boundingBox, labelColor, 5f)
                }
                for (barcode in box.palletBarcodes) {
                    StrokeRect(
                        barcode.boundingBox,
                        if (barcode.data.isNotEmpty()) Color.Green else Color.Blue,
                        5f
                    )
                }
            }

            StrokeRect(
                box.boundingBox, color = when (box.validation) {
                    BOX_VALIDATION.VERIFIED ->
                        validBoxColor

                    BOX_VALIDATION.PARTIAL_DETECTION ->
                        partialReadColor

                    BOX_VALIDATION.MISMATCH_QTY ->
                        qtyMismatchedColor

                    BOX_VALIDATION.NOT_DETECTED ->
                        if (box.classId == 1) {
                            NotDetectedMainLblColor
                        } else if (box.classId == 3) {
                            ClassId3Color
                        } else if (box.classId == 2) {
                            ClassId2Color
                        } else {
                            OtherClassId
                        }
                }, 12f
            )

            var imageSize = Size(100f, 100f)
            var size = Size(box.boundingBox.height * 0.5f, box.boundingBox.height * 0.5f)
            if (size.width > 60f) {
                size = imageSize
            } else if (size.width < 30f) {
                size = Size(30f, 30f)
            }
            val offsetX = box.boundingBox.center.x - (size.width / 2)
            val offsetY = box.boundingBox.center.y - (size.height / 2)

            val scaleFactor = 1f

            if (box.validation == BOX_VALIDATION.VERIFIED || box.validation == BOX_VALIDATION.MISMATCH_QTY || box.validation == BOX_VALIDATION.PARTIAL_DETECTION) {

                drawValidationIcon(offset = Offset(offsetX,offsetY),size = size, density = density, vectors = localVectors,box = box)
            }
        }
    }
}


@Composable
fun palletBoxCanvasForPIP(
    modifier: Modifier = Modifier,
    list: List<PBoxUIModel>,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        for (box in list) {
            FillRect(
                box.boundingBox, color = when (box.validation) {
                    BOX_VALIDATION.VERIFIED ->
                        validBoxColor

                    BOX_VALIDATION.PARTIAL_DETECTION ->
                        partialReadColor

                    BOX_VALIDATION.MISMATCH_QTY ->
                        qtyMismatchedColor

                    BOX_VALIDATION.NOT_DETECTED ->
                        if (box.classId == 1) {
                            NotDetectedMainLblColor
                        } else if (box.classId == 3) {
                            ClassId3Color
                        } else if (box.classId == 2) {
                            ClassId2Color
                        } else {
                            OtherClassId
                        }
                }
            )

        }
    }
}

fun DrawScope.StrokeRect(
    box: androidx.compose.ui.geometry.Rect,
    color: Color,
    strokeWidth: Float = 1f
) {
    drawRect(
        color = color, style = Stroke(strokeWidth), topLeft = box.topLeft, size = box.size
    )
}

fun DrawScope.FillRect(box: androidx.compose.ui.geometry.Rect, color: Color) {
    drawRect(
        color = color.copy(alpha = 0.5f), style = Fill, topLeft = box.topLeft, size = box.size
    )
}


fun DrawScope.FillRectText(
    text: String,
    box: androidx.compose.ui.geometry.Rect,
    color: Color,
    textMeasure: TextMeasurer
) {

    val textLayoutResult: TextLayoutResult = textMeasure.measure(
        text = AnnotatedString(text),
        style = TextStyle(
            fontSize = 20.sp, // Set the desired text size here
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    )
    drawRect(
        color = color, style = Fill, topLeft = box.topLeft, size = box.size
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            box.center.x - (textLayoutResult.size.width / 2),
            box.center.y - (textLayoutResult.size.height / 2)
        )
    )
}


fun DrawScope.Text(box: Rect, text: String, color: Color, textMeasure: TextMeasurer) {

    drawText(
        textMeasurer = textMeasure, text = text,

        topLeft = Offset(
            box.left.toFloat() + contentPadding,
            box.top.toFloat() + contentPadding,
        ),
        style = TextStyle(
            color = color,
            fontSize = 14.sp
        )
    )
}
//When I translate and scale vector resources inside the canvas, the coordinate system isn't clearing properly.
// This leaves behind a ghost trail, making it look like the icon is being drawn twice.
fun DrawScope.drawValidationIcon(offset: Offset, size: Size, scaleFactor: Float, vectors: Vectors, box: PBoxUIModel) {

    translate(left = offset.x, top = offset.y) {
        scale(scaleFactor) {

            with(
                when (box.validation) {
                    BOX_VALIDATION.VERIFIED -> vectors.success
                    BOX_VALIDATION.MISMATCH_QTY -> vectors.failure
                    else -> vectors.partial
                }
            ) {

                draw(
                    size = size, alpha = 1f, colorFilter = ColorFilter.tint(
                        when (box.validation) {
                            BOX_VALIDATION.VERIFIED -> validBoxColor
                            BOX_VALIDATION.MISMATCH_QTY -> qtyMismatchedColor
                            else -> partialReadColor
                        }

                    )
                )
            }
        }
    }

}

fun DrawScope.drawValidationIcon(offset:Offset, size: Size, vectors: Vectors, density: Density, box: PBoxUIModel) {
    val bitmap = painterToBitmap(
        when (box.validation) {
            BOX_VALIDATION.VERIFIED -> vectors.success
            BOX_VALIDATION.MISMATCH_QTY -> vectors.failure
            else -> vectors.partial
        }, size, density, layoutDirection
    )
    drawImage(
        image = bitmap,
        topLeft =offset,
        colorFilter = ColorFilter.tint(
            when (box.validation) {
                BOX_VALIDATION.VERIFIED -> validBoxColor
                BOX_VALIDATION.MISMATCH_QTY -> qtyMismatchedColor
                else -> partialReadColor
            }
        )
    )
}

fun painterToBitmap(
    painter: Painter,
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection
): ImageBitmap {
    val imageBitmap = ImageBitmap(
        width = size.width.roundToInt(),
        height = size.height.roundToInt()
    )
    val canvas = Canvas(imageBitmap)

    CanvasDrawScope().draw(density, layoutDirection, canvas, size) {
        with(painter) {
            draw(size = this@draw.size)
        }
    }
    return imageBitmap
}


data class Vectors(val success: VectorPainter, val failure: VectorPainter, val partial: VectorPainter)

val LocalVectorPainter = staticCompositionLocalOf <Vectors>{
    error("No Vectors provided!")
}

@Composable
fun VectorProvider(content: @Composable () ->Unit ) {
    val successVector: ImageVector = ImageVector.vectorResource(id = R.drawable.box_verify_success)
    val failureVector: ImageVector = ImageVector.vectorResource(id = R.drawable.box_verify_failed)
    val partialVector: ImageVector = ImageVector.vectorResource(id = R.drawable.box_verify_partialy)
    val success = rememberVectorPainter(successVector)
    val failure = rememberVectorPainter(failureVector)
    val partial = rememberVectorPainter(partialVector)

    val vectors = Vectors(success,failure,partial)
    CompositionLocalProvider(LocalVectorPainter provides vectors) {
        content()
    }
}