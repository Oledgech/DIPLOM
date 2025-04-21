package com.example.pedometr

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {

    private lateinit var avgStepsTextView: TextView
    private lateinit var totalStepsTextView: TextView
    private lateinit var barChart: BarChart
    private lateinit var btn7Days: Button
    private lateinit var btn30Days: Button
    private lateinit var btn6Months: Button
    private lateinit var btn1Year: Button
    private lateinit var btnAllData: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        avgStepsTextView = view.findViewById(R.id.avgStepsTextView)
        totalStepsTextView = view.findViewById(R.id.totalStepsTextView)
        barChart = view.findViewById(R.id.barChart)
        btn7Days = view.findViewById(R.id.btn7Days)
        btn30Days = view.findViewById(R.id.btn30Days)
        btn6Months = view.findViewById(R.id.btn6Months)
        btn1Year = view.findViewById(R.id.btn1Year)
        btnAllData = view.findViewById(R.id.btnAllData)

        updateStatistics(7)

        btn7Days.setOnClickListener {
            setButtonActive(btn7Days)
            setButtonInactive(btn30Days, btn6Months, btn1Year)
            updateStatistics(7)
        }

        btn30Days.setOnClickListener {
            setButtonActive(btn30Days)
            setButtonInactive(btn7Days, btn6Months, btn1Year)
            updateStatistics(30)
        }

        btn6Months.setOnClickListener {
            setButtonActive(btn6Months)
            setButtonInactive(btn7Days, btn30Days, btn1Year)
            updateStatistics(180)
        }

        btn1Year.setOnClickListener {
            setButtonActive(btn1Year)
            setButtonInactive(btn7Days, btn30Days, btn6Months)
            updateStatistics(365)
        }

        btnAllData.setOnClickListener {
            val allDataFragment= AllDataFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, allDataFragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }
    private fun setButtonActive(button: Button) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())) // Синий фон
        button.setTextColor(android.graphics.Color.WHITE)
    }

    private fun setButtonInactive(vararg buttons: Button) {
        buttons.forEach { button ->
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)) // Белый фон
            button.setTextColor(android.graphics.Color.BLACK)
            button.setBackgroundResource(R.drawable.button_border)
        }
    }
    private fun updateStatistics(days: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val stepsPerDay = mutableListOf<Int>()
        val labels = mutableListOf<String>()

        var totalSteps = 0
        for (i in 0 until days) {
            val date = dateFormat.format(calendar.time)
            val steps = sharedPreferences.getInt("steps_$date", 0)
            stepsPerDay.add(steps)
            totalSteps += steps
            labels.add(getDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val avgSteps = if (stepsPerDay.isNotEmpty()) totalSteps / stepsPerDay.size else 0

        avgStepsTextView.text = "В среднем\n$avgSteps"
        totalStepsTextView.text = "Всего\n$totalSteps"

        val entries = stepsPerDay.mapIndexed { index, steps -> BarEntry(index.toFloat(), steps.toFloat()) }
        val barDataSet = BarDataSet(entries, "Шаги")
        barDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val barData = BarData(barDataSet)
        barData.barWidth = 0.9f

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.invalidate()
    }

    private fun getDayOfWeek(day: Int): String {
        return when (day) {
            Calendar.MONDAY -> "ПН"
            Calendar.TUESDAY -> "ВТ"
            Calendar.WEDNESDAY -> "СР"
            Calendar.THURSDAY -> "ЧТ"
            Calendar.FRIDAY -> "ПТ"
            Calendar.SATURDAY -> "СБ"
            Calendar.SUNDAY -> "ВС"
            else -> ""
        }
    }
}