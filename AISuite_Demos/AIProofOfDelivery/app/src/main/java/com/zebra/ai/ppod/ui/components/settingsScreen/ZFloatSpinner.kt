package com.zebra.ai.ppod.ui.components.settingsScreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.roundToInt

private fun Float.round(decimals: Int): Float {
    val factor = 10.0.pow(decimals.toDouble()).toFloat()
    return (this * factor).roundToInt() / factor
}

@Composable
fun ZFloatSpinner(
    modifier: Modifier = Modifier,
    value: Float,
    minValue: Float? = null,
    maxValue: Float? = null,
    decimalPoints: Int,
    restricted: Boolean = false,
    onChange: (Float) -> Unit = {}
){
    Row(modifier = modifier,
        verticalAlignment = CenterVertically){
        ZRepeatingButton(
            modifier = Modifier.padding(end=4.dp),
            text="-",
            restricted = restricted,
            onClick = {
                val newValue = (value - 0.01f).round(decimalPoints)
                if (minValue != null && newValue <= minValue) return@ZRepeatingButton
                onChange(newValue)
            }
        )

        Text(modifier = Modifier
            .wrapContentSize()
            .background(color = MaterialTheme.colorScheme.onSurface, shape= RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .padding(top=2.dp,bottom=2.dp, start=6.dp,end=6.dp),
            text = String.format("%.${decimalPoints}f",value),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.Center)

        ZRepeatingButton(
            modifier = Modifier.padding(start=4.dp),
            text="+",
            restricted = restricted,
            onClick = {
                val newValue = (value + 0.01f).round(decimalPoints)
                if (maxValue != null && newValue >= maxValue) return@ZRepeatingButton
                onChange(newValue)
            }
        )
    }
}