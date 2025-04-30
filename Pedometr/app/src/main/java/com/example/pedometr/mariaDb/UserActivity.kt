package com.example.pedometr.mariaDb

data class UserActivity(
    val user_group: String,
    val activity_date: String,
    val steps: Int,
    val distance_km: Double,
    val active_time_minutes: Int
)