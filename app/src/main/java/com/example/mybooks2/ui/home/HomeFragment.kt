package com.example.mybooks2.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookapp.ui.AddBook2
import com.example.mybooks2.R
import com.example.mybooks2.databinding.FragmentHomeBinding
import com.example.mybooks2.ui.OnlineSearch.SearchOnlineActivity
import com.example.mybooks2.ui.addBook.AddBook
import com.example.mybooks2.ui.addBook2.AddBook2ViewModel
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.detailScreen.DetailActivity
import com.example.mybooks2.ui.searchView.SearchActivity
import com.example.mybooks2.ui.setting.SettingsActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlin.getValue


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var bookAdapter: BookAdapter
    private lateinit var editBookLauncher: ActivityResultLauncher<Intent>

    private lateinit var fab: FloatingActionButton
    private val viewModel: HomeViewModel by activityViewModels { HomeViewModel.factory }
    private val binding get() = _binding!!

    lateinit var chipGroup: ChipGroup
    private lateinit var addBookLauncher: ActivityResultLauncher<Intent>

    private var layoutToggleMenuItem: MenuItem? = null

    private lateinit var detailBookLauncher: ActivityResultLauncher<Intent>

    private lateinit var onBackPressedCallback: OnBackPressedCallback


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        fab = binding.fabAddBook
        addBookLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // This is where you handle the result from AddBookActivity
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val title = data?.getStringExtra("EXTRA_TITLE")
                val author = data?.getStringExtra("EXTRA_AUTHOR")
                val bookId = data?.getLongExtra("EXTRA_SAVED_BOOK_ID", -1L)

                if(bookId != -1L){
                    val intent1 = Intent(requireActivity(), DetailActivity::class.java).apply {
                        // Pass the book's ID to the activity for "edit" mode
                        putExtra("EXTRA_BOOK_ID",bookId)
                    }
                    detailBookLauncher.launch(intent1)
                }

                if (title != null && author != null) {
                    Snackbar.make(requireView(), "Book Saved: $title", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        editBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val title = data?.getStringExtra("EXTRA_TITLE")
                val author = data?.getStringExtra("EXTRA_AUTHOR")

                if (title != null && author != null) {
                    Snackbar.make(requireView(), "Book Saved: $title", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        detailBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if (result.resultCode == Activity.RESULT_OK) {

            }
        }

        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        fab.setOnClickListener {
//            val intent = Intent(requireActivity(), AddBook2::class.java)
//            addBookLauncher.launch(intent)
            showAddBookOptionsDialog()
        }

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {

                viewModel.clearSelections()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        setupToolbar()
        setupFilterChips()
        setupRecyclerView()
        observeViewModel()

//        parentFragmentManager.setFragmentResultListener("filter_request", this) { _, bundle ->
//            val author = bundle.getString("KEY_AUTHOR")
//            val tag = bundle.getString("KEY_TAG")
//            viewModel.applyAdvancedFilters(author, tag)
//        }

//        parentFragmentManager.setFragmentResultListener("query_request", this) { _, bundle ->
//            if (bundle.isEmpty) {
//                println("tet")
//                viewModel.clearAdvancedFilters()
//            }
//            else {
//                val author = bundle.getString("KEY_AUTHOR")
//                val tag = bundle.getString("KEY_TAG")
//                val sortBy = bundle.getSerializable("KEY_SORT_BY") as? SortBy
//                val order = bundle.getSerializable("KEY_ORDER") as? SortOrder
//                val format = bundle.getSerializable("KEY_FORMAT") as? BookFormat
//
//                viewModel.updateQuery(author = author, tag = tag, sortBy = sortBy, order = order, format = format)
//            }
//        }


        // In HomeFragment's onViewCreated()
        parentFragmentManager.setFragmentResultListener("query_request", this) { _, bundle ->
            if (bundle.isEmpty) {
                // --- This is the "Clear Filters" logic ---
                viewModel.updateQuery { currentState ->
                    currentState.copy(
                        author = null,
                        tag = null,
                        format = null,
                        sortBy = SortBy.DATE_ADDED,
                        order = SortOrder.DESCENDING
                    )
                }
            } else {
                // --- This is the "Apply" logic ---
                val author = bundle.getString("KEY_AUTHOR")
                val tag = bundle.getString("KEY_TAG")
                val format = bundle.getSerializable("KEY_FORMAT") as? BookFormat
                val sortBy = bundle.getSerializable("KEY_SORT_BY") as? SortBy
                val order = bundle.getSerializable("KEY_ORDER") as? SortOrder

                viewModel.updateQuery { currentState ->
                    currentState.copy(
                        author = if (author.isNullOrBlank()) null else author,
                        tag = if (tag.isNullOrBlank()) null else tag,
                        format = format, // The 'null' from the "Any" chip is now applied correctly
                        sortBy = sortBy ?: currentState.sortBy,
                        order = order ?: currentState.order
                    )
                }
            }
        }
        parentFragmentManager.setFragmentResultListener("delete_request", this) { _, bundle ->
            val confirmed = bundle.getBoolean("result")
            if (confirmed) {
                viewModel.deleteSelectedItems()
            }
        }
    }

    private fun showAddBookOptionsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add a new book")
            .setItems(R.array.add_book_options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Option 0: "Add Manually"
                        val intent = Intent(requireActivity(), AddBook2::class.java)
                        addBookLauncher.launch(intent)
                    }
                    1 -> {
                        // Option 1: "Search Online"
                        // TODO: Create a new SearchOnlineActivity
                        val intent = Intent(requireActivity(), SearchOnlineActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }
    private fun setupToolbar() {
        // Required to have the fragment manage the menu
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.myToolbar)
        setHasOptionsMenu(true)
    }
    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            // checkedIds is a list, but we only have one selection
            val selectedChipId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            val scroll = binding.horizontalScroll
            val filter = when (selectedChipId) {
                R.id.chip_in_progress -> "In progress"
                R.id.chip_finished -> "Finished"
                R.id.chip_to_be_read -> "To be read"
                else -> "Dropped"
            }
           //  viewModel.setFilter(filter)
            viewModel.updateQuery { currentState ->
                currentState.copy(status = filter)
            }

            group.post {
                // 1. Check if the ChipGroup is wider than the ScrollView (i.e., if it's scrollable)
                val isScrollable = scroll.width < group.width

                println("is scrollable $isScrollable")
                if (isScrollable) {
                    // 2. Check if the first chip ("All") was the one selected
                    if (selectedChipId == R.id.chip_in_progress) {
                        println("tets scroll")
                        // Scroll to the far left
                        scroll.smoothScrollTo(0, 0)
                    } else {
                        // For any other chip, scroll to the far right
                        scroll.smoothScrollTo(group.width, 0)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter ({ book ->
            // This is the code that runs when a book is clicked
            val intent = Intent(requireActivity(), AddBook2::class.java).apply {
                // Pass the book's ID to the activity for "edit" mode
                putExtra("EXTRA_BOOK_ID", book.id)
            }
            val intent1 = Intent(requireActivity(), DetailActivity::class.java).apply {
                // Pass the book's ID to the activity for "edit" mode
                putExtra("EXTRA_BOOK_ID", book.id)
            }
            editBookLauncher.launch(intent1)
        },
            onItemLongClicked = { book -> viewModel.toggleSelection(book.id) }

        )

        binding.recyclerViewBooks.adapter = bookAdapter
    }

    private fun observeViewModel() {
//        viewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
//            // Submit the new list to the adapter. DiffUtil will handle the animations.
//            books?.let {
//                bookAdapter.submitList(it)
//            }
//        }
        viewModel.booksToShow.observe(viewLifecycleOwner) { books ->
            // Submit the final, filtered, and sorted list to the adapter.
            // DiffUtil will handle the animations.
            books?.let {
                bookAdapter.submitList(it)
            }
        }
        viewModel.layoutMode.observe(viewLifecycleOwner) { mode ->
            updateLayoutIcon(mode)
            switchLayoutManager(mode)
        }
        viewModel.isSelectionModeActive.observe(viewLifecycleOwner) { isActive ->
            // Redraw the menu whenever selection mode changes
            bookAdapter.isSelectionModeActive = isActive
            onBackPressedCallback.isEnabled = isActive
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.selectedItems.observe(viewLifecycleOwner) { selectedIds ->
            // Update the adapter with the new set of selected IDs
            bookAdapter.setSelectedIds(selectedIds)
            // Update the toolbar title with the selection count
            val toolbar = (requireActivity() as AppCompatActivity).supportActionBar
            if (selectedIds.isNotEmpty()) {
                toolbar?.title = "${selectedIds.size} selected"
            } else {
                toolbar?.title = getString(R.string.app_name)
            }
        }

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val isSelectionActive = viewModel.isSelectionModeActive.value ?: false

        // Show/hide menu items based on selection mode
        menu.findItem(R.id.search).isVisible = !isSelectionActive
        menu.findItem(R.id.action_filter).isVisible = !isSelectionActive
        menu.findItem(R.id.layout_pref).isVisible = !isSelectionActive
        menu.findItem(R.id.settings).isVisible=!isSelectionActive

        // Add a delete item to your home_menu.xml and control its visibility
        menu.findItem(R.id.action_delete).isVisible = isSelectionActive

        val toolbar = (requireActivity() as AppCompatActivity).supportActionBar
        if (isSelectionActive) {
            toolbar?.setDisplayHomeAsUpEnabled(true)
            toolbar?.setHomeAsUpIndicator(R.drawable.outline_close_24) // Close icon
        } else {
            toolbar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun switchLayoutManager(mode: LayoutMode) {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val desiredItemWidthDp = 160 // The width you want each grid item to be

        val spanCount = (screenWidthDp / desiredItemWidthDp).toInt()

        binding.recyclerViewBooks.layoutManager = when (mode) {
            LayoutMode.GRID -> GridLayoutManager(requireContext(),  spanCount.coerceAtLeast(2))
            LayoutMode.LIST -> LinearLayoutManager(requireContext())
        }
        if (mode == LayoutMode.GRID) {
            binding.recyclerViewBooks.layoutManager = GridLayoutManager(requireContext(), spanCount.coerceAtLeast(2))
        } else {
            binding.recyclerViewBooks.layoutManager = LinearLayoutManager(requireContext())
        }
        // Tell the adapter to use the new layout mode
        bookAdapter.setLayoutMode(mode)
    }

    private fun updateLayoutIcon(mode: LayoutMode?) {
        val iconRes = when (mode) {
            LayoutMode.GRID -> R.drawable.outline_list_24 // If current mode is Grid, show the List icon
            LayoutMode.LIST -> R.drawable.outline_grid_view_24  // If current mode is List, show the Grid icon
            else -> R.drawable.outline_list_24
        }
        layoutToggleMenuItem?.setIcon(iconRes)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
       // inflater.inflate(R.menu.action_bar_menu_1, menu)
        inflater.inflate(R.menu.action_bar_menu_1, menu)
        // Get a reference to the menu item
        layoutToggleMenuItem = menu.findItem(R.id.layout_pref)
        // Set the initial icon state based on the ViewModel's current value
        updateLayoutIcon(viewModel.layoutMode.value)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.layout_pref -> {
                println("test")
                // Tell the ViewModel to toggle the mode
                viewModel.toggleLayoutMode()
                true
            }
//            R.id.sort -> {
//                showSortDialog()
//                true
//            }
            R.id.search ->{
                startActivity(Intent(requireContext(), SearchActivity::class.java))
                true
            }
            android.R.id.home -> { // Handles the toolbar's navigation icon click
                if (viewModel.isSelectionModeActive.value == true) {
                    viewModel.clearSelections() // If in selection mode, the icon is "close"
                }
                true
            }
            R.id.action_delete -> {
               // viewModel.deleteSelectedItems()
                //showDeleteConfirmationDialog()
                val selectedCount = viewModel.selectedItems.value?.size ?: 0
                if (selectedCount > 0) {
                    DeleteConfirmationDialogFragment.newInstance(selectedCount)
                        .show(parentFragmentManager, DeleteConfirmationDialogFragment.TAG)
                }
                true
            }
            R.id.settings -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
                true
            }
            R.id.action_filter -> {
                FilterBottomSheetFragment().show(parentFragmentManager, "FilterBottomSheet")
                true
            }
            else -> {
                println(item)
                super.onOptionsItemSelected(item)
            }
        }
    }
    private fun showDeleteConfirmationDialog() {
        val selectedCount = viewModel.selectedItems.value?.size ?: 0
        if (selectedCount == 0) return

        // Use the plural string resource for a dynamic title
        val title = resources.getQuantityString(R.plurals.delete_dialog_title, selectedCount, selectedCount)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage("Are you sure you want to permanently delete these books?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSelectedItems()
            }
            .show()
    }

    private fun showSortDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort, null)
        val sortByGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_sort_by)
        val orderGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_order)

        // Pre-select the current sort options
        val currentSortState = viewModel.getCurrentSortState()
        when (currentSortState.sortBy) {
            SortBy.DATE_ADDED -> sortByGroup.check(R.id.radio_date_added)
            SortBy.TITLE -> sortByGroup.check(R.id.radio_title)
            SortBy.AUTHOR -> sortByGroup.check(R.id.radio_author)
            SortBy.RATING -> sortByGroup.check(R.id.radio_rating)
        }
        if (currentSortState.order == SortOrder.ASCENDING) {
            orderGroup.check(R.id.radio_ascending)
        } else {
            orderGroup.check(R.id.radio_descending)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Books")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply") { dialog, _ ->
                val newSortBy = when (sortByGroup.checkedRadioButtonId) {
                    R.id.radio_title -> SortBy.TITLE
                    R.id.radio_author -> SortBy.AUTHOR
                    R.id.radio_rating -> SortBy.RATING
                    else -> SortBy.DATE_ADDED
                }
                val newOrder = if (orderGroup.checkedRadioButtonId == R.id.radio_ascending) {
                    SortOrder.ASCENDING
                } else {
                    SortOrder.DESCENDING
                }
                viewModel.applySort(newSortBy, newOrder)
            }
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
class DemoCollectionPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getCount(): Int  = 4

    override fun getItem(i: Int): Fragment {
        val fragment = DemoObjectFragment()
        fragment.arguments = Bundle().apply {
            // Our object is just an integer :-P
            putInt(ARG_OBJECT, i + 1)
        }
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence {
        return "OBJECT ${(position + 1)}"
    }
}


private const val ARG_OBJECT = "object"

// Instances of this class are fragments representing a single
// object in the collection.
class DemoObjectFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tab1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            val textView: TextView = view.findViewById(R.id.text1)
            textView.text = getInt(ARG_OBJECT).toString()
        }
    }
}