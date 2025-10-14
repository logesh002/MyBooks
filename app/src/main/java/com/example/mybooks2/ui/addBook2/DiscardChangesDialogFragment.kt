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
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to true))
            }
            .create()
    }

    companion object {
        const val TAG = "DiscardChangesDialog"
        const val REQUEST_KEY = "discard_changes_request"
        const val RESULT_KEY = "discard_confirmed"

        fun newInstance(): DiscardChangesDialogFragment {
            return DiscardChangesDialogFragment()
        }
    }
}