package com.example.mybooks2.model

import com.google.gson.annotations.SerializedName

// This class represents the top-level JSON response
data class OpenLibrarySearchResponse(
    val numFound: Int,
    val docs: List<BookDoc>
)

// This class represents a single book document in the search results
data class BookDoc(
    val title: String?,
    @SerializedName("author_name")
    val authorName: List<String>?,
    @SerializedName("first_publish_year")
    val firstPublishYear: Int?,
    val isbn: List<String>?,
    @SerializedName("cover_i")
    val coverId: Int?
)