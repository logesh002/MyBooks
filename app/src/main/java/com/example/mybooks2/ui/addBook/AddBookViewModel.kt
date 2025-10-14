package com.example.mybooks2.ui.addBook

import androidx.lifecycle.ViewModel
import com.example.mybooks2.data.BookDao
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.model.Book
import com.example.mybooks2.ui.addBook2.ReadingStatus
import kotlinx.coroutines.launch

class AddBookViewModel(private val bookDao: BookDao) : ViewModel() {

    // LiveData for each form field
    val title = MutableLiveData<String>("")
    val author = MutableLiveData<String>("")
    val totalPages = MutableLiveData<String>("")
    val status = MutableLiveData<String>("Want to Read") // Default status

    // LiveData for the conditional "Read" section
    val startDate = MutableLiveData<Long?>()
    val finishDate = MutableLiveData<Long?>()
    val rating = MutableLiveData<Float?>()
    val review = MutableLiveData<String>("")

    // LiveData to signal when the save operation is complete
    private val _saveResult = MutableLiveData<Long?>()
    val saveResult: LiveData<Long?> = _saveResult

    private var currentBookId: Long? = null
    val isEditMode: Boolean
        get() = currentBookId != null

    val titleError = MutableLiveData<String?>()
    val authorError = MutableLiveData<String?>()
    val totalPagesError = MutableLiveData<String?>()
    val dateError = MutableLiveData<String?>()
    val isbnError =MutableLiveData<String?>()


    private fun validate(): Boolean {
        var isValid = true

        if (title.value.isNullOrBlank()) {
            titleError.value = "Title cannot be empty"
            isValid = false
        } else {
            titleError.value = null // Clear error
        }

        if (author.value.isNullOrBlank()) {
            authorError.value = "Author cannot be empty"
            isValid = false
        } else {
            authorError.value = null
        }

        // Total Pages Validation
        val pages = totalPages.value?.toIntOrNull()
        if (pages == null || pages <= 0) {
            totalPagesError.value = "Must be a positive number"
            isValid = false
        } else {
            totalPagesError.value = null
        }

        // Date Validation (only if status is "Read")
        if (status.value == "Read" && startDate.value != null && finishDate.value != null) {
            if (finishDate.value!! < startDate.value!!) {
                dateError.value = "Finish date cannot be before start date"
                isValid = false
            } else {
                dateError.value = null
            }
        } else {
            dateError.value = null
        }

        return isValid
    }

    fun loadBook(bookId: Long) {
        currentBookId = bookId
        viewModelScope.launch {
            val book = bookDao.getBookById(bookId)
            book?.let {
                title.value = it.title
                author.value = it.author
                totalPages.value = it.totalPages.toString()
                status.value = it.status.toString()
                startDate.value = it.startDate
                finishDate.value = it.finishedDate
                rating.value = it.personalRating?.toFloat()
                review.value = it.review ?: ""
            }
        }
    }

    // Save a new book or update an existing one
    fun saveBook() {
if(validate()) {
    val bookToSave = Book(
        id = currentBookId ?: 0, // Use current ID or 0 for a new book
        title = title.value!!,
        author = author.value!!,
        totalPages = totalPages.value?.toIntOrNull() ?: 0,
        status = ReadingStatus.FINISHED,
        startDate = startDate.value,
        finishedDate = finishDate.value,
        personalRating = rating.value,
        review = review.value
    )

    viewModelScope.launch {
        if (isEditMode) {
            bookDao.updateBook(bookToSave)
            _saveResult.postValue(currentBookId)
        } else {
            val newId = bookDao.insertBook(bookToSave)
            _saveResult.postValue(newId)
        }
        }
        }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                AddBookViewModel(application.database.bookDao())
            }
        }
    }
}

