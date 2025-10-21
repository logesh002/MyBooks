package com.example.mybooks2.model

data class GoogleBooksResponse(
    val items: List<VolumeItem>?
)

data class VolumeItem(
    val volumeInfo: VolumeInfo?
)

data class VolumeInfo(
    val title: String?,
    val subtitle: String?,
    val authors: List<String>?,
    val publishedDate: String?,
    val pageCount: Int?,
    val industryIdentifiers: List<IndustryIdentifier>?,
    val imageLinks: ImageLinks?
)

data class IndustryIdentifier(
    val type: String?,
    val identifier: String?
)

data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?,
    val large: String?,
    val small: String?,
    val medium: String?,
    val extraLarge: String?,
    )