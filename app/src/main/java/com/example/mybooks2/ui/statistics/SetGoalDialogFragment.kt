package com.example.mybooks2.ui.statistics

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceManager
import com.example.mybooks2.R // Use your app's R file
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SetGoalDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_goal, null)
        val goalEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_goal)

        // Pre-populate with the current goal from SharedPreferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentGoal = prefs.getString("yearly_reading_goal", "25") ?: "25"
        goalEditText.setText(currentGoal)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Yearly Reading Goal")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newGoalStr = goalEditText.text.toString()
                // Send the new goal string back to the calling fragment
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to newGoalStr))
            }
            .create()
    }

    companion object {
        const val TAG = "SetGoalDialog"
        const val REQUEST_KEY = "set_goal_request"
        const val RESULT_KEY = "new_goal"

        fun newInstance(): SetGoalDialogFragment {
            return SetGoalDialogFragment()
        }
    }
}