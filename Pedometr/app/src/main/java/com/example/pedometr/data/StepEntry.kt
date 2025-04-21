package com.example.pedometr.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "steps")
data class StepEntry(
    @PrimaryKey val date: String,
    val steps: Int

)