package com.example.mybooks2.ui.onlineSearch

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
import com.example.mybooks2.model.VolumeItem
import com.example.mybooks2.network.ApiClient
import com.example.mybooks2.network.ApiClientGoogle
import com.example.mybooks2.ui.addBook2.Event
import kotlinx.coroutines.launch


class SearchOnlineViewModel(val application: MyBooksApplication) : ViewModel() {

    enum class SearchScreenState { IDLE, LOADING, SUCCESS, ERROR, NO_RESULTS }

    private val API_KEY ="***REMOVED***"

    private val _screenState = MutableLiveData<SearchScreenState>(SearchScreenState.IDLE)
    val screenState: LiveData<SearchScreenState> = _screenState
    private val _searchResults = MutableLiveData<List<VolumeItem>>()
    val searchResults: LiveData<List<VolumeItem>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
     var lastQuery: String? = null

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> = _errorMessage
//    fun search(query: String) {
//        if (!NetworkUtils.isNetworkAvailable(application)) {
//            _errorMessage.value = Event("No internet connection")
//            return
//        }
//        val sanitizedQuery = query.trim().replace(Regex("\\s+"), " ")
//
//        if (sanitizedQuery == lastQuery) {
//            return
//        }
//        lastQuery = sanitizedQuery
//
//        if (sanitizedQuery.isBlank()) {
//            _searchResults.value = emptyList()
//            return
//        }
//        _isLoading.value = true
//        _screenState.postValue(SearchScreenState.LOADING)
//        viewModelScope.launch {
//            try {
//                val response = ApiClient.apiService.searchBooks(query)
//                if (response.docs.isEmpty()) {
//                    _screenState.postValue(SearchScreenState.NO_RESULTS)
//                } else {
//                    _searchResults.postValue(response.docs)
//                    _screenState.postValue(SearchScreenState.SUCCESS)
//                }
//            } catch (e: Exception) {
//                _screenState.postValue(SearchScreenState.ERROR)
//            }
//        }
//    }

    fun search(query: String, force: Boolean = false) {
        val sanitizedQuery = query.trim().replace(Regex("\\s+"), " ")

        if (!force && sanitizedQuery == lastQuery) {
            return
        }
        lastQuery = sanitizedQuery

        if (sanitizedQuery.isBlank()) {
            _searchResults.value = emptyList()
            _screenState.value = SearchScreenState.IDLE
            return
        }

        if (!NetworkUtils.isNetworkAvailable(application)) {
            _errorMessage.value = Event("No internet connection") // You can still use this for a specific message
            _screenState.value = SearchScreenState.ERROR
            return
        }

        _screenState.value = SearchScreenState.LOADING
        viewModelScope.launch {
            try {
              //  val response = ApiClient.apiService.searchBooks(sanitizedQuery)
                val response = ApiClientGoogle.googleBooksApiService.searchVolumes(query, API_KEY)
                if (response.items.isNullOrEmpty()) {
                    _searchResults.postValue(emptyList()) // Assuming _searchResults is now LiveData<List<VolumeItem>>
                    _screenState.postValue(SearchScreenState.NO_RESULTS)
                } else {
                    _searchResults.postValue(response.items)
                    _screenState.postValue(SearchScreenState.SUCCESS)
                }
            } catch (e: Exception) {
                _searchResults.postValue(emptyList())
                _screenState.postValue(SearchScreenState.ERROR)
                e.printStackTrace()
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