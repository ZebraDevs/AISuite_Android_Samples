package com.zebra.aidatacapturedemo.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun ExpandableSettingsItemsList.AddBarcodeSettings()  {
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.barcode_symbology)))
}
@Composable
fun AddBarcodeSymbologySwitchOption(viewModel: AIDataCaptureDemoViewModel){
    val currentUIState = viewModel.uiState.collectAsState().value
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.australian_postal, SwitchOptionData(R.string.australian_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.aztec, SwitchOptionData(R.string.aztec, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.canadian_postal, SwitchOptionData(R.string.canadian_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.chinese_2of5, SwitchOptionData(R.string.chinese_2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.codabar, SwitchOptionData(R.string.codabar, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.code11, SwitchOptionData(R.string.code11, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.code39, SwitchOptionData(R.string.code39, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.code93, SwitchOptionData(R.string.code93, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.code128, SwitchOptionData(R.string.code128, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.composite_ab, SwitchOptionData(R.string.composite_ab, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.composite_c, SwitchOptionData(R.string.composite_c, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.d2of5, SwitchOptionData(R.string.d2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.datamatrix, SwitchOptionData(R.string.datamatrix, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.dotcode, SwitchOptionData(R.string.dotcode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.dutch_postal, SwitchOptionData(R.string.dutch_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.ean_8, SwitchOptionData(R.string.ean_8, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.ean_13, SwitchOptionData(R.string.ean_13, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.finnish_postal_4s, SwitchOptionData(R.string.finnish_postal_4s, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.grid_matrix, SwitchOptionData(R.string.grid_matrix, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.gs1_databar, SwitchOptionData(R.string.gs1_databar, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.gs1_databar_expanded, SwitchOptionData(R.string.gs1_databar_expanded, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.gs1_databar_lim, SwitchOptionData(R.string.gs1_databar_lim, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.gs1_datamatrix, SwitchOptionData(R.string.gs1_datamatrix, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.gs1_qrcode, SwitchOptionData(R.string.gs1_qrcode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.hanxin, SwitchOptionData(R.string.hanxin, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.i2of5, SwitchOptionData(R.string.i2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.japanese_postal, SwitchOptionData(R.string.japanese_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.korean_3of5, SwitchOptionData(R.string.korean_3of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.mailmark, SwitchOptionData(R.string.mailmark, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.matrix_2of5, SwitchOptionData(R.string.matrix_2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.maxicode, SwitchOptionData(R.string.maxicode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.micropdf, SwitchOptionData(R.string.micropdf, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.microqr, SwitchOptionData(R.string.microqr, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.msi, SwitchOptionData(R.string.msi, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.pdf417, SwitchOptionData(R.string.pdf417, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.qrcode, SwitchOptionData(R.string.qrcode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.tlc39, SwitchOptionData(R.string.tlc39, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.trioptic39, SwitchOptionData(R.string.trioptic39, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.uk_postal, SwitchOptionData(R.string.uk_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.upc_a, SwitchOptionData(R.string.upc_a, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.upce0, SwitchOptionData(R.string.upce0, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.upce1, SwitchOptionData(R.string.upce1, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.usplanet, SwitchOptionData(R.string.usplanet, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.uspostnet, SwitchOptionData(R.string.uspostnet, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.us4state, SwitchOptionData(R.string.us4state, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentUIState.barcodeSettings.barcodeSymbology.us4state_fics, SwitchOptionData(R.string.us4state_fics, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
    }
}