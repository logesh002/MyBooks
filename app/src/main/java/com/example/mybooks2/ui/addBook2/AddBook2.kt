package com.example.bookapp.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.mybooks2.R
import com.example.mybooks2.databinding.AddBook2Binding
import com.example.mybooks2.ui.addBook2.AddBook2ViewModel
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.addBook2.DiscardChangesDialogFragment
import com.example.mybooks2.ui.addBook2.ReadingStatus
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AddBook2 : AppCompatActivity() {

    private lateinit var binding: AddBook2Binding
    val viewModel by viewModels<AddBook2ViewModel> { AddBook2ViewModel.Companion.factory }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateCoverImage(it)
          //  binding.imageViewPreview.setImageURI(it)
            binding.imageViewPreview.visibility = View.VISIBLE
            binding.imageCl.visibility=View.VISIBLE
            binding.coverImageCard.visibility = View.GONE
        }
    }


   // private val stars = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
       // enableEdgeToEdge()
        binding = AddBook2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val window = window
        val decorView = window.decorView

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Get the height of the keyboard
//            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
//
//            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
//            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
//
//            binding.scrollView.updatePadding(bottom = imeHeight)


            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.displayCutout
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            cutout?.let {
                if(isLandscape())
                    view.setPadding(it.safeInsetLeft, systemBars.top, it.safeInsetRight, systemBars.bottom)
                else{
                    println("No landscape")
                }
            }
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            val keyboardHeight = if (isImeVisible) {
                imeBottom - navBarBottom
            } else {
                0
            }
            view.updatePadding(bottom = keyboardHeight)


            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBarInsets.left,
                systemBarInsets.top, // Use status bar top inset
                systemBarInsets.right,
                imeInsets.bottom // Use IME bottom inset (keyboard)
            )

            WindowInsetsCompat.CONSUMED
        }
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

        setupToolbar()
        val bookId = intent.getLongExtra("EXTRA_BOOK_ID", -1L)

        if (bookId != -1L) {
            // EDIT MODE
            supportActionBar?.title = "Edit Book"
            viewModel.loadBook(bookId)
        } else {

            val prefillTitle = intent.getStringExtra("EXTRA_PREFILL_TITLE")
            if (prefillTitle != null) {
                // Pre-fill mode
                val prefillAuthor = intent.getStringExtra("EXTRA_PREFILL_AUTHOR")
                val prefillIsbn = intent.getStringExtra("EXTRA_PREFILL_ISBN")
                val prefillYear = intent.getIntExtra("EXTRA_PREFILL_YEAR", 0)
                val prefillCover = intent.getStringExtra("EXTRA_PREFILL_COVER_URL")

                viewModel.prefillData(prefillTitle, prefillAuthor, prefillIsbn, prefillYear,prefillCover)
            }
            // ADD MODE
            supportActionBar?.title = "Add Book"
            // The ViewModel is already initialized with an empty state, so we do nothing.
        }
        setupViews()
        setupObservers()
        setupTextWatchers()
        setupTags()
        setupFormatDropdown()

        binding.toolbar.setNavigationOnClickListener {
            handleCloseAttempt()
        }

        // Handle the system's back button/gesture
        onBackPressedDispatcher.addCallback(this) {
            handleCloseAttempt()
        }

        supportFragmentManager.setFragmentResultListener(DiscardChangesDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val confirmed = bundle.getBoolean(DiscardChangesDialogFragment.RESULT_KEY)
            if (confirmed) {
                // The user tapped "Discard" in the dialog, so now we close the activity.
                finish()
            }
        }

    }

    private fun scrollToShowView(scrollView: View, viewToScrollTo: View) {
        val scrollRect = Rect()
        scrollView.getHitRect(scrollRect)

        if (!viewToScrollTo.getLocalVisibleRect(scrollRect)) {
            (scrollView as? NestedScrollView)?.smoothScrollTo(0, viewToScrollTo.bottom)
        }
    }

    private fun setupFormatDropdown() {
        val formats = BookFormat.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, formats)
        binding.autoCompleteFormat.setAdapter(adapter)

        binding.autoCompleteFormat.setOnItemClickListener { _, _, position, _ ->
            val selectedFormat = BookFormat.entries[position]
            viewModel.updateFormat(selectedFormat)
        }
    }
    private fun handleCloseAttempt() {
        if (viewModel.hasUnsavedChanges()) {
            showDiscardChangesDialog()
        } else {
            finish() // No changes, so close immediately
        }
    }

    private fun isLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

