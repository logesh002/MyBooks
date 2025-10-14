package com.example.mybooks2.ui.OnlineSearch

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.model.BookDoc
import com.example.mybooks2.network.ApiClient
import com.example.mybooks2.ui.addBook2.AddBook2ViewModel
import com.example.mybooks2.ui.addBook2.Event
import kotlinx.coroutines.launch

class SearchOnlineViewModel(val application: MyBooksApplication) : ViewModel() {

    private val _searchResults = MutableLiveData<List<BookDoc>>()
    val searchResults: LiveData<List<BookDoc>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private var lastQuery: String? = null

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> = _errorMessage
    fun search(query: String) {
        if (!NetworkUtils.isNetworkAvailable(application)) {
            _errorMessage.value = Event("No internet connection")
            return
        }
        val sanitizedQuery = query.trim().replace(Regex("\\s+"), " ")

        if (sanitizedQuery == lastQuery) {
            return
        }
        lastQuery = sanitizedQuery

        if (sanitizedQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.searchBooks(sanitizedQuery)
                _searchResults.postValue(response.docs)
            } catch (e: Exception) {
                Log.d("Exceptions",e.toString())
                _errorMessage.postValue(Event("Failed to fetch results"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                SearchOnlineViewModel(application)
            }
        }
    }
}