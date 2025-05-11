package com.example.pedometr

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UserFragment : Fragment() {
    private lateinit var weightTextView: TextView
    private lateinit var stepGoalTextView: TextView
    private lateinit var decrementStepsButton: Button
    private lateinit var incrementStepsButton: Button
    lateinit var heightTextView: TextView
    private lateinit var genderSpinner: Spinner
    private lateinit var birthDateTextView: TextView
    private lateinit var groupEditText: EditText
    private lateinit var studentNameTextView: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Шаги
        stepGoalTextView = view.findViewById(R.id.stepGoalTextView)
        decrementStepsButton = view.findViewById(R.id.decrementStepsButton)
        incrementStepsButton = view.findViewById(R.id.incrementStepsButton)

        // Загрузка сохраненного значения шагов
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedSteps = sharedPreferences.getInt("stepGoal", 5000)
        stepGoalTextView.text = savedSteps.toString()

        // Обработчики кнопок для изменения шагов
        decrementStepsButton.setOnClickListener {
            var steps = stepGoalTextView.text.toString().toInt()
            if (steps >= 1000) { // Минимальное значение шагов
                steps -= 500
                stepGoalTextView.text = steps.toString()
                sharedPreferences.edit().putInt("stepGoal", steps).apply()
            }
        }

        incrementStepsButton.setOnClickListener {
            var steps = stepGoalTextView.text.toString().toInt()
            if (steps <= 20000) {
                steps += 500
                stepGoalTextView.text = steps.toString()
                sharedPreferences.edit().putInt("stepGoal", steps).apply()
            }
        }

        // Вес
        weightTextView = view.findViewById(R.id.weightTextView)

        // Загрузка сохраненного веса
        val savedWeight = sharedPreferences.getFloat("weight", 95.5f)
        weightTextView.text = "$savedWeight кг"

        weightTextView.setOnClickListener {
            showWeightPickerDialog()
        }
        heightTextView = view.findViewById(R.id.heightTextView)

        // Загрузка сохраненного роста
        val savedHeight = sharedPreferences.getInt("height", 190) // Значение по умолчанию 190 см
        heightTextView.text = "$savedHeight см"

        // Открытие диалога при нажатии на TextView для роста
        heightTextView.setOnClickListener {
            showHeightPickerDialog()
        }

        genderSpinner = view.findViewById(R.id.genderSpinner)

        val savedGender = sharedPreferences.getInt("gender", 0)
        genderSpinner.setSelection(savedGender)

        genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPreferences.edit().putInt("gender", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        birthDateTextView = view.findViewById(R.id.birthDateTextView)

        // Загрузка сохраненной даты рождения
        val savedBirthDate = sharedPreferences.getString("birthDate", "01.01.1995")
        birthDateTextView.text = savedBirthDate

        // Открытие DatePickerDialog при нажатии на TextView
        birthDateTextView.setOnClickListener {
            showDatePickerDialog()
        }
        groupEditText = view.findViewById(R.id.groupEditText)

        // Загрузка сохраненной группы
        val savedGroup = sharedPreferences.getString("moodle_group", "")
        groupEditText.setText(savedGroup)

        // Сохранение группы при изменении текста
        groupEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newGroup = groupEditText.text.toString().trim()
                sharedPreferences.edit().putString("moodle_group", newGroup).apply()
            }
        }
        studentNameTextView = view.findViewById(R.id.studentNameTextView)

        // Загрузка сохраненного имени
        val savedFullName = sharedPreferences.getString("moodle_fullname", "Студент")
        studentNameTextView.text = "Студент $savedFullName"
        return view
    }

    private fun showWeightPickerDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_weight_picker)
        val kgPicker = dialog.findViewById<NumberPicker>(R.id.kgPicker)
        val gramsPicker = dialog.findViewById<NumberPicker>(R.id.gramsPicker)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val okButton = dialog.findViewById<Button>(R.id.okButton)
        val weightString = weightTextView.text.toString().replace(" кг", "").replace(",", ".")
        Log.d("UserFragment", "Weight string to parse: $weightString")
        val weightValue = try {
            weightString.toFloat()
        } catch (e: NumberFormatException) {
            Log.e("UserFragment", "Failed to parse weight: $weightString, error: ${e.message}")
            95.5f
        }
        kgPicker.minValue = 30
        kgPicker.maxValue = 150
        kgPicker.value = weightValue.toInt()
        gramsPicker.minValue = 0
        gramsPicker.maxValue = 9
        val grams = ((weightValue - weightValue.toInt()) * 10).toInt()
        gramsPicker.value = grams
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        okButton.setOnClickListener {
            val kg = kgPicker.value
            val grams = gramsPicker.value
            val weight = "$kg,$grams кг"
            weightTextView.text = weight
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putFloat("weight", "$kg.$grams".toFloat())
                .apply()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showHeightPickerDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_height_picker)
        val heightPicker = dialog.findViewById<NumberPicker>(R.id.heightPicker)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val okButton = dialog.findViewById<Button>(R.id.okButton)
        heightPicker.minValue = 100
        heightPicker.maxValue = 250
        val heightString = heightTextView.text.toString().replace(" см", "")
        val heightValue = try {
            heightString.toInt()
        } catch (e: NumberFormatException) {
            Log.e("UserFragment", "Failed to parse height: $heightString, error: ${e.message}")
            190
        }
        heightPicker.value = heightValue
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        okButton.setOnClickListener {
            val height = heightPicker.value
            heightTextView.text = "$height см"
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putInt("height", height)
                .apply()
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun showDatePickerDialog() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedBirthDate = sharedPreferences.getString("birthDate", "01.01.1995") ?: "01.01.1995"
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Парсинг сохраненной даты
        try {
            calendar.time = dateFormat.parse(savedBirthDate) ?: calendar.time
        } catch (e: Exception) {
            Log.e("UserFragment", "Failed to parse birth date: $savedBirthDate, error: ${e.message}")
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val currentDate = Calendar.getInstance()
                currentDate.set(2025, Calendar.APRIL, 22)

                var age = currentDate.get(Calendar.YEAR) - selectedYear
                if (currentDate.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }



                val formattedDate = dateFormat.format(selectedDate.time)
                birthDateTextView.text = formattedDate

                // Сохранение даты и возраста
                sharedPreferences.edit()
                    .putString("birthDate", formattedDate)
                    .putInt("age", age)
                    .apply()
            },
            year, month, day
        )

        // Ограничение максимальной даты (April 22, 2025)
        val maxDate = Calendar.getInstance()
        maxDate.set(2025, Calendar.APRIL, 22)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        // Ограничение минимальной даты (максимум 100 лет назад от текущей даты)
        val minDate = Calendar.getInstance()
        minDate.set(1925, Calendar.APRIL, 22)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        datePickerDialog.show()
    }
}