package com.example.mybooks2.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClientGoogle {
    // New Base URL for Google Books API
    private const val GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Log everything: URL, headers, and body
    }
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_BOOKS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
    }

    val googleBooksApiService: GoogleBooksApiService by lazy {
        retrofit.create(GoogleBooksApiService::class.java)
    }
}