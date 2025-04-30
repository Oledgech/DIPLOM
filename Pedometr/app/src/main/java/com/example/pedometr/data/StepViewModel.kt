package com.example.pedometr.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StepViewModel@Inject constructor(private val repository: StepRepository) : ViewModel() {
    val todaySteps: LiveData<StepEntry?> = repository.todaySteps.asLiveData()
    fun saveSteps(steps: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.saveSteps(steps)
        }
    }

    fun getStepsForRange(startDate: String, endDate: String): Flow<List<StepEntry>> {
        return repository.getStepsForRange(startDate, endDate)
    }
    fun getAllSteps(): Flow<List<StepEntry>> {
        return repository.getAllSteps()
    }
}