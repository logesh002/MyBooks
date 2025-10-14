package com.example.mybooks2.ui.searchView

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
import com.example.mybooks2.ui.home.HomeViewModel
import com.example.mybooks2.ui.home.LayoutPreferences
import kotlinx.coroutines.launch

class SearchViewModel(private val bookDao: BookDao) : ViewModel() {

    private val _searchResults = MutableLiveData<List<Book>>()
    val searchResults: LiveData<List<Book>> = _searchResults

    fun search(query: String) {
        if (query.isBlank()) { // Optional: don't search for very short queries
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            // Add wildcards for the LIKE query
            val formattedQuery = "%$query%"
            _searchResults.postValue(bookDao.searchBooks(formattedQuery))
        }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                SearchViewModel(application.database.bookDao())
            }
        }
    }
}

