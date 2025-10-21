package com.example.mybooks2.ui.onlineSearch

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.preference.PreferenceManager
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.model.BookDoc
import com.example.mybooks2.model.VolumeItem
import com.example.mybooks2.network.ApiClient
import com.example.mybooks2.network.ApiClientGoogle
import com.example.mybooks2.ui.addBook2.Event
import kotlinx.coroutines.launch
import com.example.mybooks2.BuildConfig

class SearchOnlineViewModel(val application: MyBooksApplication) : ViewModel() {

    enum class SearchScreenState { IDLE, LOADING, SUCCESS, ERROR, NO_RESULTS }

    private val API_KEY = BuildConfig.API_KEY
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    val selectedApi = MutableLiveData<String>()
    private val _googleResults = MutableLiveData<List<VolumeItem>>()
    val googleResults: LiveData<List<VolumeItem>> = _googleResults

    private val _openLibraryResults = MutableLiveData<List<BookDoc>>()
    val openLibraryResults: LiveData<List<BookDoc>> = _openLibraryResults


    private val _unifiedSearchResults = MutableLiveData<List<UnifiedSearchResult>>()
    val unifiedSearchResults: LiveData<List<UnifiedSearchResult>> = _unifiedSearchResults
    private val _screenState = MutableLiveData<SearchScreenState>(SearchScreenState.IDLE)
    val screenState: LiveData<SearchScreenState> = _screenState
    private val _searchResults = MutableLiveData<List<VolumeItem>>()
    val searchResults: LiveData<List<VolumeItem>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
     var lastQuery: String? = null

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> = _errorMessage

    init {
        selectedApi.value = prefs.getString("online_search_api", "OPEN_LIBRARY")
    }

    fun searchNew(query: String, force: Boolean = false) {
        val sanitizedQuery = query.trim().replace(Regex("\\s+"), " ")
        if (!force && sanitizedQuery == lastQuery) return
        lastQuery = sanitizedQuery

        if (sanitizedQuery.isBlank()) {
            _googleResults.value = emptyList()
            _openLibraryResults.value = emptyList()
            _screenState.value = SearchScreenState.IDLE
            return
        }
        if (!NetworkUtils.isNetworkAvailable(application)) {
            _errorMessage.value = Event("No internet connection")
            _screenState.value = SearchScreenState.ERROR
            return
        }

        _screenState.value = SearchScreenState.LOADING
        viewModelScope.launch {
            try {
                selectedApi.postValue(prefs.getString("online_search_api", "GOOGLE_BOOKS"))

                val results: List<UnifiedSearchResult>
                if (selectedApi.value == "GOOGLE_BOOKS") {
                    val response = ApiClientGoogle.googleBooksApiService.searchVolumes(sanitizedQuery, API_KEY)
                    results = response.items?.map { it.toUnifiedResult() } ?: emptyList()
                } else {
                    val response = ApiClient.apiService.searchBooks(sanitizedQuery)
                    results = response.docs.map { it.toUnifiedResult() }
                }

                if (results.isEmpty()) {
                    _unifiedSearchResults.postValue(emptyList())
                    _screenState.postValue(SearchScreenState.NO_RESULTS)
                } else {
                    _unifiedSearchResults.postValue(results)
                    _screenState.postValue(SearchScreenState.SUCCESS)
                }
            } catch (e: Exception) {
                _unifiedSearchResults.postValue(emptyList())
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