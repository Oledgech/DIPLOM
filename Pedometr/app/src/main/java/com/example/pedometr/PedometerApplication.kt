package com.example.pedometr

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PedometerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        StepTrackingService.startService(this)
    }
}