package com.example.mybooks2.ui.addBook

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.mybooks2.R
import com.example.mybooks2.databinding.ActivityAddBookBinding
import kotlin.getValue

class AddBook : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAddBookBinding

    val viewModel by viewModels<AddBookViewModel> { AddBookViewModel.Companion.factory }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
//        val window = window
//        val decorView = window.decorView
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val controller = decorView.windowInsetsController
//            if (controller != null) {
//                if (!isDarkTheme()) {
//                    controller.setSystemBarsAppearance(
//                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
//                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
//                    )
//                }
//            }
//        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val bookId = intent.getLongExtra("EXTRA_BOOK_ID", -1L)
        if (bookId != -1L) {
            viewModel.loadBook(bookId)
        }

        setupUI()
        observeViewModel()

    }
    private fun setupUI() {
        // Set up listeners to update the ViewModel when the user types
        binding.editTextTitle.addTextChangedListener {
            viewModel.title.value = it.toString()
        }
        binding.editTextAuthor.addTextChangedListener {
            viewModel.author.value = it.toString()
        }
        binding.totalPages.addOnEditTextAttachedListener {
            viewModel.totalPages.value = it.toString()
        }


        // The save button now just calls the ViewModel's save function
        binding.buttonToolbarSave.setOnClickListener { saveBook() }
        binding.buttonSave.setOnClickListener { saveBook() }
        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }
    private fun saveBook(){
        viewModel.saveBook()
    }

    private fun observeViewModel() {
        // Observe LiveData to update the UI
        viewModel.title.observe(this) { title ->
            // Prevent infinite loops from the text watcher
            if (binding.editTextTitle.text.toString() != title) {
                binding.editTextTitle.setText(title)
            }
        }
        viewModel.author.observe(this) { author ->
            if (binding.editTextAuthor.text.toString() != author) {
                binding.editTextAuthor.setText(author)
            }
        }
        viewModel.totalPages.observe(this) { totalPages ->

        }


        viewModel.titleError.observe(this) { errorMessage ->
            binding.inputLayoutTitle.error = errorMessage
        }
        viewModel.authorError.observe(this) { errorMessage ->
            binding.inputLayoutAuthor.error = errorMessage
        }
        viewModel.totalPagesError.observe(this) { errorMessage ->
            binding.totalPages.error = errorMessage
        }
        viewModel.dateError.observe(this) { errorMessage ->
            // Display this general error on one of the date fields
            binding.inputLayoutFinishDate.error = errorMessage
            if (errorMessage != null) {
                binding.inputLayoutStartDate.error = " " // Show indicator without text
            } else {
                binding.inputLayoutStartDate.error = null
            }
        }

        // Observe the save result to finish the activity
        viewModel.saveResult.observe(this) { savedBookId ->
            savedBookId?.let {
                val resultIntent = Intent()
                resultIntent.putExtra("EXTRA_SAVED_BOOK_ID", it)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }



}