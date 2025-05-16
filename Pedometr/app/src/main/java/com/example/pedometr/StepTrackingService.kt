package com.example.pedometr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.pedometr.data.StepRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class StepTrackingService: Service(), SensorEventListener {
    @Inject
    lateinit var stepRepository: StepRepository
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var initialStepCount: Int? = null
    private var lastStepCount: Int = 0
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "StepTrackingChannel"
        const val ACTION_STOP_SERVICE = "com.example.pedometr.ACTION_STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            intent.action = ACTION_STOP_SERVICE
            context.startService(intent)
        }
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        serviceScope.cancel()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks steps in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StepTrackingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pedometer")
            .setContentText("Tracking steps in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor != null) {
            sensorManager?.registerListener(
                this,
                stepCounterSensor,
                0,
                100000
            )
            serviceScope.launch {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val todaySteps = stepRepository.getStepsForDate(today).first()
                lastStepCount = todaySteps?.steps ?: 0
            }
        } else {
            stopSelf()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialStepCount == null) {
                initialStepCount = totalSteps
            }
            val stepsSinceStart = totalSteps - (initialStepCount ?: totalSteps) + lastStepCount
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            serviceScope.launch {
                stepRepository.saveSteps(stepsSinceStart)
                lastStepCount = stepsSinceStart
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

}