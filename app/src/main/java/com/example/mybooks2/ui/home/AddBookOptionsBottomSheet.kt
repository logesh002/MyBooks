package com.example.mybooks2.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.example.bookapp.ui.AddBook2
import com.example.mybooks2.MainActivity
import com.example.mybooks2.databinding.BottomSheetAddOptionsBinding
import com.example.mybooks2.ui.OnlineSearch.SearchOnlineActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddBookOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionAddManually.setOnClickListener {
            setFragmentResult("add_book_request", bundleOf("option" to "MANUAL"))
            dismiss()
        }

        binding.optionSearchOnline.setOnClickListener {
            setFragmentResult("add_book_request", bundleOf("option" to "ONLINE"))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddBookOptionsBottomSheet"
    }
}