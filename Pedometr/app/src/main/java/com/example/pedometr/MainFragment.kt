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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

import com.example.pedometr.data.AppDatabase
import com.example.pedometr.data.StepRepository
import com.example.pedometr.data.StepViewModel
import com.example.pedometr.data.StepViewModelFactory
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.platform.android.ConscryptSocketAdapter.Companion.factory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainFragment : Fragment(), SensorEventListener{

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var isCounterSensorPresent = false
    private var initialStepCount: Int? = null
    private var running = false
    private var totalSteps = 100f
    private var previousTotalSteps = 0f
    private var currentSteps = 0
    private var lastDate: String? = "2025-04-18"
    private var currentDate: String? = null
    private lateinit var totalStepsTextView: TextView
    private lateinit var totalDistanceTextView: TextView
    private lateinit var totalCaloriesTextView: TextView
    private lateinit var stepTextView: TextView
    private lateinit var circularProgressBar: CircularProgressBar
    private lateinit var buildRouteButton: ImageButton
    private var executorService: ScheduledExecutorService? = null
    private var simulateSteps = true
    private var totalActiveTimeSeconds = 0L // Суммарное время активности в секундах
    private var lastUpdateTime = 1L // Время последнего обновления шагов
    private var lastStepCount = 0
    private lateinit var viewModel: StepViewModel
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initializeSensor()
        } else {
            Toast.makeText(requireContext(), "Permission denied. Cannot count steps.", Toast.LENGTH_LONG).show()
            stepTextView.text = "Permission Denied"
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        //Room
        val database = AppDatabase.getDatabase(requireContext())
        val repository = StepRepository(database.stepsDao())
        val factory = StepViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StepViewModel::class.java]
        //UI
        totalStepsTextView = view.findViewById(R.id.distanceTextView)
        totalDistanceTextView = view.findViewById(R.id.activeTimeTextView)
        totalCaloriesTextView = view.findViewById(R.id.caloriesTextView)
        stepTextView = view.findViewById(R.id.stepCountTextView1)
        circularProgressBar = view.findViewById(R.id.circularProgressBar)
        buildRouteButton = view.findViewById(R.id.buildRouteButton)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        buildRouteButton.setOnClickListener {
            val mapMainFragment = MapMainFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mapMainFragment)
                .addToBackStack(null)
                .commit()
        }
