package com.example.pedometr

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pedometr.List.ActivityAdapter
import com.example.pedometr.List.ActivityListViewModel
import com.example.pedometr.databinding.FragmentActivityListBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.util.Calendar

@AndroidEntryPoint
class ActivityListFragment : Fragment() {

    private var _binding: FragmentActivityListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityListViewModel by viewModels()
    private lateinit var adapter: ActivityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ActivityAdapter()
        binding.activityRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.activityRecyclerView.adapter = adapter
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ActivityListViewModel.UiState.Loading -> {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.activityRecyclerView.visibility = View.GONE
                    binding.filterButtonsLayout.visibility = View.GONE
                    binding.searchView.visibility = View.GONE
                    binding.dateRangeText.visibility = View.GONE
                }
                is ActivityListViewModel.UiState.Success -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.activityRecyclerView.visibility = View.VISIBLE
                    binding.filterButtonsLayout.visibility = View.VISIBLE
                    binding.searchView.visibility = View.VISIBLE
                    binding.dateRangeText.visibility = View.VISIBLE
                    adapter.submitList(state.data)
                    updateSortAndDateText()
                }
                is ActivityListViewModel.UiState.Error -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.errorTextView.text = state.message
                    binding.retryButton.visibility = View.VISIBLE
                    binding.activityRecyclerView.visibility = View.GONE
                    binding.filterButtonsLayout.visibility = View.GONE
                    binding.searchView.visibility = View.GONE
                    binding.dateRangeText.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                is ActivityListViewModel.UiState.Empty -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.errorTextView.text = "Нет данных для отображения"
                    binding.retryButton.visibility = View.VISIBLE
                    binding.activityRecyclerView.visibility = View.GONE
                    binding.filterButtonsLayout.visibility = View.GONE
                    binding.searchView.visibility = View.GONE
                    binding.dateRangeText.visibility = View.GONE
                }
            }
        }

        // Кнопка для повторной попытки
        binding.retryButton.setOnClickListener {
            viewModel.fetchActivities(requireContext())
        }

        // Обработчики кнопок фильтров
        binding.filterDistanceStepsButton.setOnClickListener {
            viewModel.toggleSort("steps")
            updateSortAndDateText()
        }
        binding.filterTimeButton.setOnClickListener {
            viewModel.toggleSort("time")
            updateSortAndDateText()
        }
        binding.filterStepsButton.setOnClickListener {
            viewModel.toggleSort("steps")
            updateSortAndDateText()
        }
        binding.filterEmissionsButton.setOnClickListener {
            viewModel.toggleSort("group")
            updateSortAndDateText()
        }
        binding.filterDateButton.setOnClickListener {
            viewModel.toggleSort("date")
            updateSortAndDateText()
        }
        binding.filterDateButton.setOnLongClickListener {
            showDatePicker()
            true
        }
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.search(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText)
                return true
            }
        })
        viewModel.fetchActivities(requireContext())
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)
                viewModel.setSelectedDate(selectedDate)
                updateSortAndDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateSortAndDateText() {
        val selectedDate = viewModel.getSelectedDate()
        val (sortField, sortAscending) = viewModel.getSortState()

        val dateText = selectedDate?.let { "Выбрана дата: $it" } ?: "Дата не выбрана"
        val sortText = sortField?.let {
            val fieldName = when (it) {
                "distance" -> "расстоянию"
                "time" -> "времени"
                "steps" -> "шагам"
                "date" -> "дате"
                "group" -> "группе"
                else -> it
            }
            val direction = if (sortAscending == true) "по возрастанию" else "по убыванию"
            "Сортировка по $fieldName ($direction)"
        } ?: "Без сортировки"

        val resetText = "Сброс"
        val fullText = "$dateText | $sortText | $resetText"
        val spannableString = SpannableString(fullText)
        val resetStart = fullText.indexOf(resetText)
        val resetEnd = resetStart + resetText.length

        spannableString.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    viewModel.resetFilters()
                    binding.searchView.setQuery("", false)
                    binding.searchView.clearFocus()
                    updateSortAndDateText()
                }

                override fun updateDrawState(ds: android.text.TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = resources.getColor(android.R.color.holo_blue_light, null)
                }
            },
            resetStart,
            resetEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.dateRangeText.text = spannableString
        binding.dateRangeText.movementMethod = LinkMovementMethod.getInstance()
        binding.dateRangeText.highlightColor = android.graphics.Color.TRANSPARENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}