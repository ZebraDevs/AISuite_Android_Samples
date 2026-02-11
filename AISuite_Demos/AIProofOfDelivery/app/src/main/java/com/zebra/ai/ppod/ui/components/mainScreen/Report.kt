package com.zebra.ai.ppod.ui.components.mainScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.ai.ppod.R

enum class ReportType {
    SUCCESS,
    INFO,
    ERROR
}

@Composable
fun Report(
    reportType: ReportType = ReportType.SUCCESS,
    header: String? = null,
    issues: List<String>? = null,
    onRetake: (() -> Unit)? = null,
    onContinue: (() -> Unit)? = null,
    onClose:(() -> Unit)? = null,
) {
    BackHandler() {
        onClose?.invoke()
    }

    val gradientColors = listOf(Color(0xFF888B94), Color(0xDD7F8089))
    val iconBackground = when (reportType) {
        ReportType.SUCCESS -> Color(0xFF1CBC37)
        ReportType.INFO -> Color(0x803D5AFE)
        ReportType.ERROR -> Color(0xFFD32F2F)
    }

    val icon = when (reportType) {
        ReportType.SUCCESS -> R.drawable.ic_correct
        ReportType.INFO -> R.drawable.ic_alert
        ReportType.ERROR -> R.drawable.ic_alert
    }

    Column(modifier = Modifier.fillMaxWidth()
        .wrapContentHeight()
        .padding(top = 50.dp,start = 50.dp,end = 50.dp)
        .background(brush = Brush.verticalGradient( colors = gradientColors),
            shape = RoundedCornerShape(6.dp))
        .border(width = 1.dp,color = Color(0xFFEAEAEA), shape = RoundedCornerShape(6.dp))
        .padding(8.dp)
    ) {

        Row(modifier = Modifier.fillMaxWidth()
            .wrapContentHeight()
        ) {

            Image(
                modifier = Modifier.size(28.dp)
                    .background(color = iconBackground, shape = RoundedCornerShape(6.dp))
                    .border(width = 1.dp,color = Color.White, shape = RoundedCornerShape(6.dp))
                    .padding(6.dp),
                painter = painterResource(icon),
                contentDescription = null
            )

            //Title
            Text(
                modifier = Modifier.wrapContentHeight()
                    .weight(1f)
                    .padding(6.dp)
                    .align(CenterVertically),
                text = header ?: "",
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
            )

            Image(modifier = Modifier.size(10.dp)
                .clickable(enabled = true, onClick = { onClose?.invoke() }),
                alignment = Alignment.TopEnd,
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "close")
        }

        // Draw Message Row.
        Column(modifier = Modifier.wrapContentHeight()
            .padding(start = 20.dp)
            ) {
            issues?.forEach {
                ReportLine(it)
            }
        }

        // Buttons
        if (onRetake != null || onContinue != null) {
            Row(Modifier.fillMaxWidth()
                .wrapContentHeight()
                .padding(top=20.dp),
                horizontalArrangement = Arrangement.SpaceAround) {

                Text(
                    modifier = Modifier.wrapContentSize()
                        .background(color=Color(0xFFFFB1B1), shape = RoundedCornerShape(6.dp))
                        .border(width = 1.dp,color = Color.White,shape = RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .padding(top=10.dp,start=26.dp,end=26.dp, bottom = 10.dp)
                        .clickable(enabled = true, onClick = { onContinue?.invoke() }),
                        text = stringResource(R.string.continue_text),
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFFAA0303)
                        ))


                Text(
                    modifier = Modifier.wrapContentSize()
                        .background(color=Color(0xFF84D692), shape = RoundedCornerShape(6.dp))
                        .border(width = 1.dp,color = Color.White,shape = RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .padding(top=10.dp,start=26.dp,end=26.dp, bottom = 10.dp)
                        .clickable(enabled = true, onClick = { onRetake?.invoke() }),
                    text = stringResource(R.string.retake_text),
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF005F0F)
                    ))
            }
        }
    }
}

@Composable
fun ReportLine(
    text: String,
) {

    Row(modifier = Modifier.wrapContentHeight()
        .padding(top = 3.dp)) {

        Image(modifier = Modifier.size(10.dp)
            .align(CenterVertically),
            painter = painterResource(R.drawable.ic_alert),
            contentDescription = null)

        Text( modifier = Modifier.padding(start = 5.dp),
            text = text,
            style = TextStyle(
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
        )
    }
}
