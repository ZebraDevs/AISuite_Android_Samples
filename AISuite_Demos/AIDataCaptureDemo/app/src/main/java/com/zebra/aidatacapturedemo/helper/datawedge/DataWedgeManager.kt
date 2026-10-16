package com.zebra.aidatacapturedemo.helper.datawedge

import android.content.Context
import android.content.Intent

class DataWedgeManager(private val context: Context) {


    fun startSoftScan() {
        sendDwCommand(
            DataWedgeConstants.DW_EXTRA_SOFT_SCAN_TRIGGER, DataWedgeConstants.TRIGGER_START
        )
    }

    fun stopSoftScan() {
        sendDwCommand(
            DataWedgeConstants.DW_EXTRA_SOFT_SCAN_TRIGGER, DataWedgeConstants.TRIGGER_STOP
        )
    }

    fun toggleSoftScan() {
        sendDwCommand(
            DataWedgeConstants.DW_EXTRA_SOFT_SCAN_TRIGGER, DataWedgeConstants.TRIGGER_TOGGLE
        )
    }


    private fun sendDwCommand(extraKey: String, extraValue: String) {
        val i = Intent(DataWedgeConstants.DW_API_ACTION)
        i.putExtra(extraKey, extraValue)
        context.sendBroadcast(i)
    }
}