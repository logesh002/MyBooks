package com.example.mybooks2.network

import com.example.mybooks2.model.OpenLibrarySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApiService {
    // Defines a GET request to the search endpoint
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String, // The main search query
        @Query("fields") fields: String = "key,title,author_name,isbn,first_publish_year,cover_i" // The fields we want
    ): OpenLibrarySearchResponse
}