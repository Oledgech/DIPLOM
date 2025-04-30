package com.example.pedometr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stepEntity: StepEntry)

    @Query("SELECT * FROM steps WHERE date = :date")
    fun getStepsForDate(date: String): Flow<StepEntry?>

    @Query("SELECT * FROM steps")
    fun getAllSteps(): Flow<List<StepEntry>>
    @Query("SELECT * FROM steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getStepsForRange(startDate: String, endDate: String): Flow<List<StepEntry>>
}