package com.example.mybooks2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import com.example.mybooks2.R
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.mybooks2.databinding.ActivitySearchBinding
import com.example.mybooks2.databinding.FragmentFilterBottomSheetBinding
import com.example.mybooks2.ui.addBook2.BookFormat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.getValue

class FilterBottomSheetFragment : BottomSheetDialogFragment() {
    // ... use ViewBinding ...

    // Use activityViewModels() to get the ViewModel shared with HomeFragment
    private val viewModel: HomeViewModel by activityViewModels { HomeViewModel.factory }
    private lateinit var binding: FragmentFilterBottomSheetBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate autocomplete fields
        viewModel.allAuthors.observe(viewLifecycleOwner) { authors ->
            val adapter =
                ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, authors)
            binding.filterAuthor.setAdapter(adapter)
        }
        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tags)
            binding.filterTag.setAdapter(adapter)
        }

        binding.buttonApplyFilters.setOnClickListener {
            val bundle = bundleOf(
                "KEY_AUTHOR" to binding.filterAuthor.text.toString(),
                "KEY_TAG" to binding.filterTag.text.toString()
            )
            parentFragmentManager.setFragmentResult("filter_request", bundle)
            dismiss()
        }

        binding.buttonClearFilters.setOnClickListener {
            parentFragmentManager.setFragmentResult("filter_request", bundleOf())
            dismiss()
        }
    }*/
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.allAuthors.observe(viewLifecycleOwner) { authors ->
            val adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, authors)
            binding.filterAuthor.setAdapter(adapter)

            val maxDropdownHeight = (240 * resources.displayMetrics.density).toInt() // 240dp in pixels
            val fiveOrFewerItems = adapter.count <= 5

            if (fiveOrFewerItems) {
                // If 5 or fewer items, let the dropdown be exactly as tall as its content
                binding.filterAuthor.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                // If more than 5 items, set the fixed max height to enable scrolling
                binding.filterAuthor.dropDownHeight = maxDropdownHeight
            }
        }
        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tags)
            binding.filterTag.setAdapter(adapter)

            val maxDropdownHeight = (240 * resources.displayMetrics.density).toInt() // 240dp in pixels
            val fiveOrFewerItems = adapter.count <= 5

            if (fiveOrFewerItems) {
                // If 5 or fewer items, let the dropdown be exactly as tall as its content
                binding.filterTag.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                // If more than 5 items, set the fixed max height to enable scrolling
                binding.filterTag.dropDownHeight = maxDropdownHeight
            }
        }
        // Pre-populate all fields from the ViewModel's current state
        val currentState = viewModel.getCurrentQueryState()
        println("current $currentState")
        binding.filterAuthor.setText(currentState.author, false)
        binding.filterTag.setText(currentState.tag, false)


        val chipIdToCheck = when (currentState.format) {
            BookFormat.PAPERBACK -> R.id.chip_format_paperback
            BookFormat.EBOOK -> R.id.chip_format_ebook
            BookFormat.AUDIOBOOK -> R.id.chip_format_audiobook
            else -> R.id.chip_format_any
        }
        binding.filterChipGroupFormat.check(chipIdToCheck)

        when (currentState.sortBy) {
            SortBy.DATE_ADDED -> binding.radioGroupSortBy.check(R.id.radio_date_added)
            SortBy.TITLE -> binding.radioGroupSortBy.check(R.id.radio_title)
            SortBy.AUTHOR -> binding.radioGroupSortBy.check(R.id.radio_author)
            SortBy.RATING -> binding.radioGroupSortBy.check(R.id.radio_rating)
        }

        // Pre-check the correct "Order" radio button
        if (currentState.order == SortOrder.ASCENDING) {
            binding.radioGroupOrder.check(R.id.radio_ascending)
        } else {
            binding.radioGroupOrder.check(R.id.radio_descending)
        }

        binding.buttonApplyFilters.setOnClickListener {
            // Get values from all filter AND sort fields
            val author = binding.filterAuthor.text.toString()
            val tag = binding.filterTag.text.toString()
            val sortBy = when (binding.radioGroupSortBy.checkedRadioButtonId) {
                R.id.radio_title -> SortBy.TITLE
                R.id.radio_author -> SortBy.AUTHOR
                R.id.radio_rating -> SortBy.RATING
                R.id.radio_date_added -> SortBy.DATE_ADDED
                else -> SortBy.TITLE // Default case
            }
            val order = when (binding.radioGroupOrder.checkedRadioButtonId) {
                R.id.radio_ascending -> SortOrder.ASCENDING
                R.id.radio_descending -> SortOrder.DESCENDING
                else -> SortOrder.ASCENDING // Default case
            }
            val selectedFormat = when (binding.filterChipGroupFormat.checkedChipId) {
                R.id.chip_format_paperback -> BookFormat.PAPERBACK
                R.id.chip_format_ebook -> BookFormat.EBOOK
                R.id.chip_format_audiobook -> BookFormat.AUDIOBOOK
                else -> null // For "Any"
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
        // This line is the fix
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}