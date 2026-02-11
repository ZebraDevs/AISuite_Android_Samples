package com.zebra.ai.ppod.ui.components.settingsScreen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ZSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    restricted: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit) = {},
    width: Dp = 30.dp,
    height: Dp = 20.dp,
    thumbSize: Dp = 18.dp,
    checkedTrackColor: Color = Color(0xFF0073E6),
    uncheckedTrackColor: Color = Color(0xFFAFB6C2),
    restrictedColor: Color = MaterialTheme.colorScheme.error,
    thumbColor: Color = Color.White) {

    val interactionSource = remember { MutableInteractionSource() }
    val trackColor by animateColorAsState(
        targetValue = if (restricted) restrictedColor else if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = tween(300), label = "trackColor"
    )
    val gap = (height - thumbSize) / 2
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) (width - thumbSize - gap) else gap,
        animationSpec = tween(300), label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .offset(y=height / 2)
            .background(trackColor, shape = RoundedCornerShape(50))
            .toggleable(
                value = checked,
                onValueChange = { if (!restricted) onCheckedChange.invoke(it) },
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .background(color = thumbColor, shape = CircleShape)
        )
    }
}