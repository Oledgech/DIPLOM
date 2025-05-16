package com.example.pedometr

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.pedometr.databinding.FragmentLoginBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val uiState = MutableLiveData<UiState>()
    private var lastUsername: String? = null
    private var lastPassword: String? = null

    // Состояния UI
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String? = null) : UiState()
        data class Error(val message: String, val showLoginButtonOnly: Boolean = false) : UiState()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Начальное состояние
        updateUIBasedOnLoginState()

        // Наблюдение за состоянием UI
        uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.loginContent.visibility = View.VISIBLE
                    binding.btnLogin.visibility = View.VISIBLE
                }
                is UiState.Loading -> {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.loginContent.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    binding.loginContent.visibility = View.VISIBLE
                    binding.btnLogin.visibility = View.VISIBLE
                    state.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    }
                    updateUIBasedOnLoginState()
                }
                is UiState.Error -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.errorTextView.visibility = View.VISIBLE
                    binding.errorTextView.text = state.message
                    binding.retryButton.visibility = View.VISIBLE
                    if (state.showLoginButtonOnly) {
                        binding.loginContent.visibility = View.VISIBLE
                        binding.btnLogin.visibility = View.VISIBLE
                        binding.btnLogin.text = "Войти"
                        binding.etUsername.visibility = View.GONE
                        binding.etPassword.visibility = View.GONE
                        binding.tvLoggedInUser.visibility = View.GONE
                        binding.tvRole.visibility = View.GONE
                        binding.tvGroup.visibility = View.GONE
                        binding.btnBack.visibility = View.GONE
                    } else {
                        binding.loginContent.visibility = View.GONE
                        binding.btnLogin.visibility = View.GONE
                    }
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Обработчик кнопки логина/выхода
        binding.btnLogin.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("moodle_token", null)
            if (token != null) {
                // Выход из аккаунта
                sharedPreferences.edit()
                    .remove("moodle_token")
                    .remove("moodle_username")
                    .remove("moodle_fullname")
                    .remove("moodle_group")
                    .remove("is_student")
                    .apply()
                lastUsername = null
                lastPassword = null
                uiState.value = UiState.Success("Вы вышли из аккаунта")
                (activity as? MainActivity)?.onLogout()
            } else {
                // Логин или повторный ввод
                if (binding.etUsername.visibility == View.VISIBLE && binding.etPassword.visibility == View.VISIBLE) {
                    val username = binding.etUsername.text.toString().trim()
                    val password = binding.etPassword.text.toString().trim()

                    if (username.isEmpty() || password.isEmpty()) {
                        uiState.value = UiState.Error("Введите имя пользователя и пароль", showLoginButtonOnly = true)
                        return@setOnClickListener
                    }

                    if (!isNetworkAvailable(requireContext())) {
                        uiState.value = UiState.Error("Нет подключения к интернету")
                        return@setOnClickListener
                    }

                    lastUsername = username
                    lastPassword = password
                    loginToMoodle(username, password)
                } else {
                    // Показываем форму для повторного ввода
                    binding.etUsername.visibility = View.VISIBLE
                    binding.etPassword.visibility = View.VISIBLE
                    binding.etUsername.text.clear()
                    binding.etPassword.text.clear()
                    binding.btnBack.visibility = View.GONE
                    binding.errorTextView.visibility = View.GONE
                    binding.retryButton.visibility = View.GONE
                    uiState.value = UiState.Idle
                }
            }
        }

        // Обработчик кнопки "Назад"
        binding.btnBack.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("moodle_token", null)
            if (token != null) {
                parentFragmentManager.popBackStack()
            } else {
                uiState.value = UiState.Error("Авторизация обязательна", showLoginButtonOnly = true)
            }
        }

        // Обработчик кнопки повторной попытки
        binding.retryButton.setOnClickListener {
            if (lastUsername.isNullOrEmpty() || lastPassword.isNullOrEmpty()) {
                // Показываем форму для ввода
                binding.etUsername.visibility = View.VISIBLE
                binding.etPassword.visibility = View.VISIBLE
                binding.etUsername.text.clear()
                binding.etPassword.text.clear()
                binding.btnBack.visibility = View.GONE
                binding.errorTextView.visibility = View.GONE
                binding.retryButton.visibility = View.GONE
                binding.loginContent.visibility = View.VISIBLE
                binding.btnLogin.visibility = View.VISIBLE
                uiState.value = UiState.Idle
            } else if (!isNetworkAvailable(requireContext())) {
                uiState.value = UiState.Error("Нет подключения к интернету")
            } else {
                loginToMoodle(lastUsername!!, lastPassword!!)
            }
        }
    }

    private fun updateUIBasedOnLoginState() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("moodle_token", null)
        val fullName = sharedPreferences.getString("moodle_fullname", "Пользователь")
        val group = sharedPreferences.getString("moodle_group", null)
        val isStudent = sharedPreferences.getBoolean("is_student", false)

        if (token != null) {
            binding.etUsername.visibility = View.GONE
            binding.etPassword.visibility = View.GONE
            binding.btnLogin.text = "Выйти"
            binding.tvLoggedInUser.visibility = View.VISIBLE
            binding.tvRole.visibility = View.VISIBLE
            binding.tvGroup.visibility = View.VISIBLE
            binding.tvLoggedInUser.text = "Вы вошли как: $fullName"
            binding.tvRole.text = "Роль: ${if (isStudent) "Студент" else "Не студент"}"
            binding.tvGroup.text = if (group.isNullOrEmpty()) "Группа: Нет данных" else "Группа: $group"
            binding.btnBack.visibility = View.VISIBLE
        } else {
            binding.etUsername.visibility = View.VISIBLE
            binding.etPassword.visibility = View.VISIBLE
            binding.btnLogin.text = "Войти"
            binding.tvLoggedInUser.visibility = View.GONE
            binding.tvRole.visibility = View.GONE
            binding.tvGroup.visibility = View.GONE
            binding.etUsername.text.clear()
            binding.etPassword.text.clear()
            binding.btnBack.visibility = View.GONE
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loginToMoodle(username: String, password: String) {
        uiState.value = UiState.Loading
        val moodleUrl = "https://eios.mauniver.ru/moodle"
        val wsUrl = "$moodleUrl/login/token.php"

        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("service", "moodle_mobile_app")
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    uiState.value = UiState.Error("Ошибка подключения: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка сервера: ${response.code}")
                    }
                    return
                }

                try {
                    val json = JSONObject(responseBody)
                    if (json.has("token")) {
                        val token = json.getString("token")
                        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putString("moodle_token", token)
                            .putString("moodle_username", username)
                            .apply()

                        fetchUserInfo(token, username)
                    } else if (json.has("error")) {
                        val error = json.getString("error")
                        val errorMessage = if (error.contains("invalidlogin", ignoreCase = true)) {
                            "Неверный логин или пароль"
                        } else {
                            "Ошибка входа: $error"
                        }
                        activity?.runOnUiThread {
                            uiState.value = UiState.Error(errorMessage, showLoginButtonOnly = true)
                        }
                    } else {
                        activity?.runOnUiThread {
                            uiState.value = UiState.Error("Неизвестная ошибка", showLoginButtonOnly = true)
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка обработки ответа: ${e.message}", showLoginButtonOnly = true)
                    }
                }
            }
        })
    }

    private fun fetchUserInfo(token: String, username: String) {
        val moodleUrl = "https://eios.mauniver.ru/moodle"
        val wsFunction = "core_user_get_users_by_field"
        val wsUrl = "$moodleUrl/webservice/rest/server.php?wstoken=$token&wsfunction=$wsFunction&moodlewsrestformat=json"

        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("field", "username")
            .add("values[0]", username)
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    uiState.value = UiState.Error("Ошибка получения данных пользователя: ${e.message}")
                    navigateToMainFragment()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка сервера: ${response.code}")
                        navigateToMainFragment()
                    }
                    return
                }

                try {
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val user = jsonArray.getJSONObject(0)
                        val fullName = user.optString("fullname", username)
                        val userId = user.optInt("id", -1)

                        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putString("moodle_fullname", fullName)
                            .apply()

                        if (userId != -1) {
                            fetchUserCourses(token, userId) { courseId ->
                                if (courseId != null) {
                                    fetchUserRole(token, userId, courseId) { isStudent ->
                                        sharedPreferences.edit()
                                            .putBoolean("is_student", isStudent)
                                            .apply()
                                        fetchUserGroups(token, userId, courseId)
                                    }
                                } else {
                                    activity?.runOnUiThread {
                                        uiState.value = UiState.Error("Не удалось определить курс")
                                        navigateToMainFragment()
                                    }
                                }
                            }
                        } else {
                            activity?.runOnUiThread {
                                uiState.value = UiState.Error("Не удалось получить ID пользователя")
                                navigateToMainFragment()
                            }
                        }
                    } else {
                        activity?.runOnUiThread {
                            uiState.value = UiState.Error("Пользователь не найден")
                            navigateToMainFragment()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка обработки данных пользователя: ${e.message}")
                        navigateToMainFragment()
                    }
                }
            }
        })
    }

    private fun fetchUserCourses(token: String, userId: Int, callback: (Int?) -> Unit) {
        val moodleUrl = "https://eios.mauniver.ru/moodle"
        val wsFunction = "core_enrol_get_users_courses"
        val wsUrl = "$moodleUrl/webservice/rest/server.php?wstoken=$token&wsfunction=$wsFunction&moodlewsrestformat=json&userid=$userId"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(wsUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    uiState.value = UiState.Error("Ошибка получения курсов: ${e.message}")
                    callback(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка сервера: ${response.code}")
                        callback(null)
                    }
                    return
                }

                try {
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val course = jsonArray.getJSONObject(0)
                        val courseId = course.getInt("id")
                        callback(courseId)
                    } else {
                        activity?.runOnUiThread {
                            uiState.value = UiState.Error("Курсы не найдены")
                            callback(null)
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка обработки данных курсов: ${e.message}")
                        callback(null)
                    }
                }
            }
        })
    }

    private fun fetchUserRole(token: String, userId: Int, courseId: Int, callback: (Boolean) -> Unit) {
        val moodleUrl = "https://eios.mauniver.ru/moodle"
        val wsFunction = "core_enrol_get_enrolled_users"
        val wsUrl = "$moodleUrl/webservice/rest/server.php?wstoken=$token&wsfunction=$wsFunction&moodlewsrestformat=json&courseid=$courseId"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(wsUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    uiState.value = UiState.Error("Ошибка получения ролей: ${e.message}")
                    callback(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка сервера: ${response.code}")
                        callback(false)
                    }
                    return
                }

                try {
                    val jsonArray = JSONArray(responseBody)
                    var isStudent = false
                    for (i in 0 until jsonArray.length()) {
                        val user = jsonArray.getJSONObject(i)
                        if (user.getInt("id") == userId) {
                            val roles = user.getJSONArray("roles")
                            for (j in 0 until roles.length()) {
                                val role = roles.getJSONObject(j)
                                val roleId = role.getInt("roleid")
                                if (roleId == 5) {
                                    isStudent = true
                                    break
                                }
                            }
                            break
                        }
                    }
                    callback(isStudent)
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка обработки данных ролей: ${e.message}")
                        callback(false)
                    }
                }
            }
        })
    }

    private fun fetchUserGroups(token: String, userId: Int, courseId: Int) {
        val moodleUrl = "https://eios.mauniver.ru/moodle"
        val wsFunction = "core_group_get_course_user_groups"
        val wsUrl = "$moodleUrl/webservice/rest/server.php?wstoken=$token&wsfunction=$wsFunction&moodlewsrestformat=json&courseid=$courseId&userid=$userId"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(wsUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    uiState.value = UiState.Error("Ошибка получения групп: ${e.message}")
                    navigateToMainFragment()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка сервера: ${response.code}")
                        navigateToMainFragment()
                    }
                    return
                }

                try {
                    val json = JSONObject(responseBody)
                    if (json.has("groups")) {
                        val groupsArray = json.getJSONArray("groups")
                        val groupNames = mutableListOf<String>()
                        for (i in 0 until groupsArray.length()) {
                            val group = groupsArray.getJSONObject(i)
                            val groupName = group.optString("name", "Неизвестная группа")
                            groupNames.add(groupName)
                        }

                        val groupName = if (groupNames.isNotEmpty()) {
                            groupNames.first()
                        } else {
                            "Нет групп"
                        }

                        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putString("moodle_group", groupName)
                            .apply()

                        activity?.runOnUiThread {
                            val fullName = sharedPreferences.getString("moodle_fullname", "Пользователь")
                            uiState.value = UiState.Success("Добро пожаловать, $fullName! Курс обучения: $groupName")
                            (activity as? MainActivity)?.onLoginSuccess()
                        }
                    } else if (json.has("error")) {
                        val error = json.getString("error")
                        activity?.runOnUiThread {
                            uiState.value = UiState.Error("Ошибка получения групп: $error")
                            navigateToMainFragment()
                        }
                    } else {
                        activity?.runOnUiThread {
                            uiState.value = UiState.Error("Группы не найдены")
                            navigateToMainFragment()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        uiState.value = UiState.Error("Ошибка обработки данных групп: ${e.message}")
                        navigateToMainFragment()
                    }
                }
            }
        })
    }

    private fun navigateToMainFragment() {
        val mainFragment = MainFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, mainFragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
