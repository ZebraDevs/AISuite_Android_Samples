package com.zebra.ai.ppod.ui.components.settingsScreen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ZRadioButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    restricted: Boolean = false,
    onClick: (() -> Unit) = {},
    size: Dp = 16.dp,
    selectedColor: Color = Color(0xFFF8D249),
    unselectedColor: Color = MaterialTheme.colorScheme.onSurface,
    dotColor: Color = Color(0xFFF8D249),
    restrictedColor: Color = MaterialTheme.colorScheme.error

) {
    val interactionSource = remember { MutableInteractionSource() }
    val radioColor by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = tween(200), label = "radioColor"
    )
    val dotRadius by animateDpAsState(
        targetValue = if (selected) size / 4 else 0.dp,
        animationSpec = tween(200), label = "dotRadius"
    )

    Canvas(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = { if (!restricted ) onClick() },
                role = Role.RadioButton,
                interactionSource = interactionSource,
            )
            .size(size)
            .padding(3.dp)
    ) {
        val strokeWidth = 2.dp.toPx()
        drawCircle(
            color = radioColor,
            radius = (size / 2).toPx(),
            style = Stroke(width = strokeWidth)
        )

        if (restricted) {
            drawCircle(color = restrictedColor, radius = (size / 2).toPx(), style = Fill)
        }

        if (selected) {
            drawCircle(color = dotColor, radius = dotRadius.toPx(), style = Fill)
        }
    }
}