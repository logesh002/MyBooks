package com.example.mybooks2.ui.home
import android.content.Context
import android.content.SharedPreferences
enum class LayoutMode {
    GRID, LIST
}

class LayoutPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("layout_prefs", Context.MODE_PRIVATE)
    private val KEY_LAYOUT_MODE = "key_layout_mode"

    fun saveLayoutMode(mode: LayoutMode) {
        prefs.edit().putString(KEY_LAYOUT_MODE, mode.name).apply()
    }

    fun getLayoutMode(): LayoutMode {
        val modeName = prefs.getString(KEY_LAYOUT_MODE, LayoutMode.GRID.name)
        return LayoutMode.valueOf(modeName ?: LayoutMode.GRID.name)
    }
}