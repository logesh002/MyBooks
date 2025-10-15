package com.example.mybooks2.network

import com.example.mybooks2.model.OpenLibrarySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApiService {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fields") fields: String = "key,title,author_name,isbn,first_publish_year,cover_i,number_of_pages_median",
        @Query("limit") limit: Int = 30
    ): OpenLibrarySearchResponse
}