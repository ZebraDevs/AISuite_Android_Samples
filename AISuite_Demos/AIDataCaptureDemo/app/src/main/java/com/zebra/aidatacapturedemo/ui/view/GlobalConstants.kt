package com.zebra.aidatacapturedemo.ui.view

import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.Variables.mainIcon1
import com.zebra.aidatacapturedemo.ui.view.Variables.mainIcon2
import com.zebra.aidatacapturedemo.ui.view.Variables.secondaryIcon1
import com.zebra.aidatacapturedemo.ui.view.Variables.secondaryIcon2

object Variables {
    val surfaceDefault: Color = Color(0xFFFFFFFF)
    val mainDefault: Color = Color(0xFF1D1E23)
    val mainPrimary: Color = Color(0xFF0073E6)
    val mainSubtle: Color = Color(0xFF545963)
    val mainIcon1: Color = Color(0xFF7E0CFF)
    val secondaryIcon1: Color = Color(0xFFE600E6)
    val mainIcon2: Color = Color(0xFF7E0CFF)
    val secondaryIcon2: Color = Color(0xFF3F40F3)
    val borderDefault: Color = Color(0xFFCED2DB)
    val mainInverse: Color = Color(0xFFF3F6FA)
    val warningColor: Color = Color(0xCCFFCB00)
    val warningBorder: Color = Color(0xFFDDB000)
    val mainDisabled: Color = Color(0xFF8D95A3)
    val stateDefaultEnabled: Color = Color(0xFFFFFFFF)
    val borderPrimaryMain: Color = Color(0xFF0073E6)
    val uncheckedThumbColor: Color = Color(0xFFAFB6C2)
    val uncheckedTrackColor: Color = Color(0xFFE8EBF1)

    val blackText: Color = Color(0xFF000000)
    val spacingSmall: Dp = 8.dp
    val spacingMinimum: Dp = 4.dp

    val spacingLarge: Dp = 16.dp
    val spacingMedium: Dp = 12.dp

    val surfaceTertiary: Color = Color(0xFF151519)
    val surfaceTertiarySelected: Color = Color(0xFF3C414B)
    val mainLight: Color = Color(0xFFE0E3E9)
    val inverseDefault: Color = Color(0xFFFFFFFF)
    val textSubtle: Color = Color(0xFF646A78)
}

fun getIconMainColor(demo: String): Color {
    var mainColor: Color = mainIcon2
    when (demo) {
        UsecaseState.Barcode.value -> {
            mainColor = mainIcon2
        }

        UsecaseState.OCR.value -> {
            mainColor = mainIcon2
        }

        UsecaseState.Retail.value -> {
            mainColor = mainIcon2
        }

        UsecaseState.OCRFind.value -> {
            mainColor = mainIcon1
        }

        UsecaseState.Product.value -> {
            mainColor = mainIcon1
        }
    }
    return mainColor
}

fun getIconSecondaryColor(demo: String): Color {
    var secondaryColor: Color = secondaryIcon2
    when (demo) {
        UsecaseState.Barcode.value -> {
            secondaryColor = secondaryIcon2
        }

        UsecaseState.OCR.value -> {
            secondaryColor = secondaryIcon2
        }

        UsecaseState.Retail.value -> {
            secondaryColor = secondaryIcon2
        }

        UsecaseState.OCRFind.value -> {
            secondaryColor = secondaryIcon1
        }

        UsecaseState.Product.value -> {
            secondaryColor = secondaryIcon1
        }
    }
    return secondaryColor
}

fun getIconId(demo: String): Int? {
    var iconId: Int? = null
    when (demo) {
        UsecaseState.Barcode.value -> {
            iconId = R.drawable.barcode_icon
        }

        UsecaseState.OCR.value -> {
            iconId = R.drawable.ocr_icon
        }

        UsecaseState.Retail.value -> {
            iconId = R.drawable.retail_shelf_icon
        }

        UsecaseState.OCRFind.value -> {
            iconId = R.drawable.ocr_finder_icon
        }

        UsecaseState.Product.value -> {
            iconId = R.drawable.product_enrollment_recognition_icon
        }
    }
    return iconId
}

