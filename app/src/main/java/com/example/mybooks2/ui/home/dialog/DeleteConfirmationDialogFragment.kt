package com.example.mybooks2.ui.home.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.mybooks2.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteConfirmationDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val count = arguments?.getInt(ARG_COUNT) ?: 0
        val title = resources.getQuantityString(R.plurals.delete_dialog_title, count, count)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage("Are you sure you want to permanently delete these books?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                setFragmentResult("delete_request", bundleOf("result" to true))
            }
            .create()
    }

    companion object {
        const val TAG = "DeleteConfirmationDialog"
        private const val ARG_COUNT = "arg_count"

        fun newInstance(count: Int): DeleteConfirmationDialogFragment {
            return DeleteConfirmationDialogFragment().apply {
                arguments = bundleOf(ARG_COUNT to count)
            }
        }
    }
}