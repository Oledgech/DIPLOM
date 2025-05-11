package com.example.pedometr

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
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

        viewModel.filteredActivities.observe(viewLifecycleOwner) { activities ->
            adapter.submitList(activities)
            updateSortAndDateText()
        }

        binding.filterDistanceStepsButton.setOnClickListener {
            viewModel.toggleSort("distance")
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
        binding.filterDateButton.setOnClickListener {
            viewModel.toggleSort("date")
            updateSortAndDateText()
        }
        binding.filterDateButton.setOnLongClickListener {
            showDatePicker()
            true
        }
        binding.filterEmissionsButton.setOnClickListener {
            viewModel.toggleSort("emissions")
            updateSortAndDateText()
        }
        updateSortAndDateText()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.search(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText)
                return true
            }
        })
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)
                viewModel.setSelectedDate(selectedDate)
                binding.dateRangeText.text = "Selected Date: $selectedDate"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    private fun updateSortAndDateText() {
        val selectedDate = viewModel.getSelectedDate()
        val (sortField, sortAscending) = viewModel.getSortState()

        val dateText = if (selectedDate != null) {
            "Выбрана дата: $selectedDate"
        } else {
            "Дата не выбрана"
        }

        val sortText = if (sortField != null && sortAscending != null) {
            val direction = if (sortAscending) "по убыванию" else "по возрастанию"
            "Отсортировано по $sortField ($direction)"
        } else {
            "Не сортировано"
        }

        val resetText = "Сброс"
        val fullText = "$dateText | $sortText | $resetText"
        val spannableString = SpannableString(fullText)
        val resetStart = fullText.indexOf(resetText)
        val resetEnd = resetStart + resetText.length
        spannableString.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    viewModel.resetFilters()
                    binding.searchView.setQuery("", false) // Сбрасываем поиск
                    binding.searchView.clearFocus()
                    updateSortAndDateText()
                }

                override fun updateDrawState(ds: TextPaint) {
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