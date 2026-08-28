package com.zebra.ai.palletchecker.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.ai.palletchecker.domain.Configuration
import com.zebra.ai.palletchecker.domain.model.AppConfig
import com.zebra.ai.palletchecker.domain.model.BarcodeConfig
import com.zebra.ai.palletchecker.domain.model.FIELD_TYPE
import com.zebra.ai.palletchecker.presentation.model.ConfigUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConfigureViewModel(val app: Application) : AndroidViewModel(application = app) {
    private val configUsecase = Configuration(app)

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    val config: StateFlow<AppConfig> = configUsecase.getConfig()

    private fun observeAppConfig() {
        viewModelScope.launch {
            configUsecase.getConfig().collectLatest {
                _uiState.value = _uiState.value.copy(listConfig = it.listOfConfig)
            }
        }
    }

    init {
        observeAppConfig()
    }

    fun updateExpandStatus(config: BarcodeConfig) {
        if(_uiState.value.expandedList.contains(config.type)){
            val list = _uiState.value.expandedList.filterNot { it == config.type }
            _uiState.value = _uiState.value.copy(expandedList = list)
        } else {
            val list = _uiState.value.expandedList.toMutableList()
            list.add(config.type)
            _uiState.value = _uiState.value.copy(expandedList = list)
        }

    }

    /**
     * Updates settings using the provided update function and syncs with repository.
     * @param update Function to update AppSettings
     */
    private fun updateSettingsWith(
        update: (AppConfig) -> AppConfig,
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        configUsecase.updateSettings(update)

        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Updates the model input size and syncs with the AI Vision SDK.
     */
    fun updateFields(config: BarcodeConfig, type: FIELD_TYPE) {

        updateSettingsWith({
            val list = it.listOfConfig.mapNotNull {  if(it.type ==type.name ) config else it  }
            it.copy(listOfConfig =  list)
        })
    }

    fun updateProductQty(qty: Int) {
        updateSettingsWith({
            it.copy(expectedBoxes = qty)
        })
    }
}