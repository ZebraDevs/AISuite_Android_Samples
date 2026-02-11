package com.zebra.ai.ppod.ui.components.settingsScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.ai.ppod.R

@Composable
fun ZPreferenceHeader(
    title: String = "",
    description: String? = null,
) {
    Column() {
        Text(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(10.dp, 20.dp, 10.dp, 10.dp),
            style = TextStyle(
                color = Color(0xFF00ACC1),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            text = title)
        if (description == null) return

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .background(color = Color(0x803D5AFE), shape = RoundedCornerShape(6.dp))
            .border(
                1.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(6.dp)
        ) {
            Image(modifier = Modifier
                .size(28.dp, 28.dp)
                .padding(6.dp),
                painter = painterResource(id = R.drawable.ic_blue_alert), contentDescription = null)


            Text(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(6.dp),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                ),
                text = description
            )
        }
    }
}
