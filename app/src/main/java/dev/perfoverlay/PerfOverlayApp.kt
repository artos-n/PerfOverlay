package dev.perfoverlay

import android.app.Application

class PerfOverlayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PerfOverlayApp
            private set
    }
}
