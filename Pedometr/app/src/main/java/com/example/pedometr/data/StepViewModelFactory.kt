package com.example.pedometr.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StepViewModelFactory(private val repository: StepRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T
    {
        if (modelClass.isAssignableFrom(StepViewModel::class.java))
        {
            @Suppress("UNCHECKED_CAST") return StepViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}