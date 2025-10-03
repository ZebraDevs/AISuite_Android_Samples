// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.configurescreen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.boxYellow
import com.zebra.ai.barcodefinder.presentation.ui.theme.mainInverse
import com.zebra.ai.barcodefinder.presentation.ui.theme.surfaceDefaultInverse


@Composable
fun CameraOverlay(
    isVisible: Boolean,
    onClose: () -> Unit
) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AppDimensions.MediumPadding,
                    start = AppDimensions.SmallPadding,
                    end = AppDimensions.SmallPadding
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .alpha(AppDimensions.WeightFull)
                    .border(
                        width = AppDimensions.smallWidth,
                        color = boxYellow,
                        shape = RoundedCornerShape(size = AppDimensions.shape360)
                    )
                    .width(AppDimensions.shape325)
                    .height(AppDimensions.shape44)
                    .background(
                        color = surfaceDefaultInverse,
                        shape = RoundedCornerShape(size = AppDimensions.shape360)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.dimension_12dp)// Ensure Row takes full width
                ) {
                    Image(
                        imageVector = ImageVector.vectorResource(id = R.drawable.plus_1),
                        contentDescription = null,
                        modifier = Modifier
                            .width(AppDimensions.dimension_24dp)
                            .height(AppDimensions.dimension_24dp)
                    )

                    ZebraText(
                        modifier = Modifier
                            .weight(1f) // Allow text to take available space
                            .padding(start = AppDimensions.MediumPadding)
                            .width(AppDimensions.dimension_200dp),
                        textValue = stringResource(R.string.cameraoverlay_toast_message),
                        style = TextStyle(
                            fontSize = AppDimensions.dialogTextFontSizeSmall,
                            lineHeight = AppDimensions.dialogTextFontSizeLarge
                        ),
                        textColor = mainInverse
                    )

                    IconButton(
                        modifier = Modifier.background(color = surfaceDefaultInverse.copy(alpha = 0.1f)), // Temporary background for visibility
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.close),
                            contentDescription = null,
                            tint = mainInverse
                        )
                    }
                }
            }
        }
    }
}
