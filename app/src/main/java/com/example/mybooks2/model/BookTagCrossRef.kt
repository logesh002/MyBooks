package com.example.mybooks2.model

import androidx.room.Entity

// This class links one Book with one Tag.
@Entity(tableName = "book_tag_cross_ref", primaryKeys = ["id", "tagId"])
data class BookTagCrossRef(
    val id: Long, // This is the book's ID
    val tagId: Long
)