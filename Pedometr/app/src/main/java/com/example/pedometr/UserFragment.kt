package com.example.pedometr

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
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

class UserFragment(
    private val sharedPreferences: SharedPreferences? = null // Allow injection for testing
) : Fragment() {

    private lateinit var stepGoalTextView: TextView
    private lateinit var decrementStepsButton: Button
    private lateinit var incrementStepsButton: Button
    private lateinit var weightTextView: TextView
    private lateinit var heightTextView: TextView
    lateinit var birthDateTextView: TextView
    private lateinit var genderSpinner: Spinner
    private lateinit var groupEditText: EditText
    private lateinit var studentNameTextView: TextView

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val sharedPrefs: SharedPreferences
        get() = sharedPreferences ?: requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        stepGoalTextView = view.findViewById(R.id.stepGoalTextView)
        decrementStepsButton = view.findViewById(R.id.decrementStepsButton)
        incrementStepsButton = view.findViewById(R.id.incrementStepsButton)
        weightTextView = view.findViewById(R.id.weightTextView)
        heightTextView = view.findViewById(R.id.heightTextView)
        birthDateTextView = view.findViewById(R.id.birthDateTextView)
        genderSpinner = view.findViewById(R.id.genderSpinner)
        groupEditText = view.findViewById(R.id.groupEditText)
        studentNameTextView = view.findViewById(R.id.studentNameTextView)

        setupViews()
        setupListeners()

        return view
    }

    private fun setupViews() {
        stepGoalTextView.text = sharedPrefs.getInt("stepGoal", 5000).toString()
        weightTextView.text = "${sharedPrefs.getFloat("weight", 95.5f)} кг"
        heightTextView.text = "${sharedPrefs.getInt("height", 190)} см"
        birthDateTextView.text = sharedPrefs.getString("birthDate", "01.01.1995")
        studentNameTextView.text = sharedPrefs.getString("moodle_fullname", "Студент")

        val genderPosition = sharedPrefs.getInt("gender", 0)
        genderSpinner.setSelection(genderPosition)

        groupEditText.setText(sharedPrefs.getString("moodle_group", ""))
    }

    private fun setupListeners() {
        decrementStepsButton.setOnClickListener {
            var steps = stepGoalTextView.text.toString().toInt()
            if (steps > 500) {
                steps -= 500
                stepGoalTextView.text = steps.toString()
                sharedPrefs.edit().putInt("stepGoal", steps).apply()
            }
        }

        incrementStepsButton.setOnClickListener {
            var steps = stepGoalTextView.text.toString().toInt()
            steps += 500
            stepGoalTextView.text = steps.toString()
            sharedPrefs.edit().putInt("stepGoal", steps).apply()
        }

        weightTextView.setOnClickListener {
            showWeightPickerDialog()
        }

        heightTextView.setOnClickListener {
            showHeightPickerDialog()
        }

        birthDateTextView.setOnClickListener {
            showDatePickerDialog()
        }

        genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPrefs.edit().putInt("gender", position).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        groupEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val group = groupEditText.text.toString()
                sharedPrefs.edit().putString("moodle_group", group).apply()
            }
        }
    }

    internal fun showWeightPickerDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_weight_picker)

        val kgPicker: NumberPicker = dialog.findViewById(R.id.kgPicker)
        val gramsPicker: NumberPicker = dialog.findViewById(R.id.gramsPicker)
        val okButton: Button = dialog.findViewById(R.id.okButton)

        kgPicker.minValue = 30
        kgPicker.maxValue = 150
        gramsPicker.minValue = 0
        gramsPicker.maxValue = 9

        val currentWeight = sharedPrefs.getFloat("weight", 95.5f)
        kgPicker.value = currentWeight.toInt()
        gramsPicker.value = ((currentWeight - currentWeight.toInt()) * 10).toInt()

        okButton.setOnClickListener {
            val kg = kgPicker.value
            val grams = gramsPicker.value
            val weight = kg + grams / 10f
            sharedPrefs.edit().putFloat("weight", weight).apply()
            weightTextView.text = "$weight кг"
            dialog.dismiss()
        }

        dialog.show()
    }

    internal fun showHeightPickerDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_height_picker)

        val heightPicker: NumberPicker = dialog.findViewById(R.id.heightPicker)
        val okButton: Button = dialog.findViewById(R.id.okButton)

        heightPicker.minValue = 120
        heightPicker.maxValue = 220

        val currentHeight = sharedPrefs.getInt("height", 190)
        heightPicker.value = currentHeight

        okButton.setOnClickListener {
            val height = heightPicker.value
            sharedPrefs.edit().putInt("height", height).apply()
            heightTextView.text = "$height см"
            dialog.dismiss()
        }

        dialog.show()
    }

    internal fun showDatePickerDialog() {
        val savedBirthDate = sharedPrefs.getString("birthDate", "01.01.1995") ?: "01.01.1995"
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
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
                sharedPrefs.edit()
                    .putString("birthDate", formattedDate)
                    .putInt("age", age)
                    .apply()
            },
            year, month, day
        )
        val maxDate = Calendar.getInstance()
        maxDate.set(2025, Calendar.APRIL, 22)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        val minDate = Calendar.getInstance()
        minDate.set(1925, Calendar.APRIL, 22)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        datePickerDialog.show()
    }
}