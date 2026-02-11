package com.zebra.ai.ppod.ui.components.settingsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZPreferenceChoice(
    title: String,
    selected: String,
    restricted: Boolean = false,
    entries: List<Pair<String,String>>? = null,
    onOptionSelected: (String) -> Unit = {}
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(15.dp, 0.dp, 15.dp, 0.dp)
        )
        {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(0.dp, 10.dp, 0.dp, 10.dp),
                text = title,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Column(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(6.dp, 0.dp, 6.dp, 0.dp)
            ) {
                entries?.let {
                    it.forEach { idx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp, 6.dp, 0.dp, 6.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                        ) {

                            Text(
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .weight(1f),
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                ),
                                text = idx.first
                            )
                            ZRadioButton(selected = (selected == idx.second),
                                onClick = { onOptionSelected(idx.second) },
                                restricted = restricted)
                        }
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier
            .padding(10.dp, 0.dp, 10.dp, 0.dp)
            .background(color = Color(0xff414141))
        )
    }
}