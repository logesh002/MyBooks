package com.example.mybooks2.ui.addBook2

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.preference.PreferenceManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.R
import com.example.mybooks2.data.BookDao
import com.example.mybooks2.model.Book
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.UUID

data class BookFormState(
    val id: Long = 0L,
    val coverImageUri: Uri? = null,
    val title: String = "",
    val subtitle: String = "",
    val author: String = "",
    val status: ReadingStatus = ReadingStatus.FOR_LATER,
    var rating: Float = 0.0f,
    val numberOfPages: String = "",
    val publicationYear: String = "",
    val description: String = "",
    val isbn: String = "",
    val tags: String = "",
    var review: String = "",
    val notes: String = "",
    var startDate:Long?=null,
    var endDate:Long?=null,
    val format: BookFormat = BookFormat.PAPERBACK,
    var coverImagePath: String?=null,
    val currentBookTags: Set<String> = emptySet(),
    val isCoverLoading: Boolean = false,
    val addDate: Long? = null
)
enum class ReadingStatus {
    FINISHED, IN_PROGRESS, FOR_LATER, UNFINISHED
}

enum class BookFormat(val displayName: String) {
    PAPERBACK("Paperback/Hardcover"),
    EBOOK("E-book"),
    AUDIOBOOK("Audiobook")
}

data class ValidationError(
    val titleError: String? = null,
    val authorError: String? = null,
    val pagesError: String? = null,
    val yearError: String? = null,
    val dateError: String? = null,
    val isbnError: String?=null
)

