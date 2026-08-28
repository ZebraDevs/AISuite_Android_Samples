package com.zebra.ai.palletchecker.presentation.ui.compose.components

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner

data class ViewModelOwner(val viewModelStoreOwner: ViewModelStoreOwner?=null)
val LocalModelStoreOwner = compositionLocalOf { ViewModelOwner() }