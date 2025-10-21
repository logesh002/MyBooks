package com.example.mybooks2.ui.detailScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.data.BookDao
import com.example.mybooks2.model.Book
import com.example.mybooks2.model.BookWithTags
import com.example.mybooks2.ui.addBook2.AddBook2ViewModel
import com.example.mybooks2.ui.addBook2.Event
import com.example.mybooks2.ui.addBook2.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookDetailViewModel(private val bookDao: BookDao) : ViewModel() {

    private val _bookDetails = MutableStateFlow<BookWithTags?>(null)
    val bookDetails: StateFlow<BookWithTags?> = _bookDetails
    private var previousBookState: Book? = null

    private val _showUndoSnackbarEvent =
        MutableLiveData<Event<ReadingStatus>>()
    val showUndoSnackbarEvent: LiveData<Event<ReadingStatus>> = _showUndoSnackbarEvent

    private val _showRatingDialogEvent = MutableLiveData<Event<Unit>>()
    val showRatingDialogEvent: LiveData<Event<Unit>> = _showRatingDialogEvent

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            bookDao.getBookWithTags(bookId).collect {
                _bookDetails.value = it
            }
        }
    }

    fun updateStatus(newStatus: ReadingStatus) {
        val currentBook = _bookDetails.value?.book ?: return

        previousBookState = currentBook
        viewModelScope.launch {
            var updatedBook = currentBook.copy(status = newStatus)

            if (currentBook.status == ReadingStatus.FOR_LATER && newStatus == ReadingStatus.IN_PROGRESS) {
                updatedBook = updatedBook.copy(startDate = System.currentTimeMillis())
            }
            if (newStatus == ReadingStatus.FINISHED) {
                updatedBook = updatedBook.copy(finishedDate = System.currentTimeMillis())
            }
            bookDao.updateBook(updatedBook)
            _showUndoSnackbarEvent.postValue(Event(newStatus))
        }
    }
    fun undoStatusChange() {
        previousBookState?.let { bookToRestore ->
            viewModelScope.launch {
                bookDao.updateBook(bookToRestore)
                previousBookState = null
            }
        }
    }

    fun triggerRatingDialogIfNeeded() {
        if (_bookDetails.value?.book?.status == ReadingStatus.FINISHED && previousBookState?.status != ReadingStatus.FINISHED) {
            _showRatingDialogEvent.value = Event(Unit)
        }
    }
    fun readAgain() {
        val currentBook = _bookDetails.value?.book ?: return
        viewModelScope.launch {
            val updatedBook = currentBook.copy(
                status = ReadingStatus.IN_PROGRESS,
                currentPage = 0,
                startDate = System.currentTimeMillis(),
                finishedDate = null,
                timesRead = currentBook.timesRead + 1
            )
            bookDao.updateBook(updatedBook)
        }
    }
    fun saveFinishedDetails(rating: Float, review: String) {
        val currentBook = _bookDetails.value?.book ?: return
        viewModelScope.launch {
            val updatedBook = currentBook.copy(
                personalRating = rating,
                review = review
            )
            bookDao.updateBook(updatedBook)
        }
    }

    fun setRating(rating: Float) {
        val currentBook = _bookDetails.value?.book ?: return
        viewModelScope.launch {
            val updatedBook = currentBook.copy(personalRating = rating)
            bookDao.updateBook(updatedBook)
        }
    }

    fun updateCurrentPage(currentPage: Int) {
        val currentBook = _bookDetails.value?.book ?: return

        previousBookState = currentBook
        viewModelScope.launch {
            var updatedBook = currentBook.copy(currentPage = currentPage)
            if (currentPage >= (currentBook.totalPages ?: 0)) {
                previousBookState = currentBook.copy(currentPage = if(currentPage>0) currentPage-1 else 0)
                updatedBook = updatedBook.copy(
                    status = ReadingStatus.FINISHED,
                    finishedDate = System.currentTimeMillis()
                )
                _showUndoSnackbarEvent.postValue(Event( ReadingStatus.FINISHED))

            }
            bookDao.updateBook(updatedBook)
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            _bookDetails.value?.book?.let {
                bookDao.deleteBook(it)
            }
        }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                BookDetailViewModel(application.database.bookDao())
            }
        }
    }
}