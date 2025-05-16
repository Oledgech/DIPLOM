package com.example.pedometr
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
@RunWith(MockitoJUnitRunner::class)
class UserFragmentTest {
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    @Mock
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var scenario: FragmentScenario<UserFragment>
    @Before
    fun setUp() {
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)
        doNothing().`when`(mockSharedPreferencesEditor).apply()
        `when`(mockSharedPreferencesEditor.putInt(anyString(), anyInt())).thenAnswer { invocation ->
            mockSharedPreferencesEditor
        }
        `when`(mockSharedPreferencesEditor.putFloat(anyString(), anyFloat())).thenAnswer { invocation ->
            mockSharedPreferencesEditor
        }
        `when`(mockSharedPreferencesEditor.putString(anyString(), nullable(String::class.java))).thenAnswer { invocation ->
            mockSharedPreferencesEditor
        }
        `when`(mockSharedPreferences.getInt("stepGoal", 5000)).thenReturn(5000)
        scenario = launchFragmentInContainer<UserFragment>(Bundle()) {
            UserFragment(mockSharedPreferences)
        }
    }
    @Test
    fun testDecrementStepsButton() {
        scenario.onFragment { fragment ->
            val stepGoalTextView: TextView = fragment.view!!.findViewById(R.id.stepGoalTextView)
            val decrementStepsButton: Button = fragment.view!!.findViewById(R.id.decrementStepsButton)
            stepGoalTextView.text = "5000"
            decrementStepsButton.performClick()
            val captor = ArgumentCaptor.forClass(Int::class.java)
            verify(mockSharedPreferencesEditor).putInt(eq("stepGoal"), captor.capture())
            assert(captor.value == 4500)
            assert(stepGoalTextView.text.toString() == "4500")
        }
    }
    @Test
    fun testIncrementStepsButton() {
        scenario.onFragment { fragment ->
            val stepGoalTextView: TextView = fragment.view!!.findViewById(R.id.stepGoalTextView)
            val incrementStepsButton: Button = fragment.view!!.findViewById(R.id.incrementStepsButton)
            stepGoalTextView.text = "5000"
            incrementStepsButton.performClick()
            val captor = ArgumentCaptor.forClass(Int::class.java)
            verify(mockSharedPreferencesEditor).putInt(eq("stepGoal"), captor.capture())
            assert(captor.value == 5500)
            assert(stepGoalTextView.text.toString() == "5500")
        }
    }
}