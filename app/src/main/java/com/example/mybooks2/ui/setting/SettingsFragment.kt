package com.example.mybooks2.ui.setting

import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.R
import com.example.mybooks2.data.AppDatabase
import com.example.mybooks2.ui.detailScreen.BookDetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.getValue

class SettingsFragment : PreferenceFragmentCompat() {

    val viewModel by viewModels<SettingsViewModel> { SettingsViewModel.Companion.factory }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
             viewModel.exportToCsv(it)
        }
    }

    // Launcher for opening an existing file (Import)
    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
             viewModel.importFromCsv(it)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        // --- Theme Preference Logic ---
        val themePreference: ListPreference? = findPreference("theme_preference")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val mode = when (newValue as String) {
                "LIGHT" -> AppCompatDelegate.MODE_NIGHT_NO
                "DARK" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            true
        }


        // --- Reset Database Logic ---
        val resetPreference: Preference? = findPreference("reset_database")
        resetPreference?.setOnPreferenceClickListener {
            showResetConfirmationDialog()
            true
        }

        findPreference<Preference>("export_data")?.setOnPreferenceClickListener {
            // Suggest a filename like "MyBooks_Export_20251014.csv"
            val fileName = "MyBooks_Export_${System.currentTimeMillis()}.csv"
            exportLauncher.launch(fileName)
            true
        }

        findPreference<Preference>("import_data")?.setOnPreferenceClickListener {
            importLauncher.launch("text/*")
            true
        }

        // --- App Version Logic ---
        val versionPreference: Preference? = findPreference("app_version")
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            versionPreference?.summary = pInfo.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.toastMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Database?")
            .setMessage("Are you sure you want to delete all your saved books and tags? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset") { _, _ ->
                // Call your database clearing logic here
             //   val database = AppDatabase.getDatabase(requireContext().applicationContext)
                val database = (requireActivity().application as MyBooksApplication).database
                lifecycleScope.launch(Dispatchers.IO) {
                    database.clearAllTables()
                    // You might want to navigate out or show a Toast
                }
                Toast.makeText(requireContext(), "Database has been reset.", Toast.LENGTH_SHORT).show()

            }
            .show()
    }
}