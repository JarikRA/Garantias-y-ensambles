package com.intesc.controldegarantiasyensambles.main

import android.app.Application
import timber.log.Timber

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        //Enable if you want to debug
        val isDebug = false

        if (isDebug) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber initialized in MyApplication!")
        }
    }
}
