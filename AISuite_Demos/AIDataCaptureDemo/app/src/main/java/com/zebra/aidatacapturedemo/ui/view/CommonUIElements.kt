package com.zebra.aidatacapturedemo.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDisabled
import com.zebra.aidatacapturedemo.ui.view.Variables.mainInverse
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary

data class RadioButtonData(
    val title: String,
    val description: Int?,
    val index: Int,
    val onItemSelected: (itemId: Int) -> Unit // A callback with a String parameter
)

@Composable
fun ListOfRadioButtonOptions(currentSelection: Int, radioOptions: List<RadioButtonData>) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[currentSelection]) }

    Column(Modifier.selectableGroup()) { // Modifier.selectableGroup() is crucial for accessibility
        radioOptions.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        color = Variables.surfaceDefault,
                        shape = RoundedCornerShape(size = 3.6.dp)
                    )
                    .padding(start = 14.4.dp, top = 8.dp, end = 14.4.dp, bottom = 8.dp)
                    .selectable(
                        selected = (item == selectedOption),
                        onClick = {
                            onOptionSelected(item)
                            item.onItemSelected(item.index)
                        }
                    ),
                horizontalArrangement = Arrangement.spacedBy(
                    10.799999237060547.dp,
                    Alignment.Start
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                    item.description?.let {
                        Text(
                            text = stringResource(it),
                            style = TextStyle(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(400),
                                color = Variables.mainDefault,
                            )
                        )
                    }

                }
                Spacer(modifier = Modifier.weight(1f))
                RadioButton(
                    modifier = Modifier
                        .background(color = Variables.surfaceDefault),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Variables.mainPrimary,
                        unselectedColor = Variables.mainDefault,
                        disabledSelectedColor = Variables.mainDisabled,
                        disabledUnselectedColor = Variables.mainDisabled
                    ),
                    selected = (item == selectedOption),
                    onClick = {
                        onOptionSelected(item)
                        item.onItemSelected(item.index)
                    }
                )
            }
        }
    }
}

data class SwitchOptionData(
    val titleId: Int,
    val onItemSelected: (title: String, selected: Boolean) -> Unit // A callback with a String parameter
)

@Composable
fun SwitchOption(currentValue: Boolean, switchOption: SwitchOptionData) {
    var title = stringResource(switchOption.titleId)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .wrapContentHeight()
            .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(size = 3.6.dp))
            .padding(start = 14.4.dp, top = 8.dp, end = 14.4.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .width(256.dp)
                .height(22.dp),
            style = TextStyle(
                fontSize = 14.4.sp,
                lineHeight = 21.6.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
                fontWeight = FontWeight(500),
                color = Color(0xFF1D1E23),
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = currentValue,
            onCheckedChange = {
                switchOption.onItemSelected(title, it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = mainInverse,
                checkedTrackColor = mainPrimary,
                uncheckedThumbColor = mainInverse,
                uncheckedTrackColor = mainDisabled,
                uncheckedBorderColor = Color.Transparent
            ),
            thumbContent = {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = Variables.surfaceDefault,
                            shape = CircleShape
                        )
                )
            },
            modifier = Modifier
                .width(43.2.dp)
                .height(21.6.dp)
        )
    }
}

data class TextInputData(
    val titleId: Int,
    val currentValue: String,
    val placeholder: String = "",
    val onItemSelected: (title: String, newValue: String) -> Unit
)

@Composable
fun TextInputOption(textInputOption: TextInputData, enabled: Boolean = true) {
    var text by remember { mutableStateOf(textInputOption.currentValue) }
    var title = stringResource(textInputOption.titleId)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            enabled = enabled,
            onValueChange = {
                text = it
                textInputOption.onItemSelected(title, text)
            },
            label = { Text(title) },
            placeholder = { Text(textInputOption.placeholder) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(size = 3.6.dp))
                .fillMaxWidth()
                .padding(start = 14.4.dp, top = 8.dp, end = 14.4.dp, bottom = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Variables.surfaceDefault,
                unfocusedContainerColor = Variables.surfaceDefault,
                cursorColor = Variables.mainPrimary,
                focusedIndicatorColor = Variables.mainPrimary,
                unfocusedIndicatorColor = Variables.mainPrimary,
                unfocusedLabelColor = Variables.mainSubtle,
                focusedLabelColor = Variables.mainSubtle,
                selectionColors = TextSelectionColors(
                    handleColor = mainPrimary,
                    backgroundColor = mainPrimary
                ),
                disabledContainerColor = Variables.surfaceDefault,
                disabledLabelColor = Variables.mainDisabled
            )
        )
    }
}

data class ButtonData(
    val titleId: Int,
    val color: Color,
    val alpha: Float = 1.0F,
    val enabled: Boolean,
    val onButtonClick: () -> Unit
)

@Composable
fun ButtonOption(buttonData: ButtonData) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            Variables.spacingSmall,
            Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(buttonData.alpha)
            .background(color = buttonData.color, shape = RoundedCornerShape(size = 4.dp))
            .padding(
                start = Variables.spacingLarge,
                top = Variables.spacingMedium,
                end = Variables.spacingLarge,
                bottom = Variables.spacingMedium
            )
            .clickable(enabled = buttonData.enabled) {
                buttonData.onButtonClick()
            }
    ) {
        Text(
            text = stringResource(buttonData.titleId),
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
                fontWeight = FontWeight(500),
                color = Variables.stateDefaultEnabled,
                textAlign = TextAlign.Center,
            )
        )
    }
    }
}

data class BorderlessButtonData(
    val titleId: Int,
    val onButtonClick: () -> Unit
)

@Composable
fun BorderlessButton(buttonData: BorderlessButtonData) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .background(
                color = Variables.stateDefaultEnabled,
                shape = RoundedCornerShape(size = 4.dp)
            )
            .padding(top = Variables.spacingSmall, bottom = Variables.spacingSmall)
            .clickable {
                buttonData.onButtonClick()
            }
    ) {
        Text(
            text = stringResource(buttonData.titleId),
            softWrap = true,
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                fontWeight = FontWeight(500),
                color = Variables.borderPrimaryMain,
            )
        )
    }
}

@Composable
fun TextviewNormal(info: String) {
    Text(
        text = info,
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        style = TextStyle(
            fontSize = 14.4.sp,
            lineHeight = 21.6.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
            fontWeight = FontWeight(500),
            color = Color(0xFF1D1E23),
        )
    )
}

@Composable
fun TextviewBold(info: String) {
    Text(
        text = info,
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        style = TextStyle(
            fontSize = 14.4.sp,
            lineHeight = 21.6.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
            fontWeight = FontWeight(500),
            color = Color(0xFF1D1E23),
        )
    )
}
