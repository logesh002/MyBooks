package com.example.mybooks2.model

// Represents the top-level response
data class GoogleBooksResponse(
    val items: List<VolumeItem>?
)

// Represents a single book item
data class VolumeItem(
    val volumeInfo: VolumeInfo?
)

// Contains the actual book details
data class VolumeInfo(
    val title: String?,
    val subtitle: String?,
    val authors: List<String>?,
    val publishedDate: String?,
    val pageCount: Int?,
    val industryIdentifiers: List<IndustryIdentifier>?,
    val imageLinks: ImageLinks?
)

// For getting the ISBN
data class IndustryIdentifier(
    val type: String?, // "ISBN_10" or "ISBN_13"
    val identifier: String?
)

// For getting the cover image URL
data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?
)