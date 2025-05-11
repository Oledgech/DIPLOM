package com.example.pedometr.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class StepRepository @Inject constructor(
    private val stepsDao: StepsDao,
) {
    val todaySteps: Flow<StepEntry?> = stepsDao.getStepsForDate(LocalDate.now().format(
        DateTimeFormatter.ISO_LOCAL_DATE))
    suspend fun saveSteps(steps: Int) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE).toString()
        stepsDao.insert(StepEntry(date = today, steps = steps))
    }
    fun getAllSteps(): Flow<List<StepEntry>> = stepsDao.getAllSteps()
    fun getStepsForDate(date: String): Flow<StepEntry?> {
        return stepsDao.getStepsForDate(date)
    }
    fun getStepsForRange(startDate: String, endDate: String): Flow<List<StepEntry>> {
        return stepsDao.getStepsForRange(startDate, endDate)
    }
}