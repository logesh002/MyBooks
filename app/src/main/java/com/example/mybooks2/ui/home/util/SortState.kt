package com.example.mybooks2.ui.home.util

enum class SortBy {
    TITLE,
    AUTHOR,
    DATE_ADDED,
    RATING
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

data class SortState(
    val sortBy: SortBy = SortBy.DATE_ADDED,
    val order: SortOrder = SortOrder.DESCENDING
)