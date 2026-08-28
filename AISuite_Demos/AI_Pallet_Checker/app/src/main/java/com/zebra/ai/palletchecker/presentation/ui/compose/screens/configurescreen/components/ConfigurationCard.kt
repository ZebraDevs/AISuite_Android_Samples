package com.zebra.ai.palletchecker.presentation.ui.compose.screens.configurescreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions
import com.zebra.ai.palletchecker.presentation.ui.theme.configCardContentColor
import com.zebra.ai.palletchecker.presentation.ui.theme.settingsCardIconTintColor
import com.zebra.ai.palletchecker.presentation.ui.theme.settingsCardTitleColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchCheckedThumbColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchCheckedTrackColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchUncheckedThumbColor
import com.zebra.ai.palletchecker.presentation.ui.theme.switchUncheckedTrackColor
import com.zebra.ai.palletchecker.presentation.ui.theme.white


@Composable
fun ConfigurationCard(
    title: String,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    onSelectionChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var isSelected by remember { mutableStateOf(selected) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = white),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.smallWidth),
        shape = RectangleShape
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth().background(color=configCardContentColor)
                .padding(vertical = AppDimensions.zeroPadding)
                .animateContentSize(animationSpec = spring())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(white)
                    .padding(AppDimensions.dimension_14dp)
                    .height(AppDimensions.dimension_24dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(.1f),
                    text = title,
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    fontWeight = FontWeight(AppDimensions.fontWeight500),
                    lineHeight = AppDimensions.linePaddingDefault,
                    color = settingsCardTitleColor // Darker text color
                )

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.filter_icon),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = settingsCardIconTintColor ,// Medium gray for icon,
                    modifier = Modifier.clickable{
                        onExpandToggle()
                    }
                )
                Spacer(modifier = Modifier.size(10.dp))
                Switch(
                    checked = isSelected,
                    onCheckedChange = if (enabled) {
                        { newValue ->
                            isSelected = newValue
                            onSelectionChange(newValue)
                        }
                    } else {
                        null
                    },
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = switchCheckedThumbColor,
                        checkedTrackColor = switchCheckedTrackColor,
                        uncheckedThumbColor = switchUncheckedThumbColor,
                        uncheckedTrackColor = switchUncheckedTrackColor
                    )
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(AppDimensions.animationDurationMillis)),
                exit = shrinkVertically(animationSpec = tween(AppDimensions.animationDurationMillis))
            ) {
                content()
            }
        }
    }
}