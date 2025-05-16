package com.example.pedometr
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import junit.framework.TestCase.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After import org.junit.Before import org.junit.Test import org.junit.runner.RunWith
import java.util.regex.Pattern
@RunWith(AndroidJUnit4::class)
class LoginFragmentTest{
    private lateinit var mockWebServer: MockWebServer
    private lateinit var device: UiDevice
    @Before
    fun setUp(){
        mockWebServer = MockWebServer()
        mockWebServer.start(8080)
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val sharedPreferences =
                activity.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .clear()
                .putString(" ", "http://10.0.2.2:8080")
                .commit()
        }
    }
    @After
    fun tearDown(){
        mockWebServer.shutdown()
    }
    @Test
    fun testLoginWithEmptyFields(){
        val toastMessage = "Введите имя пользователя и пароль"
        onView(withId(R.id.btnLogin)).perform(click())
        assertTrue(
            device.wait(
                Until.hasObject(By.text(Pattern.compile(".*$toastMessage.*"))),
                6000
            )
        )
    }
    @Test
    fun testLoginWithInvalidCredentials(){
        val toastMessage = "Неверный логин или пароль"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"error": "invalidlogin"}""")
        )
        onView(withId(R.id.etUsername)).perform(typeText("wronguser"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("wrongpass"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        assertTrue(
            device.wait(
                Until.hasObject(By.text(Pattern.compile(".*$toastMessage.*"))),
                6000
            )
        )
    }
}



