package com.example.mybooks2.ui.home

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.preference.PreferenceManager
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.R
import com.example.mybooks2.data.BookDao
import com.example.mybooks2.model.Book
import com.example.mybooks2.model.BookWithTags
import com.example.mybooks2.model.Tag
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.addBook2.Event
import com.example.mybooks2.ui.addBook2.ReadingStatus
import com.example.mybooks2.ui.home.util.LayoutMode
import com.example.mybooks2.ui.home.util.LayoutPreferences
import com.example.mybooks2.ui.home.util.SortBy
import com.example.mybooks2.ui.home.util.SortOrder
import kotlinx.coroutines.launch


enum class TagMatchMode { ANY, ALL }
data class QueryState(
    val status: String = "In progress",
    val author: String? = null,
    val tags: Set<String> = emptySet(),
    val sortBy: SortBy = SortBy.DATE_ADDED,
    val order: SortOrder = SortOrder.DESCENDING,
    val format: BookFormat? = null,
    val tagMatchMode: TagMatchMode = TagMatchMode.ANY
)
class HomeViewModel(val bookDao: BookDao,
                    private val layoutPreferences: LayoutPreferences,
                    val application: MyBooksApplication,
                    private val prefs: SharedPreferences,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener  {


    private val allBooksWithTags: LiveData<List<BookWithTags>> = bookDao.getAllBooksWithTags().asLiveData()
    private val _queryState: MutableLiveData<QueryState>

    private val _chipOrderChangedEvent = MutableLiveData<Event<Unit>>()
    val chipOrderChangedEvent: LiveData<Event<Unit>> = _chipOrderChangedEvent


    val allAuthors: LiveData<List<String>> = bookDao.getAllAuthors().asLiveData()

    private val _layoutMode = MutableLiveData<LayoutMode>()
    val layoutMode: LiveData<LayoutMode> = _layoutMode


    private val _booksToShow = MediatorLiveData<List<Book>>()
    val booksToShow: LiveData<List<Book>> = _booksToShow


    init {
        val defaultOrder = application.resources.getStringArray(R.array.status_values).joinToString(",")

        val savedOrderStr = prefs.getString("chip_order_preference", defaultOrder) ?: defaultOrder
        val statusOrder = savedOrderStr.split(",")



        val statusTextMap = mapOf(
            "IN_PROGRESS" to "In progress",
            "FINISHED" to "Finished",
            "FOR_LATER" to "To be read",
            "UNFINISHED" to "Dropped"
        )
        val initialStatus = if (statusOrder.isNotEmpty()) statusTextMap[statusOrder.first()]?:"In progress" else "In progress"

        _queryState = MutableLiveData(QueryState(status = initialStatus))

        _booksToShow.addSource(allBooksWithTags) { update() }
        _booksToShow.addSource(_queryState) { update() }

        _layoutMode.value = layoutPreferences.getLayoutMode()
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

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

            val tagMatch = when {
                query.tags.isEmpty() -> true
                query.tagMatchMode == TagMatchMode.ANY -> bookWithTags.tags.any { bookTag ->
                    query.tags.contains(bookTag.name)
                }
                query.tagMatchMode == TagMatchMode.ALL -> query.tags.all { filterTag ->
                    bookWithTags.tags.any { bookTag -> bookTag.name.equals(filterTag, true) }
                }
                else -> true
            }
            val formatMatch = query.format == null || book.format == query.format
            statusMatch && authorMatch && tagMatch && formatMatch
        }

        val sortedList = when (query.sortBy) {
            SortBy.TITLE -> {
                if (query.order == SortOrder.ASCENDING) {
                    filteredList.sortedBy { it.book.title.lowercase() }
                } else {
                    filteredList.sortedByDescending { it.book.title.lowercase() }
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
        _booksToShow.value = sortedList.map { it.book }
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "chip_order_preference") {
            _chipOrderChangedEvent.value = Event(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

                HomeViewModel(
                    application.database.bookDao(),
                    LayoutPreferences(application.applicationContext),
                    application,
                    prefs = sharedPreferences,
                )
            }
        }
    }

}