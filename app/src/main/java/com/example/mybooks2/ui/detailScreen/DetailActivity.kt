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
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

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
                viewModel.deleteBook()
            }
        }
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.bookDetails.collect { bookWithTags ->
                if (bookWithTags == null) {
                    return@collect
                }

                val book = bookWithTags.book

                if (previousStatus!=null && previousStatus != ReadingStatus.FINISHED && book.status == ReadingStatus.FINISHED) {
                    showRatingDialog()
                }
                previousStatus = book.status

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

            appBarLayoutParams.height = (300 * resources.displayMetrics.density).toInt() // Convert 300dp to pixels

            binding.imageViewCoverLarge.visibility = View.VISIBLE


            binding.appBar.elevation = 4 * resources.displayMetrics.density


            Glide.with(this)
                .asBitmap() // Request a Bitmap to extract colors
                .load(File(book.coverImagePath))
                .error(R.drawable.outline_book_24)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        binding.imageViewCoverLarge.setImageBitmap(resource)
                        extractAndApplyDominantColor(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        binding.imageViewCoverLarge.setImageDrawable(placeholder)
                        resetAppBarColors()
                    }
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        binding.imageViewCoverLarge.setImageDrawable(errorDrawable)
                        resetAppBarColors()
                    }
                })


            binding.appBar.setExpanded(true, false)
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
            binding.collapsingToolbarLayout.statusBarScrim = null
            appBarLayoutParams.topMargin = statusBarHeight
            binding.appBar.elevation = 0f

            binding.toolbar.title = book.title
            binding.collapsingToolbarLayout.title = ""

            binding.imageViewCoverLarge.visibility = View.GONE

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
                    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

                    val startDateStr = dateFormat.format(Date(book.startDate))
                    val finishDateStr = dateFormat.format(Date(book.finishedDate))

                    val durationInMillis = book.finishedDate - book.startDate
                    val durationInDays = TimeUnit.MILLISECONDS.toDays(durationInMillis) + 1

                    val dateRangeText = "$startDateStr - $finishDateStr ($durationInDays days)"

                    binding.textViewDates.text = dateRangeText
                    binding.textViewDates.visibility = View.VISIBLE
                } else {
                    binding.textViewDates.visibility = View.GONE
                }
            }
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

        binding.cardReview.visibility = if (book.review?.isNotBlank()?:false) View.VISIBLE else View.GONE
        binding.textViewReview.text = book.review

        binding.cardNotes.visibility = if (book.notes?.isNotBlank()?:false) View.VISIBLE else View.GONE
        binding.textViewNotes.text = book.notes
        binding.isbnCard.visibility = if (book.isbn?.isNotBlank()?:false) View.VISIBLE else View.GONE
        binding.textIsbn.text = book.isbn

         if (book.addedDate != null) {
             binding.addedDateCard.visibility =    View.VISIBLE
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

            val addedDate = Date(book.addedDate)
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


    private fun extractAndApplyDominantColor(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            palette?.let {
                val defaultColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer)

                val mutedColor = it.getMutedColor(defaultColor)
                val darkMutedColor = it.getDarkMutedColor(mutedColor)

                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(darkMutedColor, mutedColor)
                )

                binding.collapsingToolbarLayout.background = gradient

                binding.collapsingToolbarLayout.contentScrim = ColorDrawable(darkMutedColor)
                binding.collapsingToolbarLayout.statusBarScrim = ColorDrawable(darkMutedColor)
            }
        }
    }


    private fun resetAppBarColors() {
        val defaultPrimaryColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer) // Or a different default
        val defaultSurfaceColor =ContextCompat.getColor(this, R.color.md_theme_surface)


        binding.collapsingToolbarLayout.background = ColorDrawable(defaultSurfaceColor)

        binding.collapsingToolbarLayout.contentScrim = ColorDrawable(defaultPrimaryColor)
        binding.collapsingToolbarLayout.statusBarScrim = ColorDrawable(defaultPrimaryColor)
    }


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

        if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        }

        return (56 * resources.displayMetrics.density).toInt()
    }

    private fun setupListeners() {
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

        val currentBook = viewModel.bookDetails.value?.book
        ratingBar.rating = currentBook?.personalRating?.toFloat() ?: 0f
        reviewEditText.setText(currentBook?.review ?: "")

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val rating = ratingBar.rating
                val review = reviewEditText.text.toString()
                viewModel.saveFinishedDetails(rating, review)
            }
            .show()
    }

    fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
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

        val currentStatus = viewModel.bookDetails.value?.book?.status
        shareMenuItem?.isVisible = (currentStatus == ReadingStatus.FINISHED)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                val intent = Intent(this, AddBook2::class.java).apply {
                    putExtra("EXTRA_BOOK_ID", currentBookId)
                }
                editBookLauncher.launch(intent)
                true
            }
            R.id.action_delete -> {
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

        val shareView = layoutInflater.inflate(R.layout.share_card_layout, null)

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

            Glide.with(this)
                .asBitmap()
                .load(File(book.coverImagePath))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        coverImage.setImageBitmap(resource)
                        val finalBitmap = createBitmapFromView(shareView)
                        shareBitmap(finalBitmap)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        coverImage.setImageResource(R.drawable.outline_book_24)
                        val finalBitmap = createBitmapFromView(shareView)
                        shareBitmap(finalBitmap)
                    }
                })
        } else {
            coverImage.setImageResource(R.drawable.outline_book_24)
            val finalBitmap = createBitmapFromView(shareView)
            shareBitmap(finalBitmap)
        }
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val widthInDp = 360
        val widthInPixels = (widthInDp * resources.displayMetrics.density).toInt()

        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthInPixels, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun shareBitmap(bitmap: Bitmap) {
        val cachePath = File(externalCacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "book_share.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Book"))
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}