package com.example.mybooks2.ui.detailScreen

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import coil.load
import com.bumptech.glide.Glide
import com.example.bookapp.ui.AddBook2
import com.example.mybooks2.R
import com.example.mybooks2.databinding.ActivityDetailBinding
import com.example.mybooks2.databinding.AddBook2Binding
import com.example.mybooks2.model.BookWithTags
import com.example.mybooks2.model.Tag
import com.example.mybooks2.ui.addBook2.AddBook2ViewModel
import com.example.mybooks2.ui.addBook2.ReadingStatus
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mybooks2.ui.addBook2.BookFormat
import java.io.FileOutputStream

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private var currentBookId: Long = -1L
    val viewModel by viewModels<BookDetailViewModel> { BookDetailViewModel.Companion.factory }

    private lateinit var editBookLauncher: ActivityResultLauncher<Intent>

    private var statusBarHeight: Int = 0

    private var shareMenuItem: MenuItem? = null


    private var previousStatus: ReadingStatus? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Get the height of the status bar
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

//            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).top
//
//            binding.scrollView.updatePadding(bottom =bottomInset )
            // Apply the status bar height as top padding to the toolbar
          //  binding.toolbar.updatePadding(top = topInset)
            // Return the insets to allow other views to consume them
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


         currentBookId = intent.getLongExtra("EXTRA_BOOK_ID", -1L)
        if (currentBookId == -1L) {
            finish()
            return
        }
        viewModel.loadBook(currentBookId)

        editBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val title = data?.getStringExtra("EXTRA_TITLE")
                val author = data?.getStringExtra("EXTRA_AUTHOR")

                if (title != null && author != null) {
                    Snackbar.make(binding.root, "Book Saved: $title", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        observeViewModel()
        setupListeners()

        supportFragmentManager.setFragmentResultListener(DeleteBookDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val confirmed = bundle.getBoolean(DeleteBookDialogFragment.RESULT_KEY)
            if (confirmed) {
                // The user tapped "Delete" in the dialog.
                viewModel.deleteBook()
                // The activity will finish automatically because the book data will become null.
            }
        }
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.bookDetails.collect { bookWithTags ->

                println("in observe book dertail $bookWithTags")
                if (bookWithTags == null) {
                  //  finish()
                    return@collect
                }

                val book = bookWithTags.book

                // Check if status just changed TO Finished
                if (previousStatus!=null && previousStatus != ReadingStatus.FINISHED && book.status == ReadingStatus.FINISHED) {
                    showRatingDialog()
                }
                previousStatus = book.status // Update for next change

                updateUi(bookWithTags)
            }
        }
    }



    private fun updateUi(bookWithTags: BookWithTags) {
        println("updateUI")
        val book = bookWithTags.book

        val appBarLayoutParams = binding.appBar.layoutParams as CoordinatorLayout.LayoutParams
        val collapsingToolbarLayoutParams = binding.collapsingToolbarLayout.layoutParams as AppBarLayout.LayoutParams

        binding.collapsingToolbarLayout.title = ""
        binding.toolbar.title = ""

        binding.contentForLater.visibility = View.GONE
        binding.contentFinished.visibility = View.GONE
        binding.contentInProgress.visibility = View.GONE

        if (!book.subtitle.isNullOrBlank()) {
            binding.textViewSubtitle.visibility = View.VISIBLE
            binding.textViewSubtitle.text = book.subtitle
        } else {
            binding.textViewSubtitle.visibility = View.GONE
        }

        if (book.year != null && book.year > 0) {
            binding.textViewYear.visibility = View.VISIBLE
            binding.textViewYear.text = book.year.toString()
        } else {
            binding.textViewYear.visibility = View.GONE
        }

        // Handle Tags
        if (bookWithTags.tags.isNotEmpty()) {
            binding.chipGroupTags.visibility = View.VISIBLE
            populateTags(bookWithTags.tags)
        } else {
            binding.chipGroupTags.visibility = View.GONE
        }

        if (!book.subtitle.isNullOrBlank()) {
            binding.divider.visibility = View.VISIBLE
        } else {
            binding.divider.visibility = View.GONE
        }

        if (!book.coverImagePath.isNullOrEmpty()) {
            // --- IMAGE IS AVAILABLE ---

            appBarLayoutParams.height = (300 * resources.displayMetrics.density).toInt() // Convert 300dp to pixels

            // 1. Ensure ImageView is visible
            binding.imageViewCoverLarge.visibility = View.VISIBLE


            binding.appBar.elevation = 4 * resources.displayMetrics.density
            // 2. Load the image
//            binding.imageViewCoverLarge.load(File(book.coverImagePath)) {
//                error(R.drawable.outline_book_24)
//            }

            Glide.with(this)
                .asBitmap() // Request a Bitmap to extract colors
                .load(File(book.coverImagePath))
                .error(R.drawable.outline_book_24)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        binding.imageViewCoverLarge.setImageBitmap(resource) // Set the image
                        extractAndApplyDominantColor(resource) // Extract and apply color
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        binding.imageViewCoverLarge.setImageDrawable(placeholder)
                        // If image fails or clears, reset to default colors
                        resetAppBarColors()
                    }
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        binding.imageViewCoverLarge.setImageDrawable(errorDrawable)
                        // If image fails, reset to default colors
                        resetAppBarColors()
                    }
                })

            // 3. Ensure the AppBar is expanded and scrollable
            binding.appBar.setExpanded(true, false) // Expand without animation
            val params = binding.collapsingToolbarLayout.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            binding.toolbar.title = ""
            binding.toolbarTitleCustom.visibility = View.INVISIBLE
            appBarLayoutParams.topMargin = statusBarHeight
            binding.toolbar.updatePadding(top=20)
        } else {
            appBarLayoutParams.height = getToolbarHeight()

            binding.appBar.setBackgroundColor(Color.TRANSPARENT)
            binding.collapsingToolbarLayout.setContentScrimColor(Color.TRANSPARENT)
            binding.collapsingToolbarLayout.statusBarScrim = null // Also clear status bar scrim
            appBarLayoutParams.topMargin = statusBarHeight
            binding.appBar.elevation = 0f

            binding.toolbar.title = book.title
            binding.collapsingToolbarLayout.title = "" // Ensure collapsing title is empty

            // 4. Hide the ImageView
            binding.imageViewCoverLarge.visibility = View.GONE

            // 5. Disable scrolling
            binding.appBar.setExpanded(false, false)
            val params = binding.collapsingToolbarLayout.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL

        }
        binding.appBar.layoutParams = appBarLayoutParams
        binding.collapsingToolbarLayout.layoutParams = collapsingToolbarLayoutParams


        binding.textViewTitle.text = book.title
        binding.textViewAuthor.text = book.author


        binding.cardDescription.visibility = if (book.description?.isNotBlank() ?:false ) View.VISIBLE else View.GONE
        binding.textViewDescription.text = book.description


        when (book.status) {
            ReadingStatus.FOR_LATER -> {
                binding.statusTitle.text = "To be read"
                binding.statusIcon.setImageResource(R.drawable.outline_bookmark_24)

                binding.contentForLater.visibility = View.VISIBLE
            }
            ReadingStatus.IN_PROGRESS -> {
                binding.contentInProgress.visibility = View.VISIBLE
                binding.statusTitle.text = "In Progress"
                binding.statusIcon.setImageResource(R.drawable.outline_hourglass_top_24)

                if(book.totalPages != null && book.totalPages>0) {
                    binding.sliderProgress.visibility = View.VISIBLE
                    binding.textProgress.visibility = View.VISIBLE
                    binding.sliderProgress.valueTo = book.totalPages.toFloat()
                    binding.sliderProgress.value = book.currentPage.toFloat()
                    binding.textProgress.text = "${book.currentPage} / ${book.totalPages} pages"
                }
                else{
                    binding.sliderProgress.visibility = View.GONE
                    binding.textProgress.visibility = View.GONE
                }

            }
            ReadingStatus.FINISHED -> {
                binding.contentFinished.visibility = View.VISIBLE
                binding.statusTitle.text = "Completed"
                binding.statusIcon.setImageResource(R.drawable.outline_check_24)

                if(book.personalRating != null) {
                    binding.ratingBar.visibility =View.VISIBLE
                    binding.ratingBar.rating = book.personalRating.toFloat()
                }
                else{
                    binding.ratingBar.visibility =View.GONE
                }
                if(book.timesRead>1) {
                    binding.textReadCount.visibility =View.VISIBLE
                    binding.textReadCount.text = "Read ${book.timesRead} times"
                }
                else{
                    binding.textReadCount.visibility =View.GONE
                }

                if (book.startDate != null && book.finishedDate != null) {
                    // 1. Create a formatter for "Month Day, Year"
                    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

                    // 2. Format the start and finish dates
                    val startDateStr = dateFormat.format(Date(book.startDate))
                    val finishDateStr = dateFormat.format(Date(book.finishedDate))

                    // 3. Calculate the duration in days
                    val durationInMillis = book.finishedDate - book.startDate
                    // Use TimeUnit for accurate conversion and add 1 for an inclusive day count
                    val durationInDays = TimeUnit.MILLISECONDS.toDays(durationInMillis) + 1

                    // 4. Assemble the final string
                    val dateRangeText = "$startDateStr - $finishDateStr ($durationInDays days)"

                    // 5. Set the text
                    binding.textViewDates.text = dateRangeText
                    binding.textViewDates.visibility = View.VISIBLE
                } else {
                    // Hide the date text view if dates are not available
                    binding.textViewDates.visibility = View.GONE
                }
            }
            // Handle UNFINISHED case
            ReadingStatus.UNFINISHED -> {
                binding.statusTitle.text = "Dropped"
                binding.statusIcon.setImageResource(R.drawable.outline_cancel_presentation_24)

                binding.contentForLater.visibility = View.VISIBLE
            }
        }

        binding.cardDescription.visibility = if (book.description?.isNotBlank()?:false) View.VISIBLE else View.GONE
        binding.textViewDescription.text = book.description

        binding.cardPage.visibility = if(book.totalPages != null && book.totalPages>0) View.VISIBLE else View.GONE
        binding.textPage.text = book.totalPages.toString()

        // My Review
        binding.cardReview.visibility = if (book.review?.isNotBlank()?:false) View.VISIBLE else View.GONE
        binding.textViewReview.text = book.review

        binding.cardNotes.visibility = if (book.notes?.isNotBlank()?:false) View.VISIBLE else View.GONE
        binding.textViewNotes.text = book.notes
        binding.isbnCard.visibility = if (book.isbn?.isNotBlank()?:false) View.VISIBLE else View.GONE
        binding.textIsbn.text = book.isbn

         if (book.addedDate != null) {
             binding.addedDateCard.visibility =    View.VISIBLE
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

            // 2. Format the 'addedDate'
            val addedDate = Date(book.addedDate) // Convert the Long timestamp to a Date object
            binding.textViewAddedDate.text = "Added on ${dateFormat.format(addedDate)}"
        }
        else binding.addedDateCard.visibility =View.GONE

        binding.textViewFormat.text = book.format.displayName
        val iconRes = when (book.format) {
            BookFormat.PAPERBACK -> R.drawable.outline_menu_book_24
            BookFormat.EBOOK -> R.drawable.outline_fullscreen_portrait_24
            BookFormat.AUDIOBOOK -> R.drawable.outline_headphones_24
        }
        binding.textViewFormat.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)


        shareMenuItem?.isVisible = (book.status == ReadingStatus.FINISHED)

    }


