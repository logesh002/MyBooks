package com.example.mybooks2.ui.searchView

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.example.mybooks2.databinding.ActivitySearchBinding
import com.example.mybooks2.ui.detailScreen.DetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    val viewModel by viewModels<SearchViewModel> { SearchViewModel.Companion.factory }
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var binding: ActivitySearchBinding

    private lateinit var detailLauncher: ActivityResultLauncher<Intent>


    private var searchJob: Job? = null
    private lateinit var searchResultsRecyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        searchResultsRecyclerView = binding.recyclerViewSearchResults


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                left = systemBarInsets.left,
                top = systemBarInsets.top,
                right = systemBarInsets.right
            )

            binding.recyclerViewSearchResults.updatePadding(bottom = imeInsets.bottom)

            insets
        }

        val window = window
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = decorView.windowInsetsController
            if (controller != null) {
                if (!isDarkTheme()) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        }

        detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val currentQuery = binding.editTextSearch.text.toString()
            if (currentQuery.isNotBlank()) {
                viewModel.search(currentQuery, force = true)
            }
        }

        setupRecyclerView()
        setupSearch()
        observeViewModel()
        binding.editTextSearch.requestFocus()
        showKeyboard(binding.editTextSearch)
    }

    override fun onResume() {
        super.onResume()

    }
    fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupSearch() {
        binding.editTextSearch.addTextChangedListener { editable ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {

                delay(300L)
                viewModel.search(editable.toString())
            }
        }
        binding.editTextSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchJob?.cancel()
                viewModel.search(textView.text.toString())

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(textView.windowToken, 0)
                textView.clearFocus()

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }
    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setupRecyclerView() {
    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        searchAdapter = SearchResultAdapter { book ->

            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("EXTRA_BOOK_ID", book.id)
            }
            detailLauncher.launch(intent)
        }
        searchResultsRecyclerView.adapter = searchAdapter
    searchResultsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState == RecyclerView.SCROLL_STATE_DRAGGING){
                imm.hideSoftInputFromWindow(binding.root.windowToken,0)
            }
        }
    })
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { results ->
//            searchAdapter.submitList(results)
//            if(results.isEmpty()){
//                binding.textViewNoResults.visibility=View.VISIBLE
//                binding.recyclerViewSearchResults.visibility=View.GONE
//            }
//            else{
//                binding.textViewNoResults.visibility=View.GONE
//                binding.recyclerViewSearchResults.visibility=View.VISIBLE
//            }

            if(results.isEmpty()){
                binding.recyclerViewSearchResults.visibility = View.GONE
                binding.textViewNoResults.visibility = View.VISIBLE
                searchAdapter.submitList(results)
            } else {
                binding.textViewNoResults.visibility = View.GONE

                binding.recyclerViewSearchResults.itemAnimator = null
                binding.recyclerViewSearchResults.visibility = View.VISIBLE

                searchAdapter.submitList(results) {
                    binding.recyclerViewSearchResults.itemAnimator = DefaultItemAnimator()
                }
            }
        }
    }
}