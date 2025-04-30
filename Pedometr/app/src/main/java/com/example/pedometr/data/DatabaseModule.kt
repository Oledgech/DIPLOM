package com.example.pedometr.data

import android.content.Context
import androidx.room.Room
import com.example.pedometr.data.AppDatabase
import com.example.pedometr.data.StepsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "step_database"
        ).build()
    }
    @Provides
    @Singleton
    fun provideStepRepository(stepsDao: StepsDao): StepRepository {
        return StepRepository(stepsDao)
    }
    @Provides
    fun provideStepsDao(database: AppDatabase): StepsDao {
        return database.stepsDao()
    }
}