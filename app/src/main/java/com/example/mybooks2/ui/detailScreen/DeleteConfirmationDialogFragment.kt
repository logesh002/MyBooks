import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteBookDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Book?")
            .setMessage("Are you sure you want to permanently delete this book? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                // Send a result back to the activity that opened this dialog.
                // Do NOT call the ViewModel or finish() directly from here.
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY to true))
            }
            .create()
    }

    companion object {
        // Define keys for communication
        const val TAG = "DeleteBookDialog"
        const val REQUEST_KEY = "delete_book_request"
        const val RESULT_KEY = "delete_confirmed"

        // Helper function to create a new instance of the dialog
        fun newInstance(): DeleteBookDialogFragment {
            return DeleteBookDialogFragment()
        }
    }
}