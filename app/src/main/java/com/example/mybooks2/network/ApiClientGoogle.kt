package com.example.mybooks2.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClientGoogle {
    private const val GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_BOOKS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val googleBooksApiService: GoogleBooksApiService by lazy {
        retrofit.create(GoogleBooksApiService::class.java)
    }
}