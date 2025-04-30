package com.example.pedometr.mariaDb

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ActivityApi {
    @GET("activity")
    suspend fun getAllActivities(): List<UserActivity>
    @POST("activity")
    suspend fun sendActivity(@Body activity: UserActivity)
}