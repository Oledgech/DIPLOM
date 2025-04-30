package com.example.pedometr

import android.content.Context
import android.os.Bundle
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

        // Обработчик кнопки "Войти" или "Выйти"
        binding.btnLogin.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("moodle_token", null)

            if (token != null) {
                // Пользователь залогинен, выполняем выход
                sharedPreferences.edit()
                    .remove("moodle_token")
                    .remove("moodle_username")
                    .remove("moodle_fullname")
                    .apply()

                Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()

                // Обновляем UI после выхода
                updateUIBasedOnLoginState()
            } else {
                // Пользователь не залогинен, выполняем вход
                val username = binding.etUsername.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Введите имя пользователя и пароль", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                loginToMoodle(username, password)
            }
        }

        // Обработчик кнопки "Назад"
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun updateUIBasedOnLoginState() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("moodle_token", null)
        val fullName = sharedPreferences.getString("moodle_fullname", "Пользователь")

        if (token != null) {
            // Пользователь залогинен
            binding.etUsername.visibility = View.GONE
            binding.etPassword.visibility = View.GONE
            binding.btnLogin.text = "Выйти"
            binding.tvLoggedInUser.visibility = View.VISIBLE
            binding.tvLoggedInUser.text = "Вы вошли как: $fullName"
        } else {
            // Пользователь не залогинен
            binding.etUsername.visibility = View.VISIBLE
            binding.etPassword.visibility = View.VISIBLE
            binding.btnLogin.text = "Войти"
            binding.tvLoggedInUser.visibility = View.GONE
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
                    val jsonArray = org.json.JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val user = jsonArray.getJSONObject(0)
                        val fullName = user.optString("fullname", username)
                        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putString("moodle_fullname", fullName)
                            .apply()

                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Добро пожаловать, $fullName!", Toast.LENGTH_SHORT).show()
                            navigateToMainFragment()
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

