package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.finderscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.mainInverse
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.surfaceDefaultInverse

@Composable
fun ViewResultOverlay(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(
                top = AppDimensions.LargePadding,
                end = AppDimensions.LargePadding
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .alpha(AppDimensions.WeightFull)
                .background(
                    color = surfaceDefaultInverse.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(size = AppDimensions.shape360)
                )
                // Add the clickable modifier and remove the fixed width and height
                .clickable(onClick = onClick)
                // Add padding and align content
                .padding(
                    horizontal = AppDimensions.dimension_12dp,
                    vertical = AppDimensions.dimension_8dp
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.mdi_eye),
                    contentDescription = null,
                    tint = mainInverse
                )
                ZebraText(
                    modifier = Modifier
                        .padding(start = AppDimensions.dimension_10dp),
                    textValue = stringResource(R.string.finder_screen_view_result),
                    style = TextStyle(
                        fontSize = AppDimensions.dialogTextFontSizeSmall,
                        lineHeight = AppDimensions.dialogTextFontSizeLarge,
                        fontWeight = FontWeight.Medium
                    ),
                    textColor = mainInverse
                )
            }
        }
    }

}

@Preview
@Composable
fun PreviewOverlay() {
    ViewResultOverlay( onClick = {})
}