//    private fun extractAndApplyDominantColor(bitmap: Bitmap) {
//        Palette.from(bitmap).generate { palette ->
//            palette?.let {
//                // Try to get a vibrant, muted, or other suitable color
//                val defaultColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer) // Fallback color
//
//                // Prioritize colors that work well for backgrounds
//                val dominantColor = it.getDominantColor(defaultColor)
//                val vibrantColor = it.getVibrantColor(dominantColor)
//                val mutedColor = it.getMutedColor(vibrantColor)
//
//                // Use a slightly darker version for the status bar if desired
//                val statusBarColor = manipulateColor(mutedColor, 0.8f) // Make it 20% darker
//
//                // Apply the color to the CollapsingToolbarLayout
//                binding.collapsingToolbarLayout.contentScrim = ColorDrawable(mutedColor)
//                binding.collapsingToolbarLayout.statusBarScrim = ColorDrawable(statusBarColor)
//                binding.appBar.setBackgroundColor(mutedColor) // Ensure AppBarLayout's initial background is also this color
//            }
//        }
//    }

    private fun extractAndApplyDominantColor(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            palette?.let {
                val defaultColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer)

                // Get two complementary colors from the palette
                val mutedColor = it.getMutedColor(defaultColor)
                val darkMutedColor = it.getDarkMutedColor(mutedColor)

                // Create a top-to-bottom gradient
                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(darkMutedColor, mutedColor)
                )

                // --- THE FIX ---

                // 1. Apply the gradient to the CollapsingToolbarLayout's background
                binding.collapsingToolbarLayout.background = gradient

                // 2. Set the scrims to SOLID colors from the gradient
                binding.collapsingToolbarLayout.contentScrim = ColorDrawable(darkMutedColor)
                binding.collapsingToolbarLayout.statusBarScrim = ColorDrawable(darkMutedColor)
            }
        }
    }


    private fun resetAppBarColors() {
        // Reset to your theme's default colors
        val defaultPrimaryColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer) // Or a different default
       // val defaultOnPrimaryColor = ContextCompat.getColor(this, R.color.md_theme_onPrimaryContainer)
        val defaultSurfaceColor =ContextCompat.getColor(this, R.color.md_theme_surface)