////        loadData()
//        resetSteps()
//        executorService = Executors.newSingleThreadScheduledExecutor()

        //Сегодняшние шаги
        viewModel.todaySteps.observe(viewLifecycleOwner) { stepEntity ->
            val steps = stepEntity?.steps ?: 0
            Log.d("MainActivitysafsa", "Saved steps for  $steps")
            updateUI(steps)
        }

        requestActivityRecognitionPermission()

        return view
    }
    //Разрешения
    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                initializeSensor()
            }
        } else {
            initializeSensor()
        }
    }
    //Сенсор
    private fun initializeSensor() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor != null) {
            isCounterSensorPresent = true
        } else {
            stepTextView.text = "Step Counter Sensor not available!"
            isCounterSensorPresent = false
            Toast.makeText(requireContext(), "No step sensor available", Toast.LENGTH_LONG).show()
        }
    }
    override fun onResume() {
        super.onResume()
//        running = true
//        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
//        if (stepSensor == null) {
//            loadData()
//            Toast.makeText(requireContext(), "Нет сенсора", Toast.LENGTH_SHORT).show()
//            simulateSteps = false // Имитация шагов, если датчик недоступен
//            startSimulatedSteps()
//        } else {
//            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
//        }
//        startStepTrackingService()
        if (isCounterSensorPresent) {
            sensorManager?.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }
//    private fun startSimulatedSteps() {
//        executorService?.scheduleWithFixedDelay({
//            if (simulateSteps && running) {
//                onSensorChanged(null)
//            }
//        }, 0, 1, TimeUnit.SECONDS)
//        Log.d("MainActivitysafsa", "Simulated steps timer started")
//    }
    override fun onPause() {
//        super.onPause()
//        running = false
//        sensorManager?.unregisterListener(this)
//        saveData()
    super.onPause()
    if (isCounterSensorPresent) {
        sensorManager?.unregisterListener(this)
    }

    }

    override fun onSensorChanged(event: SensorEvent?) {

//        if (running) {
//            if (simulateSteps) {
//                totalSteps += 100f
//                Log.d("MainActivityqwrwq", "Simulated steps: $totalSteps")
//            } else if (event != null) {
//                totalSteps = event.values[0]
//            }
//
//
//            currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
//            val currentTime = System.currentTimeMillis()
//            saveData(currentTime.toInt())
////            currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
////            Log.d("MainFragment", "currentDate: $currentDate")
////            Log.d("MainFragment", "lastDate: $lastDate")
////            if (lastDate != currentDate) {
////                if (lastDate != null) {
////                    Log.d("MainFragment", "lastDatecheck: $lastDate")
////                    saveDailySteps(lastDate!!, currentSteps)
////                }
////                previousTotalSteps = totalSteps
////                currentSteps = 0
////                saveData()
////                lastDate = currentDate
////                stepTextView.text = "$currentSteps"
////                circularProgressBar.setProgressWithAnimation(currentSteps.toFloat())
////            } else {
////                currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
////                activity?.runOnUiThread{
////                    stepTextView.text = "$currentSteps"
////                    circularProgressBar.setProgressWithAnimation(currentSteps.toFloat())
////                }
////
////            }
//            if (lastUpdateTime != 0L && currentSteps > lastStepCount) {
//                // Если шаги увеличились, добавляем время между обновлениями к общей сумме
//                val timeDeltaMillis = currentTime - lastUpdateTime
//                val timeDeltaSeconds = timeDeltaMillis / 1000
//                if (simulateSteps) {
//                    totalActiveTimeSeconds += 1
//                } else {
//                    totalActiveTimeSeconds += timeDeltaSeconds
//                }
//            }
//            lastUpdateTime = currentTime
//            lastStepCount = currentSteps
//            viewModel.saveSteps(currentSteps+1)
//            activity?.runOnUiThread {
//                updateUI(currentSteps)
//            }
////            currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
////            val calories = (currentSteps * 0.04).toInt()
////            val distanceKm = (currentSteps * 0.75 / 1000).toInt()
////            activity?.runOnUiThread{
////                stepTextView.text = "$currentSteps"
////                circularProgressBar.setProgressWithAnimation(currentSteps.toFloat())
////                totalCaloriesTextView.text = "$calories"
////                totalStepsTextView.text = "$distanceKm km"
////
////            }
//        }
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialStepCount == null) {
                initialStepCount = totalSteps
            }
            val stepsSinceStart = totalSteps - (initialStepCount ?: totalSteps)
            viewModel.saveSteps(stepsSinceStart+1)
            val currentTime = System.currentTimeMillis()
            if (lastUpdateTime != 0L && stepsSinceStart > lastStepCount) {
                val timeDeltaMillis = currentTime - lastUpdateTime
                totalActiveTimeSeconds += timeDeltaMillis / 1000
            }
            lastUpdateTime = currentTime
            lastStepCount = stepsSinceStart
        }
    }
    private fun updateUI(steps: Int) {
//        stepTextView.text = "Шаги: $steps"
//        circularProgressBar.setProgressWithAnimation(steps.toFloat())
//        val calories = (currentSteps * 0.04).toInt()
//        totalCaloriesTextView.text = "$calories"
//        val activeTimeMinutes = (totalActiveTimeSeconds / 60).toInt()
//        Log.d("MainFragment", "lastDatecheck: $activeTimeMinutes")
//        totalDistanceTextView.text = "${activeTimeMinutes}m"
//
//        // Рассчитываем и обновляем расстояние
//        val distanceKm = (currentSteps * 0.75 / 1000).toInt() // 0.75 метра на шаг, переводим в км
//        totalStepsTextView.text = "$distanceKm km"
        stepTextView.text = "Шаги: $steps"
        circularProgressBar.setProgressWithAnimation(steps.toFloat())
        val calories = (steps * 0.04).toInt()
        totalCaloriesTextView.text = "$calories"
        val activeTimeMinutes = (totalActiveTimeSeconds / 60).toInt()
        totalDistanceTextView.text = "${activeTimeMinutes}m"
        val distanceKm = (steps * 0.75 / 1000).toInt()
        totalStepsTextView.text = "$distanceKm km"
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//    private fun startStepTrackingService() {
//        StepTrackingService.startService(requireContext())
//    }

//    private fun resetSteps() {
//        stepTextView.setOnClickListener {
//            Toast.makeText(requireContext(), "Сброс", Toast.LENGTH_SHORT).show()
//            simulateSteps=false
//            currentDate = "2025-04-18"
////            saveDailySteps(currentDate!!, totalSteps.toInt())
////            viewModel.saveSteps(totalSteps.toInt()+100)
////            saveDailySteps1(currentDate!!, totalSteps.toInt())
//
//        }
//        stepTextView.setOnLongClickListener {
//            previousTotalSteps = totalSteps
////            stepTextView.text = "0"
////            saveData()
//            true
//        }
//    }
//
//    private fun saveData(steps: Int) {
//        val sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        Log.d("Mainew", "Loaded time: $steps")
//        editor.putInt("key2", steps)
//        editor.apply()
//    }
//
//    private fun saveDailySteps(date: String, steps: Int) {
//        val sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        editor.putInt("steps_$date", steps)
//        editor.putString("lastDate", date)
//        editor.apply()
//    }
////    private fun saveDailySteps1(date: String, steps: Int) {
////        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
////            val stepEntry = StepEntry(date, steps)
////            db.stepsDao().insert(stepEntry)
////            Log.d("MainFragment", "Saved steps for $date: $steps")
////        }
////    }
//private fun loadDataTime() {
//    val sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
//    lastUpdateTime = sharedPreferences.getLong("key2", 2L)
//    Log.d("Mainew", "Loadeasd: $lastDate")
//}
//    private fun loadData() {
//        val sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val calendar = Calendar.getInstance()
//        val date = dateFormat.format(calendar.time)
//        val savedNumber = sharedPreferences.getInt("steps_$date", 0)
//        Log.d("MainFragment", "$savedNumber")
//        previousTotalSteps = savedNumber.toFloat()
//        lastDate = sharedPreferences.getString("lastDate", currentDate)
//        Log.d("MainFragment", "Loaded lastDate: $lastDate")
//    }
}