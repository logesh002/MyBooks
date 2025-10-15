package com.example.mybooks2.ui.setting

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.mybooks2.R
import com.example.mybooks2.ui.addBook2.DiscardChangesDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ResetConfirmationDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Database?")
            .setMessage("Are you sure you want to permanently delete these books?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                setFragmentResult(
                    REQUEST_KEY, bundleOf(
                        RESULT_KEY to true))
            }
            .create()
    }

    companion object {
        const val TAG = "ResetConfirmationDialog"

        const val REQUEST_KEY = "reset_request"
        const val RESULT_KEY = "reset_confirmed"

        fun newInstance(count: Int): ResetConfirmationDialogFragment {
            return ResetConfirmationDialogFragment()
        }
    }
}