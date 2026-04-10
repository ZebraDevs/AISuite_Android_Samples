package com.zebra.aidatacapturedemo

import android.app.Application
import sdk.pendo.io.Pendo

object PendoInitializer {
    fun init(app: Application, apiKey: String, enabled: Boolean = true) {
        if (!enabled) return
        Pendo.setup(app, apiKey, null, null)
        Pendo.startSession(null, null, null, null)
    }
}

