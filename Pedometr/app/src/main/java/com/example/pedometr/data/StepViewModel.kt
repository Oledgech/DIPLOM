package com.example.pedometr.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepViewModel(private val repository: StepRepository) : ViewModel() {
    val todaySteps: LiveData<StepEntry?> = repository.todaySteps.asLiveData()
    fun saveSteps(steps: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.saveSteps(steps)
        }
    }
}