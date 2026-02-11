package com.zebra.ai.ppod.ui.components.settingsScreen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zebra.ai.ppod.R
import com.zebra.ai.ppod.repositories.PreferenceItem
import com.zebra.ai.ppod.repositories.PreferenceType
import com.zebra.ai.ppod.repositories.ZPreferences

@Composable
fun ZPreferenceScreen(
    preferences: ZPreferences,
    onClose: () -> Unit = {}
) {
    val preferencesItems: List<PreferenceItem> by preferences.getPreferenceItems().collectAsStateWithLifecycle()

    BackHandler() {
        preferences.commit()
        onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(12.dp, 10.dp, 12.dp, 0.dp)
    )
    {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {

            Icon(
                modifier = Modifier.wrapContentSize(),
                painter = painterResource(id = R.drawable.ic_settings), contentDescription = null
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .padding(6.dp, 0.dp, 0.dp, 0.dp),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ), text = "Settings"
            )

            Icon(
                modifier = Modifier.wrapContentSize()
                    .clickable(enabled = true, onClick = {
                        preferences.commit()
                        onClose() }),
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "close"
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                preferencesItems.size,
                key = { idx -> preferencesItems[idx].key }
            ) { idx ->
                val preferenceItem = preferencesItems[idx]
                if (!preferenceItem.hidden) {
                    when (preferencesItems[idx].type) {
                        PreferenceType.CATEGORY -> {
                            ZPreferenceHeader(
                                title = preferenceItem.title,
                                description = preferenceItem.description
                            )
                        }

                        PreferenceType.BOOL -> {
                            ZPreferenceSwitch(
                                title = preferenceItem.title,
                                restricted = preferenceItem.restricted,
                                enabled = if (preferenceItem.restricted) preferenceItem.restrictedValue as Boolean else preferenceItem.value as Boolean,
                                onCheckChanged = { value ->
                                    preferences[preferenceItem.key] = value
                                }
                            )
                        }

                        PreferenceType.CHOICE -> {
                            ZPreferenceChoice(
                                title = preferenceItem.title,
                                selected = if (preferenceItem.restricted) preferenceItem.restrictedValue as String else preferenceItem.value as String,
                                restricted = preferenceItem.restricted,
                                entries = preferenceItem.entries,
                                onOptionSelected = { value ->
                                    preferences[preferenceItem.key] = value
                                }
                            )
                        }

                        PreferenceType.FLOAT -> {
                            ZPreferenceFloat(
                                title = preferenceItem.title,
                                value = if (preferenceItem.restricted) preferenceItem.restrictedValue as Float else preferenceItem.value as Float,
                                minValue = preferenceItem.minValue as Float?,
                                maxValue = preferenceItem.maxValue as Float?,
                                decimalPoints = preferenceItem.decimalPoints,
                                restricted = preferenceItem.restricted,
                                onChange = { value ->
                                    Log.i("RRR", "new Value $value")
                                    preferences[preferenceItem.key] = value
                                }
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