//    private fun showDiscardChangesDialog() {
//        MaterialAlertDialogBuilder(this)
//            .setTitle("Discard changes?")
//            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
//            .setNegativeButton("Cancel") { dialog, which ->
//                // Do nothing, just close the dialog
//            }
//            .setPositiveButton("Discard") { dialog, which ->
//                // User confirmed, close the activity
//                finish()
//            }
//            .show()
//    }
    private fun showDiscardChangesDialog() {
        // Show the DialogFragment instead of building the dialog directly.
        DiscardChangesDialogFragment.newInstance()
            .show(supportFragmentManager, DiscardChangesDialogFragment.TAG)
    }

    private fun setupTags() {
        // 1. Set up the AutoComplete suggestions
        viewModel.allTags.observe(this) { tags ->
            println("tags sd"+tags)
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tags)
            binding.autoCompleteTags.setAdapter(adapter)
        }

        // 2. Add tag when user selects from dropdown or presses Enter
        binding.autoCompleteTags.setOnItemClickListener { _, _, _, _ ->
            addTagFromInput()
        }
        binding.autoCompleteTags.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTagFromInput()
                return@setOnEditorActionListener true
            }
            false
        }

        // 3. Observe the current book's tags and update the ChipGroup
//        viewModel.currentBookTags.observe(this) { tags ->
//            updateTagsChipGroup(tags.toSet())
//        }
    }

    private fun addTagFromInput() {
        val newTag = binding.autoCompleteTags.text.toString()
        viewModel.addTag(newTag)
        binding.autoCompleteTags.setText("") // Clear the input
    }