fun getSettingHeading(demo: String): Int? {
    var settingsString : Int? = null
    when (demo) {
        UsecaseState.Barcode.value -> {
            settingsString = R.string.barcode_settings
        }

        UsecaseState.OCR.value -> {
            settingsString = R.string.text_ocr_recognizer_settings
        }

        UsecaseState.Retail.value -> {
            settingsString = R.string.retailshelf_settings
        }

        UsecaseState.OCRFind.value -> {
            settingsString = R.string.ocr_find_settings
        }

        UsecaseState.Product.value -> {
            settingsString = R.string.productrecognition_settings
        }
    }
    return settingsString
}

fun getDemoTitle(demo: String): Int? {
    var settingsString : Int? = null
    when (demo) {
        UsecaseState.Barcode.value -> {
            settingsString = R.string.barcode_demo
        }

        UsecaseState.OCR.value -> {
            settingsString = R.string.ocr_demo
        }

        UsecaseState.Retail.value -> {
            settingsString = R.string.retail_shelf_demo
        }

        UsecaseState.OCRFind.value -> {
            settingsString = R.string.ocr_find_demo
        }

        UsecaseState.Product.value -> {
            settingsString = R.string.product_enrollment_recognition_demo
        }

        UsecaseState.Main.value -> {
            settingsString = R.string.app_name
        }
    }
    return settingsString
}

fun getSettingDescription(demo: String, setting: Int, value: Int): Int? {
    var descString : Int? = null
    when (setting) {
        R.string.runtime_processor -> {
            when (value) {
                0 -> {
                    descString = R.string.runtime_processor_auto_desc
                }

                1 -> {
                    descString = R.string.runtime_processor_dsp_desc
                }

                2 -> {
                    descString = R.string.runtime_processor_gpu_desc
                }
                3 -> {
                    descString = R.string.runtime_processor_cpu_desc
                }
            }
        }

        R.string.resolution -> {
            when (demo) {
                UsecaseState.Barcode.value -> {
                    when (value) {
                        0 -> {
                            descString = R.string.resolution_1mp_desc_bc
                        }

                        1 -> {
                            descString = R.string.resolution_2mp_desc_bc
                        }

                        2 -> {
                            descString = R.string.resolution_4mp_desc_bc
                        }

                        3 -> {
                            descString = R.string.resolution_8mp_desc_bc
                        }
                    }
                }

                UsecaseState.OCR.value,
                UsecaseState.OCRFind.value -> {
                    when (value) {
                        0 -> {
                            descString = R.string.resolution_1mp_desc_ocr
                        }

                        1 -> {
                            descString = R.string.resolution_2mp_desc_ocr
                        }

                        2 -> {
                            descString = R.string.resolution_4mp_desc_ocr
                        }

                        3 -> {
                            descString = R.string.resolution_8mp_desc_ocr
                        }
                    }
                }

                UsecaseState.Retail.value,
                UsecaseState.Product.value -> {
                    descString = R.string.retailshelf_settings
                }
            }
        }

        R.string.model_input_size -> {
            when (demo) {
                UsecaseState.Barcode.value -> {
                    when (value) {
                        0 -> {
                            descString = R.string.model_input_size_640_bc
                        }

                        1 -> {
                            descString = R.string.model_input_size_1280_bc
                        }

                        2 -> {
                            descString = R.string.model_input_size_1600_bc
                        }

                        3 -> {
                            descString = R.string.model_input_size_2560_bc
                        }
                    }
                }

                UsecaseState.OCR.value,
                UsecaseState.OCRFind.value -> {
                    when (value) {
                        0 -> {
                            descString = R.string.model_input_size_640_ocr
                        }

                        1 -> {
                            descString = R.string.model_input_size_1280_ocr
                        }

                        2 -> {
                            descString = R.string.model_input_size_1600_ocr
                        }

                        3 -> {
                            descString = R.string.model_input_size_2560_ocr
                        }
                    }
                }

                UsecaseState.Retail.value,
                UsecaseState.Product.value -> {
                    descString = R.string.retailshelf_settings
                }
            }
        }
    }
    return descString
}
