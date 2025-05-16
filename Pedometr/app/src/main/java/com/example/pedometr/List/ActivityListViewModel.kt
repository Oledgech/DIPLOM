package com.example.pedometr.List

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
class ActivityListViewModel @Inject constructor() : ViewModel() {
    private val _activities = MutableLiveData<List<UserActivity>>()
    private val _filteredActivities = MutableLiveData<List<HighlightedActivity>>()
    val filteredActivities: LiveData<List<HighlightedActivity>> get() = _filteredActivities

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> get() = _uiState

    private var selectedDate: LocalDate? = null
    private var sortField: String? = null
    private var sortOrder = mapOf(
        "distance" to false,
        "time" to false,
        "steps" to false,
        "date" to false,
        "group" to false
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(val data: List<HighlightedActivity>) : UiState()
        data class Error(val message: String) : UiState()
        object Empty : UiState()
    }

    fun fetchActivities(context: Context) {
        if (!isNetworkAvailable(context)) {
            _uiState.value = UiState.Error("Нет подключения к интернету")
            _activities.value = emptyList()
            applyFiltersAndSort()
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                Log.d("ActivityListViewModel", "Making API call to https://node-production-907e.up.railway.app/activity")
                val response = ApiClient.activityApi.getAllActivities()
                Log.d("ActivityListViewModel", "API response: $response")
                _activities.value = response
                applyFiltersAndSort()
                _uiState.value = if (response.isEmpty()) UiState.Empty else UiState.Success(_filteredActivities.value ?: emptyList())
            } catch (e: Exception) {
                Log.e("ActivityListViewModel", "Error fetching activities: ${e.message}", e)
                _activities.value = emptyList()
                applyFiltersAndSort()
                val errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> "Ошибка получения данных."
                    e.message?.contains("timeout") == true -> "Превышено время ожидания. Ошибка Получения данных."
                    else -> "Ошибка загрузки: ${e.message}"
                }
                _uiState.value = UiState.Error(errorMessage)
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun setSelectedDate(date: LocalDate?) {
        selectedDate = date
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
        sortField = field
        sortOrder = sortOrder.toMutableMap().apply {
            this[field] = !this[field]!!
        }
        applyFiltersAndSort()
    }

    fun search(query: String?) {
        applyFiltersAndSort(query)
    }

    fun getSortState(): Pair<String?, Boolean?> {
        return sortField to sortOrder[sortField]
    }

    private fun applyFiltersAndSort(query: String? = null) {
        val queryLower = query?.lowercase() ?: ""
        var filtered = _activities.value?.map { activity ->
            val matchedFields = mutableMapOf<String, String>()
            if (queryLower.isNotBlank()) {
                if (activity.user_group.lowercase().contains(queryLower)) {
                    matchedFields["user_group"] = queryLower
                }
                if (activity.activity_date.lowercase().contains(queryLower)) {
                    matchedFields["activity_date"] = queryLower
                }
                if (activity.steps.toString().contains(queryLower)) {
                    matchedFields["steps"] = queryLower
                }
                if (activity.distance_km.toString().contains(queryLower)) {
                    matchedFields["distance_km"] = queryLower
                }
                if (activity.active_time_minutes.toString().contains(queryLower)) {
                    matchedFields["active_time_minutes"] = queryLower
                }
            }

            HighlightedActivity(activity = activity, matchedFields = matchedFields)
        } ?: emptyList()

        if (selectedDate != null) {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            filtered = filtered.filter { activity ->
                try {
                    val activityDate = LocalDate.parse(activity.activity.activity_date, formatter)
                    activityDate == selectedDate
                } catch (e: Exception) {
                    Log.e("ActivityListViewModel", "Date parsing error for ${activity.activity.activity_date}: ${e.message}", e)
                    false
                }
            }
        }

        if (queryLower.isNotBlank()) {
            filtered = filtered.filter { activity ->
                activity.activity.user_group.lowercase().contains(queryLower) ||
                        activity.activity.activity_date.lowercase().contains(queryLower) ||
                        activity.activity.steps.toString().contains(queryLower) ||
                        activity.activity.distance_km.toString().contains(queryLower) ||
                        activity.activity.active_time_minutes.toString().contains(queryLower)
            }
        }

        if (sortField != null) {
            filtered = filtered.sortedWith(Comparator { a, b ->
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
                        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                        val dateA = LocalDate.parse(a.activity.activity_date, formatter)
                        val dateB = LocalDate.parse(b.activity.activity_date, formatter)
                        if (sortOrder["date"] == true) {
                            dateB.compareTo(dateA)
                        } else {
                            dateA.compareTo(dateB)
                        }
                    }
                    "group" -> if (sortOrder["group"] == true) {
                        b.activity.user_group.compareTo(a.activity.user_group)
                    } else {
                        a.activity.user_group.compareTo(b.activity.user_group)
                    }
                    else -> 0
                }
            })
        }

        _filteredActivities.value = filtered
        _uiState.value = if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
    }
}

data class HighlightedActivity(val activity: UserActivity, val matchedFields: Map<String, String>)