package com.example.mybooks2

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.example.mybooks2.data.AppDatabase

class MyBooksApplication: Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Get the SharedPreferences that the settings screen uses
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Read the saved theme preference
        val themePreference = sharedPreferences.getString("theme_preference", "SYSTEM")

        // Apply the theme based on the saved value
        val mode = when (themePreference) {
            "LIGHT" -> AppCompatDelegate.MODE_NIGHT_NO
            "DARK" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}