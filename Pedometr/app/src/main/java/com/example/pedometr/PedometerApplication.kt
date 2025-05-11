package com.example.pedometr

import android.app.Application
import android.content.Intent
import android.util.Log

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.pedometr.Worker.DailyActivitySyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
@HiltAndroidApp
class PedometerApplication : Application(), Configuration.Provider  {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override fun onCreate() {
        super.onCreate()
        Log.d("PedometerApplication", "onCreate called")
        try {
            if (::workerFactory.isInitialized) {
                Log.d("PedometerApplication", "HiltWorkerFactory injected successfully")
            } else {
                Log.e("PedometerApplication", "HiltWorkerFactory not initialized")
            }
            StepTrackingService.startService(this)
            DailyActivitySyncWorker.schedule(this)
            WorkManager.getInstance(this)
            Log.d("PedometerApplication", "WorkManager accessed with HiltWorkerFactory")
        } catch (e: Exception) {
            Log.e("PedometerApplication", "Error in onCreate: ${e.message}", e)
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        Log.d("PedometerApplication", "Providing WorkManager configuration")
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}