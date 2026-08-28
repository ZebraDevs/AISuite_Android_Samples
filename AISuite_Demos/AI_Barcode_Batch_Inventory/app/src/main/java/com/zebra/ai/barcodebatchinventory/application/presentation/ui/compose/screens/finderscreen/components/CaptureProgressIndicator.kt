// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Circular progress indicator for capture session.
 * Shows progress during capture with "Scanning" text.
 * After completion, shows a green ring for success (barcodes found) or red ring for failure (no barcodes).
 */
@Composable
fun CaptureProgressIndicator(
    progress: Float,
    showCheckmark: Boolean,
    isSuccess: Boolean = true,
    decodedBarcodeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100),
        label = "progress"
    )

    val animatedCount by animateIntAsState(
        targetValue = decodedBarcodeCount,
        animationSpec = if (decodedBarcodeCount == 0) snap() else tween(durationMillis = 600),
        label = "barcodeCount"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .semantics {
                contentDescription = "DecodedCount"
                stateDescription = decodedBarcodeCount.toString()
            },
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent dark background for readability
        Canvas(modifier = Modifier.size(80.dp)) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = size.minDimension / 2f
            )
        }

        if (showCheckmark) {
            // Green = all decoded, White = partial, Red = nothing decoded
            val completionColor = when {
                isSuccess -> Color.Green
                decodedBarcodeCount > 0 -> Color.White
                else -> Color.Red
            }

            Canvas(modifier = Modifier.size(80.dp)) {
                val strokeWidth = 6.dp.toPx()

                drawArc(
                    color = completionColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Decoded barcode count in the center of completion ring
            Text(
                text = "$decodedBarcodeCount",
                color = completionColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        } else {
            // Show circular progress indicator with "Scanning" text
            Canvas(modifier = Modifier.size(80.dp)) {
                val strokeWidth = 6.dp.toPx()

                // Background circle (gray)
                drawArc(
                    color = Color.Gray.copy(alpha = 0.3f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress arc (white)
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Decoded barcode count in the center
            Text(
                text = "$animatedCount",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