class AddBook2ViewModel(val bookDao: BookDao,
                        val application: MyBooksApplication,
                        prefs: SharedPreferences) : ViewModel() {

    private val defaultStatusName = prefs.getString("default_status_preference", "FOR_LATER")
    private val defaultFormatName = prefs.getString("default_format_preference", "PAPERBACK")


    private val _bookFormState = MutableLiveData(BookFormState(
        status = ReadingStatus.valueOf(defaultStatusName ?: "FOR_LATER"),
        format = BookFormat.valueOf(defaultFormatName ?: "PAPERBACK")
    ))
    val bookFormState: LiveData<BookFormState> = _bookFormState

    private val _showValidationErrorEvent = MutableLiveData<Event<Unit>>()
    val showValidationErrorEvent: LiveData<Event<Unit>> = _showValidationErrorEvent

    private val _scrollToErrorEvent = MutableLiveData<Event<Int>>()
    val scrollToErrorEvent: LiveData<Event<Int>> = _scrollToErrorEvent


    private var initialState: BookFormState? = null

    init {
        initialState = _bookFormState.value
    }
    private var currentBookId: Long? = null


    private val _validationError = MutableLiveData<ValidationError>()
    val validationError: LiveData<ValidationError> = _validationError


    private val _showIsbnExistsError = MutableLiveData<Event<Unit>>()
    val showIsbnExistsError: LiveData<Event<Unit>> = _showIsbnExistsError

    private val _saveSuccess = MutableLiveData<Long?>()
    val saveSuccess: LiveData<Long?> = _saveSuccess

    fun updateCoverImage(uri: Uri?) {
        _bookFormState.value = _bookFormState.value?.copy(coverImageUri = uri)
    }

    fun updateTitle(title: String) {
        _bookFormState.value = _bookFormState.value?.copy(title = title)
        validateTitle(title)
    }

    fun updateSubtitle(subtitle: String) {
        _bookFormState.value = _bookFormState.value?.copy(subtitle = subtitle)
    }

    fun updateAuthor(author: String) {
        _bookFormState.value = _bookFormState.value?.copy(author = author)
        validateAuthor(author)
    }

    fun updateStatus(status: ReadingStatus) {
        _bookFormState.value = _bookFormState.value?.copy(status = status)
    }

    fun updateRating(rating: Float) {
        _bookFormState.value = _bookFormState.value?.copy(rating = rating)
    }

    fun updateNumberOfPages(pages: String) {
        _bookFormState.value = _bookFormState.value?.copy(numberOfPages = pages)
        validatePages(pages)
    }

    fun updatePublicationYear(year: String) {
        _bookFormState.value = _bookFormState.value?.copy(publicationYear = year)
        validateYear(year)
    }

    fun updateDescription(description: String) {
        if (description.length <= 5000) {
            _bookFormState.value = _bookFormState.value?.copy(description = description)
        }
    }

    fun updateIsbn(isbn: String) {
        _bookFormState.value = _bookFormState.value?.copy(isbn = isbn)
        validateIsbn(isbn)
    }


    fun updateReview(review: String) {
        if (review.length <= 5000) {
            _bookFormState.value = _bookFormState.value?.copy(review = review)
        }
    }

    fun updateNotes(notes: String) {
        if (notes.length <= 5000) {
            _bookFormState.value = _bookFormState.value?.copy(notes = notes)
        }
    }

    fun updateStartDate(startDate: Long) {
        _bookFormState.value = _bookFormState.value?.copy(startDate = startDate)
        validateDates(startDate,bookFormState.value.endDate)

    }
    fun updateEndDate(endDate: Long){
        _bookFormState.value = _bookFormState.value?.copy(endDate = endDate)
        validateDates(bookFormState.value.startDate,endDate)

    }

    fun updateFormat(format: BookFormat) {
        _bookFormState.value = _bookFormState.value?.copy(format = format)
    }

    private fun validateTitle(title: String) {
        val currentError = _validationError.value ?: ValidationError()
        _validationError.value = if (title.isBlank()) {
            currentError.copy(titleError = "Title is required",)
        } else {
            currentError.copy(titleError = null)
        }
    }
    private fun validateDates(startDate: Long?, finishDate: Long?) {
        val currentError = _validationError.value ?: ValidationError()

        if (startDate != null && finishDate != null && startDate > finishDate) {
            _validationError.value = currentError.copy(dateError = "Start date cannot be after finish date")
        } else {
            _validationError.value = currentError.copy(dateError = null)
        }
    }

    private fun validateAuthor(author: String) {
        val currentError = _validationError.value ?: ValidationError()
        _validationError.value = if (author.isBlank()) {
            currentError.copy(authorError = "Author is required",)
        } else {
            currentError.copy(authorError = null)
        }
    }

    private fun validatePages(pages: String) {
        val currentError = _validationError.value ?: ValidationError()
        _validationError.value = when {
            pages.isNotBlank() && pages.toIntOrNull() == null -> {
                currentError.copy(pagesError = "Invalid number",)
            }
            pages.isNotBlank() && (pages.toIntOrNull() ?: 0) <= 0 -> {
                currentError.copy(pagesError = "Must be greater than 0",)
            }
            else -> {
                currentError.copy(pagesError =null)
            }
        }
    }

    private fun validateYear(year: String) {
        val currentError = _validationError.value ?: ValidationError()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        _validationError.value = when {

            year.isNotBlank() && year.toIntOrNull() == null -> {
                currentError.copy(yearError = "Invalid year",)
            }
            year.isNotBlank() && (year.toIntOrNull() ?: 0) > currentYear -> {
                currentError.copy(yearError = "Year cannot be in future",)
            }
            year.isNotBlank() && (year.toIntOrNull() ?: 0) < 1000 -> {
                currentError.copy(yearError = "Invalid year",)
            }
            else -> {
                currentError.copy(yearError = null)
            }
        }
    }
    /**
     * Validates an ISBN string.
     * @return An error message string if invalid, otherwise null.
     */
    private fun validateIsbn(isbn: String?) {
        val currentError = _validationError.value ?: ValidationError()
        if (isbn.isNullOrBlank()) {
            _validationError.value = currentError.copy(isbnError = null)
            return
        }

        val sanitizedIsbn = isbn.replace("-", "").replace(" ", "")

        _validationError.value = currentError.copy(isbnError =
         when (sanitizedIsbn.length) {
            10 -> if (isValidIsbn10Checksum(sanitizedIsbn)) null else "Invalid ISBN-10 checksum"
            13 -> if (isValidIsbn13Checksum(sanitizedIsbn)) null else "Invalid ISBN-13 checksum"
            else -> "ISBN must be 10 or 13 characters"
        }
        )
    }

    /**
     * Checks the validity of an ISBN-10 checksum.
     */
    private fun isValidIsbn10Checksum(isbn: String): Boolean {
        if (isbn.length != 10) return false

        return try {
            var sum = 0
            for (i in 0 until 9) {
                sum += isbn[i].digitToInt() * (10 - i)
            }

            val lastChar = isbn[9]
            sum += if (lastChar == 'X' || lastChar == 'x') 10 else lastChar.digitToInt()

            sum % 11 == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks the validity of an ISBN-13 checksum (EAN-13).
     */
    private fun isValidIsbn13Checksum(isbn: String): Boolean {
        if (isbn.length != 13) return false

        return try {
            var sum = 0
            for (i in 0 until 12) {
                val digit = isbn[i].digitToInt()
                sum += if (i % 2 == 0) digit else digit * 3
            }

            val checksum = (10 - (sum % 10)) % 10
            checksum == isbn[12].digitToInt()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun validateAndSave() {
        val state = _bookFormState.value ?: return

        validateTitle(state.title)
        validateAuthor(state.author)
        validatePages(state.numberOfPages)
        validateYear(state.publicationYear)
        validateDates(state.startDate,state.endDate)
        validateIsbn(state.isbn)



        val errors = _validationError.value ?: ValidationError()

        if (errors.titleError == null &&
            errors.authorError == null &&
            errors.pagesError == null &&
            errors.yearError == null &&
            errors.isbnError == null &&
            errors.dateError == null
            ) {

            var savedImagePath: String? = null

            state.coverImageUri?.let { uri ->
                savedImagePath = processAndSaveImage(application.applicationContext, uri)
            }
            state.coverImagePath = savedImagePath
            saveBook(state)
        }
        else{
            val viewIdToScrollTo = when {
                errors.titleError != null -> R.id.titleInputLayout
                errors.authorError != null -> R.id.authorInputLayout
                errors.pagesError != null -> R.id.pagesInputLayout
                errors.yearError != null -> R.id.pageAndYearLayout
                errors.dateError != null -> R.id.duration_text_view
                errors.isbnError != null -> R.id.isbn_ll
                else -> null
            }

            viewIdToScrollTo?.let {
                _scrollToErrorEvent.value = Event(it)
            }
            _showValidationErrorEvent.value = Event(Unit)

        }
    }
    suspend fun processAndSaveImage(context: Context, imageUri: Uri): String? {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .size(800, 800)
            .allowHardware(false)
            .build()

        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as BitmapDrawable).bitmap

            val uniqueName = UUID.randomUUID().toString()

            return saveImageToInternalStorage(context, bitmap, uniqueName)
        }
        return null
    }

    fun loadBook(bookId: Long) {
        currentBookId = bookId
        viewModelScope.launch {

            val bookWithTags = bookDao.getBookWithTags(bookId).first()

            if(bookWithTags == null){
                Toast.makeText(application.applicationContext,"Book not available",Toast.LENGTH_SHORT).show()
                return@launch
            }
            val book = bookWithTags.book
            val loadedTags = bookWithTags.tags.map { it.name }.toMutableSet()
            book.let { loadedBook ->
                val coverUri = loadedBook.coverImagePath?.let { path ->
                    if (path.isNotEmpty()) File(path).toUri() else null
                }

                val initialStateFromDb = BookFormState(
                    id = bookId,
                    title = loadedBook.title,
                    author = loadedBook.author,
                    numberOfPages = loadedBook.totalPages?.toString() ?: "",
                    publicationYear = loadedBook.year?.toString() ?: "",
                    subtitle = loadedBook.subtitle ?: "",
                    status = loadedBook.status,
                    description = loadedBook.description ?: "",
                    isbn = loadedBook.isbn ?: "",
                    tags = loadedBook.tags ?: "",
                    startDate = loadedBook.startDate,
                    endDate = loadedBook.finishedDate,
                    rating = loadedBook.personalRating?: 0.0f,
                    review = loadedBook.review ?: "",
                    coverImageUri = coverUri,
                    format = loadedBook.format,
                    currentBookTags = loadedTags,
                    addDate = loadedBook.addedDate,
                    notes = loadedBook.notes?:""
                )

                _bookFormState.postValue(initialStateFromDb)
                initialState = initialStateFromDb

            }
        }
    }
    fun hasUnsavedChanges(): Boolean {
        return _bookFormState.value != initialState
    }
    private fun saveBook( bookData: BookFormState) {
        if(bookData.status != ReadingStatus.FINISHED ) {
            bookData.rating=0.0f
            bookData.review=""
            bookData.endDate=null
            bookData.startDate = null
        }
        val book = bookData.toBook()

        viewModelScope.launch {
            var isDuplicate = false
            if (!book.isbn.isNullOrBlank()) {
                val existingBook = bookDao.getBookByIsbn(book.isbn)
                if (existingBook != null && existingBook.id != currentBookId) {
                    isDuplicate = true

                }
            }
            if (isDuplicate) {
                _showIsbnExistsError.postValue(Event(Unit))
            }
            else {

                try {
                    val bookId =
                        bookDao.saveBookWithTags(book, tagNames = bookData.currentBookTags.toSet())
                    _saveSuccess.postValue(bookId)

                } catch (e: SQLiteConstraintException) {
                    _showIsbnExistsError.postValue(Event(Unit))

                } catch (e: Exception) {
                    Log.e("Exception",e.toString())
                    _saveSuccess.postValue(null)
                }
            }
        }
    }

    private fun BookFormState.toBook(): Book {
        return Book(
            id = currentBookId?:0L,
            title = title,
            subtitle = subtitle,
            author = author,
            status = status,
            totalPages = numberOfPages.toIntOrNull(),
            personalRating = rating,
            review = review,
            notes =notes,
            description = description,
            startDate = startDate,
            finishedDate = endDate,
            isbn = isbn,
            tags = tags,
            coverImagePath = coverImagePath,
            year = publicationYear.toIntOrNull(),
            format = format,
            addedDate = addDate?:System.currentTimeMillis()
        )
    }
    val allTags: LiveData<List<String>> = bookDao.getAllTags().map { tags ->
        tags.map { it.name }
    }.asLiveData()

    fun addTag(tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isNotBlank()) {
            val currentTags = _bookFormState.value?.currentBookTags ?: emptySet()
            val updatedTags = currentTags + cleanTag
            _bookFormState.value = _bookFormState.value?.copy(currentBookTags = updatedTags)
        }
    }

    fun removeTag(tag: String) {
        val currentTags = _bookFormState.value?.currentBookTags ?: emptySet()
        val updatedTags = currentTags - tag
        _bookFormState.value = _bookFormState.value?.copy(currentBookTags = updatedTags)
    }


    fun resetSaveSuccess() {
        _saveSuccess.value = null
    }
    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                val prefs = PreferenceManager.getDefaultSharedPreferences(application)

                AddBook2ViewModel(application.database.bookDao(),application,prefs)
            }
        }
    }
    fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, uniqueName: String): String? {
        val fileName = "cover_${uniqueName}.jpg"

        val directory = context.filesDir
        val file = File(directory, fileName)

        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return file.absolutePath
    }

    fun prefillData(title: String, author: String?, isbn: String?, year: Int, coverUrl: String?,pages:Int) {
        val initialTextState = BookFormState(
            title = title,
            author = author ?: "",
            isbn = isbn ?: "",
            publicationYear = if(year<=0) "" else year.toString(),
            numberOfPages = if(pages<=0) "" else pages.toString(),
        )
        _bookFormState.value = initialTextState

        if (!coverUrl.isNullOrBlank()) {
            _bookFormState.value = _bookFormState.value?.copy(isCoverLoading = true)
            viewModelScope.launch {
                val imageLoader = ImageLoader(application)
                val request = ImageRequest.Builder(application)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as BitmapDrawable).bitmap

                    val uniqueName = UUID.randomUUID().toString()
                    val savedPath = saveImageToInternalStorage(application, bitmap, uniqueName)

                    savedPath?.let {
                        val localUri = File(it).toUri()
                        _bookFormState.postValue(_bookFormState.value?.copy(coverImageUri = localUri, isCoverLoading = false))
                    }
                }
                else{
                    _bookFormState.postValue(_bookFormState.value?.copy(isCoverLoading = false))
                }
            }
        }
    }
}

open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
    fun peekContent(): T = content
}