//    private fun updateTagsChipGroup(tags: Set<kotlin.String>) {
//        binding.chipGroupTags.removeAllViews()
//        for (tag in tags) {
//            val chip = Chip(this)
//            chip.text = tag
//            chip.isCloseIconVisible = true
//            chip.setOnCloseIconClickListener {
//                viewModel.removeTag(tag.toString())
//            }
//            binding.chipGroupTags.addView(chip)
//        }
//    }
    private fun updateTagsChipGroup(tags: Set<String>) {
        // To prevent an infinite loop, check if the UI is already up-to-date
        val currentChips = (0 until binding.chipGroupTags.childCount).map {
            (binding.chipGroupTags.getChildAt(it) as Chip).text.toString()
        }.toSet()

        if (currentChips != tags) {
            binding.chipGroupTags.removeAllViews()
            tags.forEach { tagName ->
                val chip = Chip(this).apply {
                    text = tagName
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        viewModel.removeTag(tagName)
                    }
                }
                binding.chipGroupTags.addView(chip)
            }
        }
    }
    fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupViews() {
        // Cover Image
        binding.coverImageCard.setOnClickListener {
            if(binding.imageCl.isGone)
            pickImageLauncher.launch("image/*")
        }
        binding.fabDeleteImage.setOnClickListener {
            viewModel.updateCoverImage(null)
            binding.imageCl.visibility=View.GONE
            binding.imageViewPreview.visibility = View.GONE
            binding.coverImageCard.visibility=View.VISIBLE
        }

        // Status Chips
        binding.statusChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val status = when (checkedIds[0]) {
                    R.id.finishedChip -> ReadingStatus.FINISHED
                    R.id.inProgressChip -> ReadingStatus.IN_PROGRESS
                    R.id.forLaterChip -> ReadingStatus.FOR_LATER
                    R.id.unfinishedChip -> ReadingStatus.UNFINISHED
                    else -> ReadingStatus.FINISHED
                }
                if(checkedIds[0] == R.id.finishedChip){
                    binding.durationTextView.visibility= View.VISIBLE
                    binding.durationLl.visibility=View.VISIBLE
                    binding.ratingCard.visibility=View.VISIBLE
                    binding.reviewInputLayout.visibility=View.VISIBLE

                }
                else{
                    binding.durationTextView.visibility= View.GONE
                    binding.durationLl.visibility=View.GONE
                    binding.ratingCard.visibility=View.GONE
                    binding.reviewInputLayout.visibility=View.GONE
                }
                viewModel.updateStatus(status)
            }
        }

        binding.editTextStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }

        binding.editTextFinishDate.setOnClickListener {
            showDatePicker(isStartDate = false)
        }

        binding.cancelButton.setOnClickListener {
            handleCloseAttempt()
        }

        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.validateAndSave()
            }
        }
    }
    fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
    private fun showDatePicker(isStartDate: Boolean) {

        hideKeyboard()

        val title = if (isStartDate) "Select Start Date" else "Select Finish Date"

        val dateValidator = DateValidatorPointBackward.now()

        val constraints = CalendarConstraints.Builder()
            .setValidator(dateValidator)
            .build()
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateInMillis ->
            if (isStartDate) {
                binding.editTextStartDate.setText(dateFormat.format(Date(selectedDateInMillis)))
                viewModel.updateStartDate( selectedDateInMillis)
            } else {
                binding.editTextFinishDate.setText(dateFormat.format(Date(selectedDateInMillis)))
                viewModel.updateEndDate(selectedDateInMillis)
            }
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER_TAG")
    }

    private fun setupTextWatchers() {
        binding.titleEditText.doAfterTextChanged { text ->
            text?.length?.let {
                binding.titleInputLayout.isCounterEnabled = it>= 249
            }
            viewModel.updateTitle(text.toString())
        }

        binding.subtitleEditText.doAfterTextChanged { text ->
            viewModel.updateSubtitle(text.toString())
        }

        binding.authorEditText.doAfterTextChanged { text ->
            text?.length?.let {
                binding.authorInputLayout.isCounterEnabled = it>= 149
            }
            viewModel.updateAuthor(text.toString())
        }

        binding.pagesEditText.doAfterTextChanged { text ->
            viewModel.updateNumberOfPages(text.toString())
        }

        binding.yearEditText.doAfterTextChanged { text ->
            viewModel.updatePublicationYear(text.toString())
        }

        binding.descriptionEditText.doAfterTextChanged { text ->
            viewModel.updateDescription(text.toString())
        }

        binding.isbnEditText.doAfterTextChanged { text ->
            viewModel.updateIsbn(text.toString())
        }


        binding.reviewEditText.doAfterTextChanged { text ->
            viewModel.updateReview(text.toString())
        }

        binding.notesEditText.doAfterTextChanged { text ->
            viewModel.updateNotes(text.toString())
        }
        binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, b ->
           viewModel.updateRating(rating)
        }
    }

