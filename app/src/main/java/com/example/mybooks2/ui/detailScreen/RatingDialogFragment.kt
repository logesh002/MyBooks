package com.example.mybooks2.ui.detailScreen

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.RatingBar
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.mybooks2.R // Use your project's R file
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class RatingDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.dialog_rating_bar)
        val reviewEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_review)

        Log.d("rating", arguments?.getFloat(ARG_RATING).toString() )
        ratingBar.rating = arguments?.getFloat(ARG_RATING) ?: 0f
        reviewEditText.setText(arguments?.getString(ARG_REVIEW) ?: "")

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Skip", null)
            .setPositiveButton("Save") { _, _ ->
                val rating = ratingBar.rating
                val review = reviewEditText.text.toString()

                // Send the result back to the activity
                setFragmentResult(REQUEST_KEY, bundleOf(
                    RESULT_RATING to rating,
                    RESULT_REVIEW to review
                ))
            }
            .create()
    }

    companion object {
        const val TAG = "RatingDialog"
        const val REQUEST_KEY = "rating_request"
        const val RESULT_RATING = "rating_result"
        const val RESULT_REVIEW = "review_result"

        private const val ARG_RATING = "arg_rating"
        private const val ARG_REVIEW = "arg_review"

        // Factory function to create a new instance with arguments
        fun newInstance(currentRating: Float, currentReview: String): RatingDialogFragment {
            return RatingDialogFragment().apply {
                arguments = bundleOf(
                    ARG_RATING to currentRating,
                    ARG_REVIEW to currentReview
                )
            }
        }
    }
}