package com.example.mybooks2.ui.home.dialog

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.example.mybooks2.databinding.FragmentFilterBottomSheetBinding
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.home.HomeViewModel
import com.example.mybooks2.ui.home.util.SortBy
import com.example.mybooks2.ui.home.util.SortOrder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheetFragment : BottomSheetDialogFragment() {
    private val viewModel: HomeViewModel by activityViewModels { HomeViewModel.Companion.factory }
    private lateinit var binding: FragmentFilterBottomSheetBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.allAuthors.observe(viewLifecycleOwner) { authors ->
            val adapter =
                ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, authors)
            binding.filterAuthor.setAdapter(adapter)

            val maxDropdownHeight = (240 * resources.displayMetrics.density).toInt()
            val fiveOrFewerItems = adapter.count <= 5

            if (fiveOrFewerItems) {
                binding.filterAuthor.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                binding.filterAuthor.dropDownHeight = maxDropdownHeight
            }
        }
        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            val adapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, tags)
            binding.filterTag.setAdapter(adapter)

            val maxDropdownHeight = (240 * resources.displayMetrics.density).toInt()
            val fiveOrFewerItems = adapter.count <= 5

            if (fiveOrFewerItems) {
                binding.filterTag.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                binding.filterTag.dropDownHeight = maxDropdownHeight
            }
        }
        val currentState = viewModel.getCurrentQueryState()
        binding.filterAuthor.setText(currentState.author, false)
        binding.filterTag.setText(currentState.tag, false)


        val chipIdToCheck = when (currentState.format) {
            BookFormat.PAPERBACK -> com.example.mybooks2.R.id.chip_format_paperback
            BookFormat.EBOOK -> com.example.mybooks2.R.id.chip_format_ebook
            BookFormat.AUDIOBOOK -> com.example.mybooks2.R.id.chip_format_audiobook
            else -> com.example.mybooks2.R.id.chip_format_any
        }
        binding.filterChipGroupFormat.check(chipIdToCheck)


        val sortByChipId = when (currentState.sortBy) {
            SortBy.DATE_ADDED -> com.example.mybooks2.R.id.chip_sort_date
            SortBy.TITLE -> com.example.mybooks2.R.id.chip_sort_title
            SortBy.AUTHOR -> com.example.mybooks2.R.id.chip_sort_author
            SortBy.RATING -> com.example.mybooks2.R.id.chip_sort_rating
        }
        binding.chipGroupSortBy.check(sortByChipId)


        if (currentState.order == SortOrder.ASCENDING) {
            binding.toggleButtonGroupOrder.check(com.example.mybooks2.R.id.button_order_asc)
        } else {
            binding.toggleButtonGroupOrder.check(com.example.mybooks2.R.id.button_order_desc)
        }

        binding.buttonApplyFilters.setOnClickListener {
            val author = binding.filterAuthor.text.toString()
            val tag = binding.filterTag.text.toString()

            val sortBy = when (binding.chipGroupSortBy.checkedChipId) {
                com.example.mybooks2.R.id.chip_sort_date -> SortBy.DATE_ADDED
                com.example.mybooks2.R.id.chip_sort_title -> SortBy.TITLE
                com.example.mybooks2.R.id.chip_sort_author -> SortBy.AUTHOR
                com.example.mybooks2.R.id.chip_sort_rating -> SortBy.RATING
                else -> SortBy.DATE_ADDED
            }
            val order = when (binding.toggleButtonGroupOrder.checkedButtonId) {
                com.example.mybooks2.R.id.button_order_asc -> SortOrder.ASCENDING
                else -> SortOrder.DESCENDING
            }
            val selectedFormat = when (binding.filterChipGroupFormat.checkedChipId) {
                com.example.mybooks2.R.id.chip_format_paperback -> BookFormat.PAPERBACK
                com.example.mybooks2.R.id.chip_format_ebook -> BookFormat.EBOOK
                com.example.mybooks2.R.id.chip_format_audiobook -> BookFormat.AUDIOBOOK
                else -> null
            }

            val bundle = bundleOf(
                "KEY_AUTHOR" to author, "KEY_TAG" to tag,
                "KEY_SORT_BY" to sortBy, "KEY_ORDER" to order,
                "KEY_FORMAT" to selectedFormat
            )
            parentFragmentManager.setFragmentResult("query_request", bundle)
            dismiss()
        }
        binding.buttonClearFilters.setOnClickListener {
            parentFragmentManager.setFragmentResult("query_request", bundleOf())
            dismiss()
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}