//        binding.collapsingToolbarLayout.contentScrim = ColorDrawable(defaultPrimaryColor)
//        binding.collapsingToolbarLayout.statusBarScrim = ColorDrawable(defaultPrimaryColor) // Or a darker version
//        binding.appBar.setBackgroundColor(defaultPrimaryColor)

        binding.collapsingToolbarLayout.background = ColorDrawable(defaultSurfaceColor)

        // Reset the scrims to your primary color
        binding.collapsingToolbarLayout.contentScrim = ColorDrawable(defaultPrimaryColor)
        binding.collapsingToolbarLayout.statusBarScrim = ColorDrawable(defaultPrimaryColor)
    }

    // Helper function to darken/lighten a color
    fun manipulateColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Math.round(Color.red(color) * factor)
        val g = Math.round(Color.green(color) * factor)
        val b = Math.round(Color.blue(color) * factor)
        return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255))
    }
    private fun populateTags(tags: List<Tag>) {
        binding.chipGroupTags.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(this).apply { text = tag.name }
            binding.chipGroupTags.addView(chip)
        }
    }
    private fun getToolbarHeight(): Int {
        val typedValue = TypedValue()

        // CORRECT: Use android.R.attr.actionBarSize
        if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        }

        // As a fallback, you can return a default value in dp converted to pixels
        return (56 * resources.displayMetrics.density).toInt()
    }

