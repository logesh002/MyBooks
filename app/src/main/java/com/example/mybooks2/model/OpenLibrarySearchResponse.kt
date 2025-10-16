package com.example.mybooks2.model

import com.google.gson.annotations.SerializedName

data class OpenLibrarySearchResponse(
    val numFound: Int,
    val docs: List<BookDoc>
)

data class BookDoc(
    val title: String?,
    @SerializedName("author_name")
    val authorName: List<String>?,
    @SerializedName("first_publish_year")
    val firstPublishYear: Int?,
    val isbn: List<String>?,
    @SerializedName("cover_i")
    val coverId: Int?,
    @SerializedName("number_of_pages_median")
    val numberOfPages: Int?
){
    fun getCoverUrl(size: String = "M"): String? {
        return coverId?.let {
            "https://covers.openlibrary.org/b/id/$it-$size.jpg"
        }
    }
}