package com.zebra.ai.ppod.ui.components.mainScreen


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CrossHair(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(150.dp)) {
        drawRect(color=Color.Yellow,style = Stroke(width = 6f))
        drawLine(color=Color.Yellow,start = Offset(this.size.width/2,0f),end = Offset(this.size.width/2,30f),strokeWidth = 6f)
        drawLine(color=Color.Yellow,start = Offset(this.size.width/2,this.size.height),end = Offset(this.size.width/2,this.size.height - 30f),strokeWidth = 6f)
        drawLine(color=Color.Yellow,start = Offset(0f,this.size.height / 2f),end = Offset(30f,this.size.height/2),strokeWidth = 6f)
        drawLine(color=Color.Yellow,start = Offset(this.size.width,this.size.height/2),end = Offset(this.size.width -30f,this.size.height/2),strokeWidth = 6f)

    }
}