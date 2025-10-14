package com.example.mybooks2.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.data.BookDao
import com.example.mybooks2.model.Book
import com.example.mybooks2.model.BookWithTags
import com.example.mybooks2.model.Tag
import com.example.mybooks2.ui.addBook2.AddBook2ViewModel
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.addBook2.ReadingStatus
import kotlinx.coroutines.launch


data class QueryState(
    val status: String = "In progress",
    val author: String? = null,
    val tag: String? = null,
    val sortBy: SortBy = SortBy.DATE_ADDED,
    val order: SortOrder = SortOrder.DESCENDING,
    val format: BookFormat? = null
)
class HomeViewModel(val bookDao: BookDao,
                    private val layoutPreferences: LayoutPreferences
) : ViewModel() {

    private val allBooks: LiveData<List<Book>> = bookDao.getAllBooks().asLiveData()

    private val allBooksWithTags: LiveData<List<BookWithTags>> = bookDao.getAllBooksWithTags().asLiveData()
    private val _queryState = MutableLiveData(QueryState())
    private val currentFilter = MutableLiveData("In progress")

    private val _filteredBooks = MediatorLiveData<List<Book>>()


    val allAuthors: LiveData<List<String>> = bookDao.getAllAuthors().asLiveData()
    val allTagNames: LiveData<List<Tag>> = bookDao.getAllTags().asLiveData()

    private val _layoutMode = MutableLiveData<LayoutMode>()
    val layoutMode: LiveData<LayoutMode> = _layoutMode


    val booksToShow: LiveData<List<Book>> = MediatorLiveData<List<Book>>().apply {
        addSource(allBooksWithTags) { update() }
        addSource(_queryState) { update() }
    }
    val filteredBooks: LiveData<List<Book>> = _filteredBooks

    private val _filters = MutableLiveData(BookFilters())
    private val _sortState = MutableLiveData(SortState())





    init {
        _filteredBooks.addSource(allBooks) { updateFilteredBooks() }
        _layoutMode.value = layoutPreferences.getLayoutMode()

    }
    data class BookFilters(
        val status: String = "In progress",
        val author: String? = null,
        val tag: String? = null
    )

    val allTags: LiveData<List<Tag>> = bookDao.getAllTags().asLiveData()



    private fun update() {
        val books = allBooksWithTags.value ?: return
        val query = _queryState.value ?: QueryState()


        val filteredList = books.filter { bookWithTags ->
            val book = bookWithTags.book
            val statusMatch = when (query.status) {
                "In progress" -> book.status == ReadingStatus.IN_PROGRESS
                "Finished" -> book.status == ReadingStatus.FINISHED
                "To be read" -> book.status == ReadingStatus.FOR_LATER
                else -> book.status == ReadingStatus.UNFINISHED
            }
            val authorMatch = query.author.isNullOrBlank() || book.author.equals(query.author, true)
            val tagMatch = query.tag.isNullOrBlank() || bookWithTags.tags.any { it.name.equals(query.tag, true) }
            val formatMatch = query.format == null || book.format == query.format
            statusMatch && authorMatch && tagMatch && formatMatch
        }

        val sortedList = when (query.sortBy) {
            SortBy.TITLE -> {
                if (query.order == SortOrder.ASCENDING) {
                    filteredList.sortedBy { it.book.title }
                } else {
                    filteredList.sortedByDescending { it.book.title }
                }
            }
            SortBy.AUTHOR -> {
                if (query.order == SortOrder.ASCENDING) {
                    filteredList.sortedBy { it.book.author }
                } else {
                    filteredList.sortedByDescending { it.book.author }
                }
            }
            SortBy.RATING -> {
                if (query.order == SortOrder.ASCENDING) {
                    filteredList.sortedBy { it.book.personalRating }
                } else {
                    filteredList.sortedByDescending { it.book.personalRating }
                }
            }
            SortBy.DATE_ADDED -> {
                if (query.order == SortOrder.ASCENDING) {
                    filteredList.sortedBy { it.book.addedDate }
                } else {
                    filteredList.sortedByDescending { it.book.addedDate }
                }
            }
        }

        (booksToShow as MediatorLiveData).value = sortedList.map { it.book }
    }

    fun updateQuery(updater: (QueryState) -> QueryState) {
        val currentQuery = _queryState.value ?: QueryState()
        _queryState.value = updater(currentQuery)
    }

    fun getCurrentQueryState(): QueryState = _queryState.value ?: QueryState()



    fun toggleLayoutMode() {
        val newMode = if (_layoutMode.value == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID
        _layoutMode.value = newMode
        layoutPreferences.saveLayoutMode(newMode)
    }


    private fun updateFilteredBooks() {
        val books = allBooks.value ?: return
        val filter = currentFilter.value ?: "In progress"
        val sortState = _sortState.value ?: SortState()

        val filtered = applyFilter (books,filter)

        val sorted = when (sortState.sortBy) {
            SortBy.TITLE -> if (sortState.order == SortOrder.ASCENDING) filtered.sortedBy { it.title } else filtered.sortedByDescending { it.title }
            SortBy.AUTHOR -> if (sortState.order == SortOrder.ASCENDING) filtered.sortedBy { it.author } else filtered.sortedByDescending { it.author }
            SortBy.RATING -> if (sortState.order == SortOrder.ASCENDING) filtered.sortedBy { it.personalRating } else filtered.sortedByDescending { it.personalRating }
            SortBy.DATE_ADDED -> if (sortState.order == SortOrder.ASCENDING) filtered.sortedBy { it.addedDate } else filtered.sortedByDescending { it.addedDate }
        }
        _filteredBooks.value = sorted
    }

    private fun applyFilter(books: List<Book>?, filter: String?): List<Book> {
        val bookList = books ?: return emptyList()
        return when (filter) {
            "In progress" -> bookList.filter { it.status == ReadingStatus.IN_PROGRESS }
            "Finished" -> bookList.filter { it.status == ReadingStatus.FINISHED }
            "To be read" -> bookList.filter { it.status == ReadingStatus.FOR_LATER }
            else -> bookList.filter { it.status == ReadingStatus.UNFINISHED }
        }
    }
    private val _selectedItems = MutableLiveData<Set<Long>>(emptySet())
    val selectedItems: LiveData<Set<Long>> = _selectedItems

    private val _isSelectionModeActive = MutableLiveData(false)
    val isSelectionModeActive: LiveData<Boolean> = _isSelectionModeActive

    fun toggleSelection(bookId: Long) {
        val currentSelection = _selectedItems.value ?: emptySet()
        val newSelection = if (bookId in currentSelection) {
            currentSelection - bookId
        } else {
            currentSelection + bookId
        }
        _selectedItems.value = newSelection
        _isSelectionModeActive.value = newSelection.isNotEmpty()
    }

    fun clearSelections() {
        _selectedItems.value = emptySet()
        _isSelectionModeActive.value = false
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val idsToDelete = _selectedItems.value ?: emptySet()
            if (idsToDelete.isNotEmpty()) {
                bookDao.deleteBooksByIds(idsToDelete.toList())
                clearSelections()
            }
        }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                HomeViewModel(application.database.bookDao(), LayoutPreferences(application.applicationContext))
            }
        }
    }

}