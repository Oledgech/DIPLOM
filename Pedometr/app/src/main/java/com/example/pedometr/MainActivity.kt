package com.example.pedometr

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.mikhaellopez.circularprogressbar.CircularProgressBar

class MainActivity : AppCompatActivity(), SensorEventListener{

    private var sensorManager: SensorManager?= null
    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        loadData()
        resetSteps()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermission() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Разрешение на шаги не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }

        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, "Нет сенсора", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (running) {
            if (event != null) {
                totalSteps=event.values[0]
            }
            val currenSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            val circularProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
            val stepTextView = findViewById<TextView>(R.id.stepCountTextView1)
            stepTextView.text=("$currenSteps")
            circularProgressBar.apply { setProgressWithAnimation(currenSteps.toFloat()) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun resetSteps(){
        val stepTextView = findViewById<TextView>(R.id.stepCountTextView1)
        stepTextView.setOnClickListener{
            Toast.makeText(this, "Сброс", Toast.LENGTH_SHORT).show()
        }
        stepTextView.setOnLongClickListener{
            previousTotalSteps = totalSteps
            stepTextView.text=0.toString()
            saveData()
            true
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("key1",previousTotalSteps)
        editor.apply()
    }
    private fun loadData()
    {
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("key1",previousTotalSteps)
        Log.d("Main","$savedNumber")
        previousTotalSteps = savedNumber
    }
}