package com.zebra.ai.palletchecker.presentation.ui.compose.screens.configurescreen


import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.palletchecker.R
import com.zebra.ai.palletchecker.domain.model.FIELD_TYPE
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ZebraText
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.configurescreen.components.ConfigurationCard
import com.zebra.ai.palletchecker.presentation.ui.theme.AppColors
import com.zebra.ai.palletchecker.presentation.ui.theme.AppDimensions
import com.zebra.ai.palletchecker.presentation.ui.theme.AppTextStyles
import com.zebra.ai.palletchecker.presentation.ui.theme.configBackgroundColor
import com.zebra.ai.palletchecker.presentation.ui.theme.darkBackground
import com.zebra.ai.palletchecker.presentation.ui.theme.textBlack
import com.zebra.ai.palletchecker.presentation.ui.theme.white
import com.zebra.ai.palletchecker.presentation.viewmodel.ConfigureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Configuration(
    configViewModel: ConfigureViewModel = viewModel(),
    onBackPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val configState = configViewModel.uiState.collectAsState()
    val appConfig = configViewModel.config.collectAsState()
    var noOfProductText by remember { mutableStateOf(appConfig.value.expectedBoxes.toString()) }

    BackHandler(enabled = true) {
        onBackPress()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(configBackgroundColor)
            .semantics { contentDescription = "ConfigureDemo" }) {
        TopAppBar(windowInsets = WindowInsets(0.dp),
            title = {
                ZebraText(
                    textValue = "Configure Demo",
                    style = AppTextStyles.TitleTextLight,
                    textColor = AppColors.TextWhite
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.setting_screen_back_icon_description),
                        tint = white
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = darkBackground
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(14.dp))
                ZebraText(
                    textValue = "Configuration",
                    fontSize = 22.sp,
                    lineHeight = AppDimensions.linePaddingDefault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                )

                ZebraText(
                    textValue = "Select or Modify form fields",
                    fontSize = 14.sp,
                    lineHeight = AppDimensions.linePaddingDefault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))

            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.dimension_16dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Number of boxes to audit",
                        fontSize = AppDimensions.dialogTextFontSizeSmall,
                        color = textBlack,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.dimension_8dp))
                    OutlinedTextField(
                        value =noOfProductText,
                        onValueChange = { newValue ->
                            if(newValue.isNotEmpty()) {
                                newValue.toIntOrNull()?.let { configViewModel.updateProductQty(it) }
                            } else {
                                newValue.toIntOrNull()?.let { configViewModel.updateProductQty(5) }
                                Toast.makeText(context, "No of Boxes should not be empty", Toast.LENGTH_SHORT).show()
                            }
                            noOfProductText = newValue
                        },
                        singleLine = true,
                        modifier = Modifier.width(80.dp),

                        )
                }
            }

            items(configState.value.listConfig) { item ->
                val expanded = configState.value.expandedList.contains(item.type)
                val type = FIELD_TYPE.values().filter { it.name == item.type }
                val isProductSku = item.type == FIELD_TYPE.PRODUCT_SKU.name
                if(type.isNotEmpty()) {
                    ConfigurationCard(
                        title = FIELD_TYPE.valueOf(item.type).title,
                        isExpanded = expanded,
                        selected = item.isSelected,
                        onSelectionChange = { select ->
                            configViewModel.updateFields(
                                item.copy(isSelected = select),
                                FIELD_TYPE.valueOf(item.type)
                            )
                        },
                        onExpandToggle = {
                            configViewModel.updateExpandStatus(item.copy(isExpanded = !expanded))
                        }) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {

                            if (isProductSku) {
                                Text(
                                    text = "Note: This field\u2019s barcode data needs to be unique on boxes across pallet. When enabled, providing the \u201cStart With\u201d criteria below is mandatory.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppDimensions.dimension_16dp, vertical = 4.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription =
                                            "ConfigOption: Product SKU Id"
                                    }
                                    .padding(
                                        horizontal = AppDimensions.dimension_2dp,
                                        vertical = AppDimensions.zeroPadding
                                    ),
                                verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppDimensions.dimension_16dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Start With",
                                        fontSize = AppDimensions.dialogTextFontSizeSmall,
                                        color = textBlack,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(AppDimensions.dimension_8dp))
                                    OutlinedTextField(
                                        value = item.regex,
                                        onValueChange = { newValue ->
                                            configViewModel.updateFields(
                                                item.copy(regex = newValue),
                                                FIELD_TYPE.valueOf(item.type)
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier.width(80.dp),

                                        )
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}