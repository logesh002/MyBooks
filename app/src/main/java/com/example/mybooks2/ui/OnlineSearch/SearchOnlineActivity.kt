package com.example.mybooks2.ui.OnlineSearch

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mybooks2.databinding.ActivitySearchOnlineBinding
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.bookapp.ui.AddBook2
import com.example.mybooks2.ui.addBook2.AddBook2ViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchOnlineActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchOnlineBinding
    private lateinit var searchAdapter: SearchOnlineAdapter
    private var searchJob: Job? = null

    val viewModel by viewModels<SearchOnlineViewModel> { SearchOnlineViewModel.Companion.factory }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchOnlineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

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

            binding.recyclerViewOnlineResults.updatePadding(bottom = imeInsets.bottom)
            insets
        }

        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }
    fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupRecyclerView() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        searchAdapter = SearchOnlineAdapter { bookDoc ->
            val intent = Intent(this, AddBook2::class.java).apply {
                putExtra("EXTRA_PREFILL_TITLE", bookDoc.title)
                putExtra("EXTRA_PREFILL_AUTHOR", bookDoc.authorName?.firstOrNull())
                putExtra("EXTRA_PREFILL_ISBN", bookDoc.isbn?.firstOrNull())
                putExtra("EXTRA_PREFILL_YEAR", bookDoc.firstPublishYear)
                putExtra("EXTRA_PREFILL_COVER_URL", bookDoc.getCoverUrl("L"))
                putExtra("EXTRA_PREFILL_PAGES", bookDoc.numberOfPages)
            }
            startActivity(intent)
            finish()
        }
        binding.recyclerViewOnlineResults.adapter = searchAdapter

        binding.recyclerViewOnlineResults.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if(newState == RecyclerView.SCROLL_STATE_DRAGGING){
                    imm.hideSoftInputFromWindow(binding.root.windowToken,0)
                }
            }
        })
    }

    private fun setupSearch() {
//        binding.editTextSearchOnline.addTextChangedListener { editable ->
//            searchJob?.cancel()
//            binding.textViewNoResults.visibility=View.GONE
//            searchJob = lifecycleScope.launch {
//                delay(500L)
//                viewModel.search(editable.toString())
//            }
//        }
        binding.editTextSearchOnline.setOnEditorActionListener { textView, actionId, _ ->
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

    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { results ->
            if(results.isEmpty()){
                binding.textViewNoResults.visibility=View.VISIBLE
            }
            else{
                binding.textViewNoResults.visibility=View.GONE
            }
            searchAdapter.submitList(results)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            binding.recyclerViewOnlineResults.visibility =if (isLoading) View.GONE else View.VISIBLE
        }
        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}