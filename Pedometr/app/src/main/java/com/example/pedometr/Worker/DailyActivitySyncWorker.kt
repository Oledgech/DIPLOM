package com.example.pedometr.Worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker


import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.pedometr.data.StepRepository
import com.example.pedometr.mariaDb.ApiClient
import com.example.pedometr.mariaDb.UserActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class DailyActivitySyncWorker @AssistedInject constructor(@Assisted  context: Context, @Assisted params: WorkerParameters, private val stepRepository: StepRepository ) : CoroutineWorker(context, params)
{
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val stepEntity = stepRepository.getStepsForDate(today).first()
            if (stepEntity != null) {
                val sharedPreferences = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                val height = sharedPreferences.getInt("height", 190)
                val gender = sharedPreferences.getInt("gender", 0)
                val stepLength = if (gender == 0) height * 0.415 / 100 else height * 0.413 / 100
                val distanceMeters = stepEntity.steps * stepLength
                val distanceKm = distanceMeters / 1000
                val activeTimeMinutes = 1000
                val userActivity = UserActivity(
                    user_group = sharedPreferences.getString("user_group", "default_group") ?: "default_group",
                    activity_date = today,
                    steps = 1000,
                    distance_km = 13.0,
                    active_time_minutes = activeTimeMinutes
                )
                ApiClient.activityApi.sendActivity(userActivity)
                Result.success()
            } else {
                val userActivity = UserActivity(
                    user_group = "default_group",
                    activity_date = today,
                    steps = 1000,
                    distance_km = 13.0,
                    active_time_minutes = 1000
                )
                Log.d("DailyActivitySyncWorker", "Sending activity: $userActivity")
                ApiClient.activityApi.sendActivity(userActivity)
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyActivitySync"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            val targetTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(21, 25))
            Log.d("DailyActivitySyncWorker", "Sending activity: $targetTime")
            val initialDelay = if (now.isBefore(targetTime)) {
                java.time.Duration.between(now, targetTime).toMinutes()
            } else {
                java.time.Duration.between(now, targetTime.plusDays(1)).toMinutes()
            }
            val testDelay = 1L
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<DailyActivitySyncWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, androidx.work.ExistingPeriodicWorkPolicy.KEEP, workRequest)
//            val constraints = Constraints.Builder()
//                .setRequiredNetworkType(NetworkType.CONNECTED)
//                .build()
//
//
//            val workRequest = OneTimeWorkRequestBuilder<DailyActivitySyncWorker>()
//                .setConstraints(constraints)
//                .build()
//
//            WorkManager.getInstance(context)
//                .enqueueUniqueWork(WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, workRequest)

        }
    }
}