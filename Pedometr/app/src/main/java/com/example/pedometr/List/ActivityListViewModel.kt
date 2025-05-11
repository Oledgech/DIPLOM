package com.example.pedometr.List

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pedometr.mariaDb.ApiClient
import com.example.pedometr.mariaDb.UserActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ActivityListViewModel  @Inject constructor() : ViewModel() {
    private val _activities = MutableLiveData<List<UserActivity>>()
    private val _filteredActivities = MutableLiveData<List<HighlightedActivity>>()
    val filteredActivities: LiveData<List<HighlightedActivity>> get() = _filteredActivities

    private var selectedDate: LocalDate? = null
    private var sortField: String? = null // Текущий критерий сортировки
    private var sortOrder = mapOf<String, Boolean>(
        "distance" to false,
        "time" to false,
        "steps" to false,
        "date" to false,
        "emissions" to false
    )

    init {
        fetchActivities()
    }

    fun fetchActivities() {
        viewModelScope.launch {
            try {
                val response = ApiClient.activityApi.getAllActivities()
                Log.d("ActivityListViewModel", "Fetched activities: $response")
                _activities.value = response
                applyFiltersAndSort()
            } catch (e: Exception) {
                Log.e("ActivityListViewModel", "Error fetching activities: ${e.message}", e)
                _activities.value = emptyList()
                applyFiltersAndSort()
            }
        }
    }

    fun setSelectedDate(date: LocalDate?) {
        selectedDate = date
        Log.d("ActivityListViewModel", "Selected date set to: $date")
        applyFiltersAndSort()
    }
    fun getSelectedDate(): LocalDate? {
        return selectedDate
    }
    fun resetFilters() {
        selectedDate = null
        sortField = null
        sortOrder = sortOrder.mapValues { false }.toMutableMap()
        applyFiltersAndSort()
    }

    fun toggleSort(field: String) {
        sortField = field // Обновляем текущий критерий сортировки
        sortOrder = sortOrder.toMutableMap().apply {
            this[field] = !this[field]!!
        }
        Log.d("ActivityListViewModel", "Sorting by $field, ascending: ${sortOrder[field]}")
        applyFiltersAndSort()
    }

    fun search(query: String?) {
        applyFiltersAndSort(query)
    }
    fun getSortState(): Pair<String?, Boolean?> {
        return sortField to sortOrder[sortField]
    }
    private fun applyFiltersAndSort(query: String? = null) {
        var filtered = _activities.value ?: emptyList()

        // Фильтрация по дате
        if (selectedDate != null) {
            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            filtered = filtered.filter { activity ->
                try {
                    val activityDate = LocalDate.parse(activity.activity_date, formatter)
                    activityDate == selectedDate
                } catch (e: Exception) {
                    Log.e("ActivityListViewModel", "Date parsing error for ${activity.activity_date}: ${e.message}", e)
                    false
                }
            }
            Log.d("ActivityListViewModel", "Filtered by date $selectedDate, result size: ${filtered.size}")
        }

        // Поиск по всем полям и создание списка с подсветкой
        val highlightedList = mutableListOf<HighlightedActivity>()
        filtered.forEach { activity ->
            val matchedFields = mutableMapOf<String, String>()
            val queryLower = query?.lowercase() ?: ""

            if (query.isNullOrBlank()) {
                highlightedList.add(HighlightedActivity(activity, emptyMap()))
                return@forEach
            }

            if (activity.user_group.lowercase().contains(queryLower)) {
                matchedFields["user_group"] = activity.user_group
            }
            if (activity.activity_date.lowercase().contains(queryLower)) {
                matchedFields["activity_date"] = activity.activity_date
            }
            if (activity.steps.toString().contains(queryLower, ignoreCase = true)) {
                matchedFields["steps"] = activity.steps.toString()
            }
            if (activity.distance_km.toString().contains(queryLower, ignoreCase = true)) {
                matchedFields["distance_km"] = activity.distance_km.toString()
            }
            if (activity.active_time_minutes.toString().contains(queryLower, ignoreCase = true)) {
                matchedFields["active_time_minutes"] = activity.active_time_minutes.toString()
            }

            if (matchedFields.isNotEmpty()) {
                highlightedList.add(HighlightedActivity(activity, matchedFields))
            }
        }

        // Сортировка по текущему критерию
        if (sortField != null) {
            highlightedList.sortWith(Comparator { a, b ->
                when (sortField) {
                    "distance" -> if (sortOrder["distance"] == true) {
                        b.activity.distance_km.compareTo(a.activity.distance_km)
                    } else {
                        a.activity.distance_km.compareTo(b.activity.distance_km)
                    }
                    "time" -> if (sortOrder["time"] == true) {
                        b.activity.active_time_minutes.compareTo(a.activity.active_time_minutes)
                    } else {
                        a.activity.active_time_minutes.compareTo(b.activity.active_time_minutes)
                    }
                    "steps" -> if (sortOrder["steps"] == true) {
                        b.activity.steps.compareTo(a.activity.steps)
                    } else {
                        a.activity.steps.compareTo(b.activity.steps)
                    }
                    "date" -> {
                        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        val dateA = LocalDate.parse(a.activity.activity_date, formatter)
                        val dateB = LocalDate.parse(b.activity.activity_date, formatter)
                        if (sortOrder["date"] == true) {
                            dateB.compareTo(dateA)
                        } else {
                            dateA.compareTo(dateB)
                        }
                    }
                    "emissions" -> if (sortOrder["emissions"] == true) {
                        b.activity.steps.compareTo(a.activity.steps) // Placeholder
                    } else {
                        a.activity.steps.compareTo(b.activity.steps) // Placeholder
                    }
                    else -> 0
                }
            })
        }

        _filteredActivities.value = highlightedList
        Log.d("ActivityListViewModel", "Filtered and sorted list size: ${highlightedList.size}")
        highlightedList.forEach { Log.d("ActivityListViewModel", "Activity: time=${it.activity.active_time_minutes}, date=${it.activity.activity_date}") }
    }
}

data class HighlightedActivity( val activity: UserActivity, val matchedFields: Map<String, String>)