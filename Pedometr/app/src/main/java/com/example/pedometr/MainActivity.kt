@file:Suppress("DEPRECATION")

package com.example.pedometr

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.pedometr.data.AppDatabase

import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var db: AppDatabase
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.selectedItemId = R.id.nav_main
        val logginButton = findViewById<ImageButton>(R.id.logginButton)
        logginButton.setOnClickListener {
            loadFragment(LoginFragment())
        }
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
}