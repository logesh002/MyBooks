package com.example.mybooks2.ui.onlineSearch

import com.example.mybooks2.model.BookDoc
import com.example.mybooks2.model.VolumeItem

data class UnifiedSearchResult(
    val title: String,
    val authors: String,
    val coverUrl: String?,
    val smallCoverUrl:String?=null,
    val isbn: String?,
    val year: Int?,
    val pages: Int?,
    val originalSource: Any
)

fun VolumeItem.toUnifiedResult(): UnifiedSearchResult {
    val volumeInfo = this.volumeInfo
    val isbn13 = volumeInfo?.industryIdentifiers?.find { it.type == "ISBN_13" }?.identifier
    val isbn10 = volumeInfo?.industryIdentifiers?.find { it.type == "ISBN_10" }?.identifier
    val year = volumeInfo?.publishedDate?.take(4)?.toIntOrNull()
    return UnifiedSearchResult(
        title = volumeInfo?.title ?: "No Title",
        authors = volumeInfo?.authors?.joinToString(", ") ?: "Unknown Author",
        coverUrl = volumeInfo?.imageLinks?.thumbnail?.replace("http://", "https://"),
        smallCoverUrl = volumeInfo?.imageLinks?.smallThumbnail?.replace("http://", "https://"),
        isbn = isbn13 ?: isbn10,
        year = year,
        pages = volumeInfo?.pageCount,
        originalSource = this
    )
}

fun BookDoc.toUnifiedResult(): UnifiedSearchResult {
    return UnifiedSearchResult(
        title = this.title ?: "No Title",
        authors = this.authorName?.joinToString(", ") ?: "Unknown Author",
        coverUrl = this.getCoverUrl("L"),
        smallCoverUrl =this.getCoverUrl("M") ,
        isbn = this.isbn?.firstOrNull(),
        year = this.firstPublishYear,
        pages = this.numberOfPages,
        originalSource = this
    )
}