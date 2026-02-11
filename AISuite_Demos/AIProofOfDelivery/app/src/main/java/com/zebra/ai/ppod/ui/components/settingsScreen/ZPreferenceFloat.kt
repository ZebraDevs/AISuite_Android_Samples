package com.zebra.ai.ppod.ui.components.settingsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZPreferenceFloat(
    title: String,
    value: Float,
    minValue: Float? = null,
    maxValue: Float? = null,
    decimalPoints: Int,
    restricted: Boolean = false,
    onChange: (Float) -> Unit = {}
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(15.dp, 10.dp, 15.dp, 10.dp),
        verticalAlignment = CenterVertically) {

        Text(modifier = Modifier
            .weight(1f)
            .wrapContentHeight()
            .padding(0.dp, 10.dp, 0.dp, 10.dp),
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 14.sp,
            ),
            text = title)

        ZFloatSpinner(modifier = Modifier.wrapContentSize(),
            value = value,
            minValue = minValue,
            maxValue = maxValue,
            decimalPoints = decimalPoints,
            restricted = restricted,
            onChange = onChange
        )
    }
    HorizontalDivider(modifier = Modifier
        .padding(10.dp, 0.dp, 10.dp, 0.dp)
        .background(color = Color(0xff414141)))
}