/*
    private fun setupObservers() {
        viewModel.bookFormState.observe(this) { state ->

            if (state.coverImageUri != null) {
                binding.coverImageCard.visibility = View.GONE
                binding.imageCl.visibility = View.VISIBLE

                binding.imageViewPreview.load(state.coverImageUri) {
                    crossfade(true)
                    placeholder(R.drawable.outline_image_24) // Optional
                    error(R.drawable.outline_filter_list_24) // Optional
                }
            } else {
                binding.coverImageCard.visibility = View.VISIBLE
            }

            // Update Title
            if (binding.titleEditText.text.toString() != state.title) {
                binding.titleEditText.setText(state.title)
            }

            // Update Author
            if (binding.authorEditText.text.toString() != state.author) {
                binding.authorEditText.setText(state.author)
            }

            // Update Status
            val buttonIdToCheck = when (state.status) {
                ReadingStatus.FINISHED -> R.id.finishedChip
                ReadingStatus.IN_PROGRESS -> R.id.inProgressChip
                ReadingStatus.FOR_LATER -> R.id.forLaterChip
                ReadingStatus.UNFINISHED -> R.id.unfinishedChip
            }
            if (binding.toggleButtonStatus.checkedButtonId != buttonIdToCheck) {
                binding.toggleButtonStatus.check(buttonIdToCheck)
            }

            // Update Rating
            if (binding.ratingBar.rating != state.rating) {
                binding.ratingBar.rating = state.rating
            }
        }

        viewModel.validationError.observe(this) { errors ->
            binding.titleInputLayout.error = errors.titleError
            binding.authorInputLayout.error = errors.authorError
            binding.pagesInputLayout.error = errors.pagesError
            binding.yearInputLayout.error = errors.yearError
            if(errors.dateError!=null){
                binding.inputLayoutStartDate.error=" "
            }
            else{
                binding.inputLayoutStartDate.error=null
            }
            binding.inputLayoutFinishDate.error= errors.dateError
            binding.isbnInputLayout.error=errors.isbnError
        }

        viewModel.saveSuccess.observe(this) { success ->
            success?.let {
                if (it !=null) {
                   // Snackbar.make(binding.root, "Book saved successfully", Snackbar.LENGTH_SHORT).show()
                    val resultIntent = Intent()
                    resultIntent.putExtra("EXTRA_SAVED_BOOK_ID", it)
                    resultIntent.putExtra("EXTRA_TITLE", viewModel.bookFormState.value.title)
                    resultIntent.putExtra("EXTRA_AUTHOR", viewModel.bookFormState.value.author)

                    setResult(Activity.RESULT_OK,resultIntent)
                    finish()
                } else {
                    Snackbar.make(binding.root, "Failed to save book", Snackbar.LENGTH_SHORT).show()
                }
                viewModel.resetSaveSuccess()
            }
        }
        viewModel.showValidationErrorEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                // This code will run only when the validation fails on a save attempt
                Snackbar.make(binding.root, "Please fix the errors before saving", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
*/
private fun setupObservers() {
    viewModel.bookFormState.observe(this) { state ->

        // Update Cover Image
        if (state.coverImageUri != null) {
            binding.coverImageCard.visibility = View.GONE
            binding.imageCl.visibility = View.VISIBLE
            binding.imageViewPreview.load(state.coverImageUri) {
                crossfade(true)
                placeholder(R.drawable.outline_image_24)
                error(R.drawable.outline_filter_list_24)
            }
        } else {
            binding.coverImageCard.visibility = View.VISIBLE
            binding.imageCl.visibility = View.GONE
        }

        // Update Title
        if (binding.titleEditText.text.toString() != state.title) {
            binding.titleEditText.setText(state.title)
        }

        // Update Subtitle
        if (binding.subtitleEditText.text.toString() != state.subtitle) {
            binding.subtitleEditText.setText(state.subtitle)
        }

        // Update Author
        if (binding.authorEditText.text.toString() != state.author) {
            binding.authorEditText.setText(state.author)
        }

        // Update Number of Pages
        if (binding.pagesEditText.text.toString() != state.numberOfPages.toString()) {
            binding.pagesEditText.setText(state.numberOfPages.toString())
        }

        // Update Publication Year
        if (binding.yearEditText.text.toString() != state.publicationYear.toString()) {
            binding.yearEditText.setText(state.publicationYear.toString())
        }

        // Update Description
        if (binding.descriptionEditText.text.toString() != state.description) {
            binding.descriptionEditText.setText(state.description)
        }

        // Update ISBN
        if (binding.isbnEditText.text.toString() != state.isbn) {
            binding.isbnEditText.setText(state.isbn)
        }

        // Update Review
        if (binding.reviewEditText.text.toString() != state.review) {
            binding.reviewEditText.setText(state.review)
        }

        // Update Notes
        if (binding.notesEditText.text.toString() != state.notes) {
            binding.notesEditText.setText(state.notes)
        }

        // Update Status Chips
        val buttonIdToCheck = when (state.status) {
            ReadingStatus.FINISHED -> R.id.finishedChip
            ReadingStatus.IN_PROGRESS -> R.id.inProgressChip
            ReadingStatus.FOR_LATER -> R.id.forLaterChip
            ReadingStatus.UNFINISHED -> R.id.unfinishedChip
        }
        if (binding.statusChipGroup.checkedChipId != buttonIdToCheck) {
            binding.statusChipGroup.check(buttonIdToCheck)
        }

        // Update Dates
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        state.startDate?.let {
            val formattedDate = dateFormat.format(Date(it))
            if (binding.editTextStartDate.text.toString() != formattedDate) {
                binding.editTextStartDate.setText(formattedDate)
            }
        }
        state.endDate?.let {
            val formattedDate = dateFormat.format(Date(it))
            if (binding.editTextFinishDate.text.toString() != formattedDate) {
                binding.editTextFinishDate.setText(formattedDate)
            }
        }

        // Update Rating
        if (binding.ratingBar.rating != state.rating) {
            binding.ratingBar.rating = state.rating.toFloat()
        }
        if (binding.autoCompleteFormat.text.toString() != state.format.displayName) {
            binding.autoCompleteFormat.setText(state.format.displayName, false)
        }
        updateTagsChipGroup(state.currentBookTags)
    }

    viewModel.validationError.observe(this) { errors ->
        binding.titleInputLayout.error = errors.titleError
        binding.authorInputLayout.error = errors.authorError
        binding.pagesInputLayout.error = errors.pagesError
        binding.yearInputLayout.error = errors.yearError
        if(errors.dateError!=null){
            binding.inputLayoutStartDate.error=" "
        }
        else{
            binding.inputLayoutStartDate.error=null
        }
        binding.inputLayoutFinishDate.error= errors.dateError
        binding.isbnInputLayout.error=errors.isbnError


    }
    viewModel.scrollToErrorEvent.observe(this) { event ->
        event.getContentIfNotHandled()?.let { viewId ->
            val viewToScrollTo = findViewById<View>(viewId)
            scrollToView(viewToScrollTo)
        }
    }

    viewModel.saveSuccess.observe(this) { success ->
        success?.let {
            // Snackbar.make(binding.root, "Book saved successfully", Snackbar.LENGTH_SHORT).show()
            val resultIntent = Intent()
            resultIntent.putExtra("EXTRA_SAVED_BOOK_ID", it)
            resultIntent.putExtra("EXTRA_TITLE", viewModel.bookFormState.value.title)
            resultIntent.putExtra("EXTRA_AUTHOR", viewModel.bookFormState.value.author)

            setResult(Activity.RESULT_OK,resultIntent)
            finish()
            viewModel.resetSaveSuccess()
        }
    }
    viewModel.showValidationErrorEvent.observe(this) { event ->
        event.getContentIfNotHandled()?.let {
            // This code will run only when the validation fails on a save attempt
            Snackbar.make(binding.root, "Please fix the errors before saving", Snackbar.LENGTH_SHORT).show()
        }
    }
    viewModel.showIsbnExistsError.observe(this) { event ->
        event.getContentIfNotHandled()?.let {
            Snackbar.make(binding.root, "A book with this ISBN already exists.", Snackbar.LENGTH_LONG).show()
        }
    }

}
    private fun scrollToView(viewToScrollTo: View) {
        // A small delay ensures the error message has been rendered before we scroll.
        binding.scrollView?.postDelayed({
            binding.scrollView?.smoothScrollTo(0, viewToScrollTo.top)
        }, 100)
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.add_book_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                lifecycleScope.launch {
                    viewModel.validateAndSave()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}