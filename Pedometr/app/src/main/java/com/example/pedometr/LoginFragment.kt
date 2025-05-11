package com.example.pedometr

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.pedometr.databinding.FragmentLoginBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUIBasedOnLoginState()
        binding.btnLogin.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("moodle_token", null)
            if (token != null) {
                sharedPreferences.edit()
                    .remove("moodle_token")
                    .remove("moodle_username")
                    .remove("moodle_fullname")
                    .remove("moodle_group")
                    .remove("is_student")
                    .apply()

                Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                updateUIBasedOnLoginState()
                (activity as? MainActivity)?.onLogout() // Вызываем метод после выхода
            } else {
                val username = binding.etUsername.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Введите имя пользователя и пароль", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                loginToMoodle(username, password)
            }
        }
        binding.btnBack.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("moodle_token", null)
            if (token != null) {
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "Авторизация обязательна", Toast.LENGTH_SHORT).show()
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
        } else {
            binding.etUsername.visibility = View.VISIBLE
            binding.etPassword.visibility = View.VISIBLE
            binding.btnLogin.text = "Войти"
            binding.tvLoggedInUser.visibility = View.GONE
            binding.tvRole.visibility = View.GONE
            binding.tvGroup.visibility = View.GONE
            binding.etUsername.text.clear()
            binding.etPassword.text.clear()
        }
    }

    private fun loginToMoodle(username: String, password: String) {
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
                    Toast.makeText(requireContext(), "Ошибка подключения: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка сервера: ${response.code}", Toast.LENGTH_LONG).show()
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
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Ошибка входа: $error", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка обработки ответа: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(requireContext(), "Ошибка получения данных пользователя: ${e.message}", Toast.LENGTH_LONG).show()
                    navigateToMainFragment()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка сервера: ${response.code}", Toast.LENGTH_LONG).show()
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
                                        Toast.makeText(requireContext(), "Не удалось определить курс", Toast.LENGTH_LONG).show()
                                        navigateToMainFragment()
                                    }
                                }
                            }
                        } else {
                            activity?.runOnUiThread {
                                Toast.makeText(requireContext(), "Не удалось получить ID пользователя", Toast.LENGTH_LONG).show()
                                navigateToMainFragment()
                            }
                        }
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_LONG).show()
                            navigateToMainFragment()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка обработки данных пользователя: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(requireContext(), "Ошибка получения курсов: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка сервера: ${response.code}", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(requireContext(), "Курсы не найдены", Toast.LENGTH_LONG).show()
                            callback(null)
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка обработки данных курсов: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(requireContext(), "Ошибка получения ролей: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка сервера: ${response.code}", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(requireContext(), "Ошибка обработки данных ролей: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(requireContext(), "Ошибка получения групп: ${e.message}", Toast.LENGTH_LONG).show()
                    navigateToMainFragment()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка сервера: ${response.code}", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(requireContext(), "Добро пожаловать, $fullName! Курс обучения: $groupName", Toast.LENGTH_LONG).show()
                            updateUIBasedOnLoginState()
                            navigateToMainFragment()
                            (activity as? MainActivity)?.onLoginSuccess() // Вызываем метод после успешного логина
                        }
                    } else if (json.has("error")) {
                        val error = json.getString("error")
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Ошибка получения групп: $error", Toast.LENGTH_LONG).show()
                            navigateToMainFragment()
                        }
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Группы не найдены", Toast.LENGTH_LONG).show()
                            navigateToMainFragment()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Ошибка обработки данных групп: ${e.message}", Toast.LENGTH_LONG).show()
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

