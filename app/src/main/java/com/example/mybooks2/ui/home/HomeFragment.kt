package com.example.mybooks2.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mybooks2.ui.addBook2.AddBook2
import com.example.mybooks2.R
import com.example.mybooks2.databinding.FragmentHomeBinding
import com.example.mybooks2.ui.onlineSearch.SearchOnlineActivity
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.detailScreen.DetailActivity
import com.example.mybooks2.ui.home.dialog.AddBookOptionsBottomSheet
import com.example.mybooks2.ui.home.dialog.DeleteConfirmationDialogFragment
import com.example.mybooks2.ui.home.dialog.FilterBottomSheetFragment
import com.example.mybooks2.ui.home.util.LayoutMode
import com.example.mybooks2.ui.home.util.SortBy
import com.example.mybooks2.ui.home.util.SortOrder
import com.example.mybooks2.ui.searchView.SearchActivity
import com.example.mybooks2.ui.setting.SettingsActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val title = data?.getStringExtra("EXTRA_TITLE")
                val author = data?.getStringExtra("EXTRA_AUTHOR")
                val bookId = data?.getLongExtra("EXTRA_SAVED_BOOK_ID", -1L)

                if(bookId != -1L){
                    val intent1 = Intent(requireActivity(), DetailActivity::class.java).apply {
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
            AddBookOptionsBottomSheet().show(parentFragmentManager, AddBookOptionsBottomSheet.TAG)
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



        parentFragmentManager.setFragmentResultListener("query_request", this) { _, bundle ->
            if (bundle.isEmpty) {
                viewModel.updateQuery { currentState ->
                    currentState.copy(
                        author = null,
                        tags = emptySet(),
                        tagMatchMode = TagMatchMode.ANY,
                        format = null,
                        sortBy = SortBy.DATE_ADDED,
                        order = SortOrder.DESCENDING
                    )
                }
            } else {
                val author = bundle.getString("KEY_AUTHOR")
                val tag = bundle.getStringArrayList("KEY_TAGS")
                val format = bundle.getSerializable("KEY_FORMAT") as? BookFormat
                val sortBy = bundle.getSerializable("KEY_SORT_BY") as? SortBy
                val order = bundle.getSerializable("KEY_ORDER") as? SortOrder
                val tagMatchMode = (bundle.getSerializable("KEY_TAG_MATCH_MODE") as? TagMatchMode) ?: TagMatchMode.ANY

                viewModel.updateQuery { currentState ->
                    currentState.copy(
                        author = if (author.isNullOrBlank()) null else author,
                        tags = tag?.toSet()?:emptySet(),
                        format = format,
                        sortBy = sortBy ?: currentState.sortBy,
                        order = order ?: currentState.order,
                        tagMatchMode = tagMatchMode
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
        parentFragmentManager.setFragmentResultListener("add_book_request", this) { _, bundle ->
            when (bundle.getString("option")) {
                "MANUAL" -> {
                    val intent = Intent(requireActivity(), AddBook2::class.java)
                    addBookLauncher.launch(intent)
                }
                "ONLINE" -> {
                    val intent = Intent(requireActivity(), SearchOnlineActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.myToolbar)
        setHasOptionsMenu(true)
    }
    private fun setupFilterChips() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultOrder = resources.getStringArray(R.array.status_values)
        val savedOrderStr = prefs.getString("chip_order_preference", defaultOrder.joinToString(","))
        val statusOrder = savedOrderStr!!.split(",")

        Log.d("order",savedOrderStr)
        binding.chipGroupFilter.removeAllViews()

        val statusTextMap = mapOf(
            "IN_PROGRESS" to "In progress",
            "FINISHED" to "Finished",
            "FOR_LATER" to "To be read",
            "UNFINISHED" to "Dropped"
        )
        val statusIdMap = mapOf(
            "IN_PROGRESS" to R.id.chip_in_progress,
            "FINISHED" to R.id.chip_finished,
            "FOR_LATER" to R.id.chip_to_be_read,
            "UNFINISHED" to R.id.chip_dropped
        )
        val inflater = LayoutInflater.from(requireContext())


        statusOrder.forEach { statusValue ->
            val chip = inflater.inflate(R.layout.chip_filter, binding.chipGroupFilter, false) as Chip
            chip.id = statusIdMap[statusValue] ?: View.generateViewId()
            chip.text = statusTextMap[statusValue]
            binding.chipGroupFilter.addView(chip)
        }

        val firstChipId = if (statusOrder.isNotEmpty()) statusIdMap[statusOrder.first()] else null
        val scroll = binding.horizontalScroll

        firstChipId?.let {
            binding.chipGroupFilter.check(it)
        }

        scroll.post {
            scroll.smoothScrollTo(0, 0)
        }
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedChipId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            val filter = when (selectedChipId) {
                R.id.chip_in_progress -> "In progress"
                R.id.chip_finished -> "Finished"
                R.id.chip_to_be_read -> "To be read"
                else -> "Dropped"
            }
            viewModel.updateQuery { currentState ->
                currentState.copy(status = filter)
            }

            group.post {
                val isScrollable = scroll.width < group.width

                if (isScrollable) {
                    if (selectedChipId == firstChipId) {
                        scroll.smoothScrollTo(0, 0)
                    } else {
                        scroll.smoothScrollTo(group.width, 0)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter ({ book ->
            val intent1 = Intent(requireActivity(), DetailActivity::class.java).apply {
                putExtra("EXTRA_BOOK_ID", book.id)
            }
            detailBookLauncher.launch(intent1)
        },
            onItemLongClicked = { book -> viewModel.toggleSelection(book.id) }

        )

        binding.recyclerViewBooks.adapter = bookAdapter
    }

    private fun observeViewModel() {

        viewModel.booksToShow.observe(viewLifecycleOwner) { books ->

            books?.let {
                bookAdapter.submitList(it)
            }
        }
        viewModel.layoutMode.observe(viewLifecycleOwner) { mode ->
            updateLayoutIcon(mode)
            switchLayoutManager(mode)
        }
        viewModel.isSelectionModeActive.observe(viewLifecycleOwner) { isActive ->
            bookAdapter.isSelectionModeActive = isActive
            onBackPressedCallback.isEnabled = isActive
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.selectedItems.observe(viewLifecycleOwner) { selectedIds ->
            bookAdapter.setSelectedIds(selectedIds)
            val toolbar = (requireActivity() as AppCompatActivity).supportActionBar
            if (selectedIds.isNotEmpty()) {
                toolbar?.title = "${selectedIds.size} selected"
            } else {
                toolbar?.title = getString(R.string.app_name)
            }
        }
        viewModel.chipOrderChangedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                setupFilterChips()
            }
        }

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val isSelectionActive = viewModel.isSelectionModeActive.value ?: false

        menu.findItem(R.id.search).isVisible = !isSelectionActive
        menu.findItem(R.id.action_filter).isVisible = !isSelectionActive
        menu.findItem(R.id.layout_pref).isVisible = !isSelectionActive
        menu.findItem(R.id.settings).isVisible=!isSelectionActive

        menu.findItem(R.id.action_delete).isVisible = isSelectionActive

        val toolbar = (requireActivity() as AppCompatActivity).supportActionBar
        if (isSelectionActive) {
            toolbar?.setDisplayHomeAsUpEnabled(true)
            toolbar?.setHomeAsUpIndicator(R.drawable.outline_close_24)
        } else {
            toolbar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun switchLayoutManager(mode: LayoutMode) {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val desiredItemWidthDp = 160

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
        bookAdapter.setLayoutMode(mode)
    }

    private fun updateLayoutIcon(mode: LayoutMode?) {
        val iconRes = when (mode) {
            LayoutMode.GRID -> R.drawable.outline_list_24
            LayoutMode.LIST -> R.drawable.outline_grid_view_24
            else -> R.drawable.outline_list_24
        }
        layoutToggleMenuItem?.setIcon(iconRes)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.action_bar_menu_1, menu)
        layoutToggleMenuItem = menu.findItem(R.id.layout_pref)
        updateLayoutIcon(viewModel.layoutMode.value)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.layout_pref -> {
                viewModel.toggleLayoutMode()
                true
            }

            R.id.search ->{
                startActivity(Intent(requireContext(), SearchActivity::class.java))
                true
            }
            android.R.id.home -> {
                if (viewModel.isSelectionModeActive.value == true) {
                    viewModel.clearSelections()
                }
                true
            }
            R.id.action_delete -> {
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
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

