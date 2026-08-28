package com.zebra.ai.palletchecker.helpers

import android.util.Log
import com.zebra.ai.palletchecker.BuildConfig

private inline fun shouldLog(): Boolean = BuildConfig.DEBUG
private val enableLogForDevelopment:Boolean = false

fun LOGD(tag:String,msg:String){
    if (shouldLog()) LOGI(tag,msg)
}
fun LOGE(tag:String,msg:String,tr : Throwable? = null){
    if (shouldLog()) tr?.let {  Log.e(tag,msg ,tr) } ?: Log.e(tag,msg )
}
fun LOGW(tag:String,msg:String){
    if (shouldLog()) Log.w(tag,msg)
}
fun LOGI(tag:String,msg:String){
    if (enableLogForDevelopment) Log.i(tag,msg)
}

fun LOGV(tag:String,msg:String){
    if (shouldLog()) Log.i(tag,msg)
}