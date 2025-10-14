package com.example.mybooks2.model

import androidx.room.Entity

@Entity(tableName = "book_tag_cross_ref", primaryKeys = ["id", "tagId"])
data class BookTagCrossRef(
    val id: Long,
    val tagId: Long
)