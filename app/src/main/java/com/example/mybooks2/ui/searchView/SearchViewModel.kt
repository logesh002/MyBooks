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
import kotlinx.coroutines.launch

class SearchViewModel(private val bookDao: BookDao) : ViewModel() {

    private val _searchResults = MutableLiveData<List<Book>>()
    val searchResults: LiveData<List<Book>> = _searchResults
    private var lastQuery: String? = null


    fun search(query: String, force: Boolean = false) {

        if (!force && query == lastQuery) {
            return
        }

        lastQuery = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
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

