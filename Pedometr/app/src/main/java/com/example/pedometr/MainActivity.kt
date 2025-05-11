@file:Suppress("DEPRECATION")

package com.example.pedometr

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.pedometr.data.AppDatabase

import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var db: AppDatabase
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var logginButton: ImageButton
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        logginButton = findViewById(R.id.logginButton)

        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("moodle_token", null)
        val isStudent = sharedPreferences.getBoolean("is_student", false)

        if (token == null) {
            bottomNavigationView.visibility = View.GONE
            logginButton.isEnabled = false
            if (savedInstanceState == null) {
                loadFragment(LoginFragment())
            }
        } else {
            if (!isStudent) {
                bottomNavigationView.menu.clear()
                bottomNavigationView.inflateMenu(R.menu.bottom_nav_teacher_menu)
                bottomNavigationView.visibility = View.VISIBLE
                logginButton.isEnabled = true
                if (savedInstanceState == null) {
                    loadFragment(ActivityListFragment())
                }
                bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.nav_main -> {
                            loadFragment(ActivityListFragment())
                            true
                        }
                        else -> false
                    }
                }
            } else {
                bottomNavigationView.menu.clear()
                bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu)
                bottomNavigationView.visibility = View.VISIBLE
                logginButton.isEnabled = true
                bottomNavigationView.selectedItemId = R.id.nav_main
                if (savedInstanceState == null) {
                    loadFragment(MainFragment())
                }
                bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.nav_main -> {
                            loadFragment(MainFragment())
                            true
                        }
                        R.id.nav_stat -> {
                            loadFragment(StatisticsFragment())
                            true
                        }
                        R.id.nav_user -> {
                            loadFragment(UserFragment())
                            true
                        }
                        else -> false
                    }
                }
            }
            logginButton.setOnClickListener {
                loadFragment(LoginFragment())
            }
        }

        requestPermission()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermission() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Разрешение на шаги не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }

        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun onLoginSuccess() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("moodle_token", null)
        val isStudent = sharedPreferences.getBoolean("is_student", false)

        if (token != null) {
            if (!isStudent) {
                bottomNavigationView.menu.clear()
                bottomNavigationView.inflateMenu(R.menu.bottom_nav_teacher_menu)
                bottomNavigationView.visibility = View.VISIBLE
                logginButton.isEnabled = true
                loadFragment(ActivityListFragment())
                bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.nav_main -> {
                            loadFragment(ActivityListFragment())
                            true
                        }
                        else -> false
                    }
                }
            } else {
                bottomNavigationView.menu.clear()
                bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu)
                bottomNavigationView.visibility = View.VISIBLE
                logginButton.isEnabled = true
                bottomNavigationView.selectedItemId = R.id.nav_main
                loadFragment(MainFragment())
                bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.nav_main -> {
                            loadFragment(MainFragment())
                            true
                        }
                        R.id.nav_stat -> {
                            loadFragment(StatisticsFragment())
                            true
                        }
                        R.id.nav_user -> {
                            loadFragment(UserFragment())
                            true
                        }
                        else -> false
                    }
                }
            }
            logginButton.setOnClickListener {
                loadFragment(LoginFragment())
            }
        }
    }

    fun onLogout() {
        bottomNavigationView.visibility = View.GONE
        logginButton.isEnabled = false
        loadFragment(LoginFragment())
    }
}