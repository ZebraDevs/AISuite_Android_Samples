package com.zebra.ai.ppod.ui.components.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CountDownTimer(
    timer: Int,
    onTick: (() -> Unit) = {}
) {

    LaunchedEffect(timer) {
        delay(1000L)
        onTick()
    }

    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(modifier = Modifier
            .padding(bottom = 50.dp)
            .size(60.dp)
            .background(color = Color(0x80000000), shape = CircleShape)
            .border(width = 2.dp, color= Color.White, shape = CircleShape)
            .clip(shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$timer",
                textAlign = TextAlign.Center,
                fontSize = 40.sp,
                color = Color.White)
        }
    }
}
