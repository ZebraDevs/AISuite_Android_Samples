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
import com.zebra.aidatacapturedemo.data.BarcodeSymbology
import com.zebra.aidatacapturedemo.data.UsecaseState
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
        var currentSymbology = BarcodeSymbology()
        if(currentUIState.usecaseSelected == UsecaseState.OCRBarcodeFind.value){
            currentSymbology = currentUIState.ocrBarcodeFindSettings.barcodeSymbology
        } else {
            currentSymbology = currentUIState.barcodeSettings.barcodeSymbology
        }

        SwitchOption(currentSymbology.australian_postal, SwitchOptionData(R.string.australian_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.aztec, SwitchOptionData(R.string.aztec, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.canadian_postal, SwitchOptionData(R.string.canadian_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.chinese_2of5, SwitchOptionData(R.string.chinese_2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.codabar, SwitchOptionData(R.string.codabar, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.code11, SwitchOptionData(R.string.code11, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.code39, SwitchOptionData(R.string.code39, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.code93, SwitchOptionData(R.string.code93, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.code128, SwitchOptionData(R.string.code128, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.composite_ab, SwitchOptionData(R.string.composite_ab, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.composite_c, SwitchOptionData(R.string.composite_c, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.d2of5, SwitchOptionData(R.string.d2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.datamatrix, SwitchOptionData(R.string.datamatrix, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.dotcode, SwitchOptionData(R.string.dotcode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.dutch_postal, SwitchOptionData(R.string.dutch_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.ean_8, SwitchOptionData(R.string.ean_8, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.ean_13, SwitchOptionData(R.string.ean_13, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.finnish_postal_4s, SwitchOptionData(R.string.finnish_postal_4s, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.grid_matrix, SwitchOptionData(R.string.grid_matrix, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.gs1_databar, SwitchOptionData(R.string.gs1_databar, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.gs1_databar_expanded, SwitchOptionData(R.string.gs1_databar_expanded, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.gs1_databar_lim, SwitchOptionData(R.string.gs1_databar_lim, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.gs1_datamatrix, SwitchOptionData(R.string.gs1_datamatrix, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.gs1_qrcode, SwitchOptionData(R.string.gs1_qrcode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.hanxin, SwitchOptionData(R.string.hanxin, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.i2of5, SwitchOptionData(R.string.i2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.japanese_postal, SwitchOptionData(R.string.japanese_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.korean_3of5, SwitchOptionData(R.string.korean_3of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.mailmark, SwitchOptionData(R.string.mailmark, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.matrix_2of5, SwitchOptionData(R.string.matrix_2of5, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.maxicode, SwitchOptionData(R.string.maxicode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.micropdf, SwitchOptionData(R.string.micropdf, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.microqr, SwitchOptionData(R.string.microqr, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.msi, SwitchOptionData(R.string.msi, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.pdf417, SwitchOptionData(R.string.pdf417, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.qrcode, SwitchOptionData(R.string.qrcode, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.tlc39, SwitchOptionData(R.string.tlc39, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.trioptic39, SwitchOptionData(R.string.trioptic39, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.uk_postal, SwitchOptionData(R.string.uk_postal, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.upc_a, SwitchOptionData(R.string.upc_a, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.upce0, SwitchOptionData(R.string.upce0, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.upce1, SwitchOptionData(R.string.upce1, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.usplanet, SwitchOptionData(R.string.usplanet, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.uspostnet, SwitchOptionData(R.string.uspostnet, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.us4state, SwitchOptionData(R.string.us4state, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
        SwitchOption(currentSymbology.us4state_fics, SwitchOptionData(R.string.us4state_fics, onItemSelected = { title, enabled ->
            viewModel.updateBarcodeSymbology(title, enabled)
        }))
    }
}