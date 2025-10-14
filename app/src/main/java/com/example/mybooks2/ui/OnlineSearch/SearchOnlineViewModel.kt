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
            // 2. If there's no connection, post an error message and stop.
            _errorMessage.value = Event("No internet connection")
            return
        }
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        if (query == lastQuery) {
            return
        }
        lastQuery = query
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.searchBooks(query)
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