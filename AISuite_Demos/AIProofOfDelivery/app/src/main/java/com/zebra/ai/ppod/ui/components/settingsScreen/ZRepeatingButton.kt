package com.zebra.ai.ppod.ui.components.settingsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ZRepeatingButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    text: String,
    restricted: Boolean,
    restrictedColor: Color = MaterialTheme.colorScheme.error,
    ) {

    val currentOnClick by rememberUpdatedState(onClick)
    val currentRestricted by rememberUpdatedState(restricted)

    Text(modifier = modifier
        .width(20.dp)
        .wrapContentHeight()
        .background(
            color = if (restricted) restrictedColor else Color(0x803D5AFE), shape= RoundedCornerShape(6.dp))
        .border(1.dp,
            color = if (restricted) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
            shape= RoundedCornerShape(6.dp))
        .clip(RoundedCornerShape(6.dp))
        .pointerInput(Unit) {
            coroutineScope {
                detectTapGestures(
                    onPress = {
                        if (!currentRestricted) currentOnClick()
                        val pressJob = launch {
                            delay(400)
                            while (true) {
                                if (!currentRestricted) currentOnClick()
                                delay(100)
                            }
                        }
                        try {
                            awaitRelease()
                        } finally {
                            pressJob.cancel()
                        }
                    }
                )
            }
        }
        .padding(top=2.dp,bottom=2.dp, start=6.dp,end=6.dp),
        text=text,
        color = if (restricted) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center)
}