//    private fun setupListeners() {
//        binding.buttonStatus.setOnClickListener { showStatusUpdateDialog() }
//        binding.sliderProgress.addOnChangeListener { _, value, fromUser ->
//            if(fromUser) {
//                viewModel.updateCurrentPage(value.toInt())
//            }
//        }
//    }
    private fun setupListeners() {
        println("setup k")
        binding.buttonStartReading.setOnClickListener { viewModel.updateStatus(ReadingStatus.IN_PROGRESS) }
        binding.buttonMarkAsFinished.setOnClickListener { viewModel.updateStatus(ReadingStatus.FINISHED) }
        binding.buttonReadAgain.setOnClickListener { viewModel.readAgain() }
    binding.buttonDrop.setOnClickListener { viewModel.updateStatus(ReadingStatus.UNFINISHED) }

    binding.sliderProgress.addOnChangeListener { _, value, fromUser ->
            if(fromUser) {
                viewModel.updateCurrentPage(value.toInt())
           }
        }
    }

    private fun showRatingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.dialog_rating_bar)

        val reviewEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_review)

        // Pre-fill the dialog with existing data if it exists
        val currentBook = viewModel.bookDetails.value?.book
        ratingBar.rating = currentBook?.personalRating?.toFloat() ?: 0f
        reviewEditText.setText(currentBook?.review ?: "")

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val rating = ratingBar.rating
                val review = reviewEditText.text.toString()
                // Call the new ViewModel function
                viewModel.saveFinishedDetails(rating, review)
            }
            .show()
    }

    fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    private fun showStatusUpdateDialog() {
        val currentStatus = viewModel.bookDetails.value?.book?.status ?: return
        val options = when (currentStatus) {
            ReadingStatus.FOR_LATER -> arrayOf("Start Reading")
            ReadingStatus.IN_PROGRESS -> arrayOf("Finish", "Drop")
            ReadingStatus.UNFINISHED -> arrayOf("Continue Reading")
            else -> return
        }.map { it.toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Update Status")
            .setItems(options) { _, which ->
                val newStatus = when (options[which]) {
                    "Start Reading", "Continue Reading" -> ReadingStatus.IN_PROGRESS
                    "Finish" -> ReadingStatus.FINISHED
                    "Drop" -> ReadingStatus.UNFINISHED
                    else -> currentStatus
                }
                viewModel.updateStatus(newStatus)
            }
            .show()
    }
    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Book?")
            .setMessage("Are you sure you want to permanently delete this book? This action cannot be undone.")
            .setNegativeButton("Cancel", null) // Does nothing, just dismisses the dialog
            .setPositiveButton("Delete") { _, _ ->
                // User confirmed the deletion
                viewModel.deleteBook()
                finish() // Close the detail screen and go back to the list
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)

        shareMenuItem = menu.findItem(R.id.action_share)

        // 3. Set its initial visibility based on the current data in the ViewModel
        val currentStatus = viewModel.bookDetails.value?.book?.status
        shareMenuItem?.isVisible = (currentStatus == ReadingStatus.FINISHED)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                // Navigate to AddBookActivity in edit mode
                val intent = Intent(this, AddBook2::class.java).apply {
                    // Pass the book's ID to the activity for "edit" mode
                    putExtra("EXTRA_BOOK_ID", currentBookId)
                }
                editBookLauncher.launch(intent)
                true
            }
            R.id.action_delete -> {
                // Show confirmation dialog, then call viewModel.deleteBook()
                showDeleteConfirmationDialog()
                true
            }
            R.id.action_share -> {
                viewModel.bookDetails.value?.let { shareBookAsImage(it) }
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareBookAsImage(bookWithTags: BookWithTags) {
        val book = bookWithTags.book

        // 1. Inflate the custom layout
        val shareView = layoutInflater.inflate(R.layout.share_card_layout, null)

        // 2. Find views and populate data
        val coverImage = shareView.findViewById<ImageView>(R.id.share_image_cover)
        val titleText = shareView.findViewById<TextView>(R.id.share_text_title)
        val authorText = shareView.findViewById<TextView>(R.id.share_text_author)
        val ratingBar = shareView.findViewById<RatingBar>(R.id.share_rating_bar)

        val reviewText = shareView.findViewById<TextView>(R.id.share_text_review) // Find the new TextView

        titleText.text = book.title
        authorText.text = "by ${book.author}"

        if(book.personalRating != null && book.personalRating>0) {
            ratingBar.rating = book.personalRating.toFloat()
        }
        else{
            ratingBar.visibility = View.GONE
        }


        if (!book.review.isNullOrBlank()) {
            reviewText.visibility = View.VISIBLE
            reviewText.text = "\"${book.review}\""
        } else {
            reviewText.visibility = View.GONE
        }

        if (!book.coverImagePath.isNullOrBlank()) {
            // --- CASE 1: IMAGE EXISTS ---
            // Load the image with Glide. The rest of the process happens in the callback.
            Glide.with(this)
                .asBitmap()
                .load(File(book.coverImagePath))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Image loaded, set it and generate the final bitmap
                        coverImage.setImageBitmap(resource)
                        val finalBitmap = createBitmapFromView(shareView)
                        shareBitmap(finalBitmap)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // Safety net: if image fails to load, generate the card without it
                        coverImage.setImageResource(R.drawable.outline_book_24)
                        val finalBitmap = createBitmapFromView(shareView)
                        shareBitmap(finalBitmap)
                    }
                })
        } else {
            // --- CASE 2: NO IMAGE EXISTS ---
            // Set a placeholder and generate the bitmap immediately.
            coverImage.setImageResource(R.drawable.outline_book_24)
            val finalBitmap = createBitmapFromView(shareView)
            shareBitmap(finalBitmap)
        }
    }

    private fun createBitmapFromView(view: View): Bitmap {
        // 1. Define the desired width of your share card from your XML (in DP)
        val widthInDp = 360
        // Convert the DP value to pixels
        val widthInPixels = (widthInDp * resources.displayMetrics.density).toInt()

        // 2. Create the MeasureSpec using the explicit pixel width
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthInPixels, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        // 3. Now measure the view with the new, valid specs
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // The rest of the function remains the same
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun shareBitmap(bitmap: Bitmap) {
        // Save bitmap to cache directory
        val cachePath = File(externalCacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "book_share.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        // Get a content URI using the FileProvider
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)

        // Create the share intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Book"))
    }

    override fun onDestroy() {
        println("Destroy")
        super.onDestroy()
    }
}