package com.example.pedometr

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.example.pedometr.data.StepViewModel
import com.example.pedometr.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StepViewModel by viewModels()
    private val uiState = MutableLiveData<UiState>()
    private var currentDays: Int = 7

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
        object Empty : UiState()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.statisticseContent.visibility = View.VISIBLE
                }
                is UiState.Loading -> {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.statisticseContent.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.statisticseContent.visibility = View.VISIBLE
                }
                is UiState.Error -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.errorTextView.text = state.message
                    binding.retryButton.visibility = View.VISIBLE
                    binding.statisticseContent.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                is UiState.Empty -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.errorTextView.text = "Данные отсутствуют"
                    binding.retryButton.visibility = View.GONE
                    binding.statisticseContent.visibility = View.GONE
                }
            }
        }
        updateStatistics(7)
        binding.btn7Days.setOnClickListener {
            setButtonActive(binding.btn7Days)
            setButtonInactive(binding.btn30Days, binding.btn6Months, binding.btn1Year)
            currentDays = 7
            updateStatistics(7)
        }
        binding.btn30Days.setOnClickListener {
            setButtonActive(binding.btn30Days)
            setButtonInactive(binding.btn7Days, binding.btn6Months, binding.btn1Year)
            currentDays = 30
            updateStatistics(30)
        }
        binding.btn6Months.setOnClickListener {
            setButtonActive(binding.btn6Months)
            setButtonInactive(binding.btn7Days, binding.btn30Days, binding.btn1Year)
            currentDays = 180
            updateStatistics(180)
        }
        binding.btn1Year.setOnClickListener {
            setButtonActive(binding.btn1Year)
            setButtonInactive(binding.btn7Days, binding.btn30Days, binding.btn6Months)
            currentDays = 365
            updateStatistics(365)
        }

        binding.btnAllData.setOnClickListener {
            val allDataFragment = AllDataFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, allDataFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.retryButton.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                uiState.value = UiState.Error("Нет подключения к интернету")
            } else {
                updateStatistics(currentDays)
            }
        }
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
        uiState.value = UiState.Loading

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -days + 1)
            val startDate = dateFormat.format(calendar.time)

            val stepsData = runBlocking {
                viewModel.getStepsForRange(startDate, endDate).first()
            }

            if (stepsData.isEmpty()) {
                uiState.value = UiState.Empty
                return
            }

            val stepsPerDay = mutableListOf<Int>()
            val labels = mutableListOf<String>()
            var totalSteps = 0

            val stepsMap = stepsData.associateBy { it.date }

            calendar.time = dateFormat.parse(startDate)!!
            val endDateParsed = dateFormat.parse(endDate)!!
            while (calendar.time <= endDateParsed) {
                val date = dateFormat.format(calendar.time)
                val steps = stepsMap[date]?.steps ?: 0
                stepsPerDay.add(steps)
                totalSteps += steps
                labels.add(getDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val avgSteps = if (stepsPerDay.isNotEmpty()) totalSteps / stepsPerDay.size else 0

            binding.avgStepsTextView.text = "В среднем\n$avgSteps"
            binding.totalStepsTextView.text = "Всего\n$totalSteps"

            val entries = stepsPerDay.mapIndexed { index, steps -> BarEntry(index.toFloat(), steps.toFloat()) }
            val barDataSet = BarDataSet(entries, "Шаги")
            barDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            val barData = BarData(barDataSet)
            barData.barWidth = 0.9f

            binding.barChart.data = barData
            binding.barChart.description.isEnabled = false
            binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            binding.barChart.invalidate()

            uiState.value = UiState.Success
        } catch (e: Exception) {
            uiState.value = UiState.Error("Ошибка загрузки данных: ${e.message}")
        }
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

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}