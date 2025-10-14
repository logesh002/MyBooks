package com.example.mybooks2.ui.addBook2

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DiscardChangesDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Discard changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setNegativeButton("Cancel", null) // Does nothing, just dismisses
            .setPositiveButton("Discard") { _, _ ->
                // Send a result back to the activity that opened this dialog.
                // Do NOT call finish() directly from here.
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to true))
            }
            .create()
    }

    companion object {
        // Define keys for communication
        const val TAG = "DiscardChangesDialog"
        const val REQUEST_KEY = "discard_changes_request"
        const val RESULT_KEY = "discard_confirmed"

        // Helper function to create a new instance of the dialog
        fun newInstance(): DiscardChangesDialogFragment {
            return DiscardChangesDialogFragment()
        }
    }
}