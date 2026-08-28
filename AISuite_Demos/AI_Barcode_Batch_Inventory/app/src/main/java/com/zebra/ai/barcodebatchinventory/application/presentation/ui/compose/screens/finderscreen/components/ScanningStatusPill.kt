// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.finderscreen.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.zebra.ai.barcodebatchinventory.R
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.mainInverse
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.surfaceDefaultInverse
import kotlin.math.abs
import kotlin.math.min

@Composable
fun ScanningStatusPill() {
    val transition = rememberInfiniteTransition(label = "scanningStatus")
    val barPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "barPhase"
    )

    Box(
        modifier = Modifier
            .padding(
                top = AppDimensions.LargePadding,
                start = AppDimensions.LargePadding
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .alpha(AppDimensions.WeightFull)
                .background(
                    color = surfaceDefaultInverse.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(size = AppDimensions.shape360)
                )
                .padding(
                    horizontal = AppDimensions.dimension_12dp,
                    vertical = AppDimensions.dimension_8dp
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZebraText(
                    textValue = stringResource(R.string.finder_screen_scanning_status),
                    style = TextStyle(
                        fontSize = AppDimensions.dialogTextFontSizeSmall,
                        lineHeight = AppDimensions.dialogTextFontSizeLarge,
                        fontWeight = FontWeight.Medium
                    ),
                    textColor = mainInverse
                )

                Spacer(modifier = Modifier.width(AppDimensions.dimension_10dp))

                Canvas(
                    modifier = Modifier
                        .width(AppDimensions.scanningStatusPillIndicatorWidth)
                        .height(AppDimensions.scanningStatusPillIndicatorHeight)
                ) {
                    val barWidth = AppDimensions.scanningStatusPillBarWidth.toPx()
                    val gap = AppDimensions.scanningStatusPillBarGap.toPx()
                    val radius = AppDimensions.scanningStatusPillBarRadius.toPx()
                    repeat(3) { index ->
                        val barIndex = index.toFloat()
                        val distance = min(abs(barPhase - barIndex), abs(barPhase - barIndex - 3f))
                        val alpha = 0.24f + ((1f - min(distance, 1f)) * 0.62f)
                        drawRoundRect(
                            color = mainInverse.copy(alpha = alpha),
                            topLeft = Offset(index * (barWidth + gap), 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                }
            }
        }
    }
}
