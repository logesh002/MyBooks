package com.example.mybooks2.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class BookWithTags(
    @Embedded val book: Book,
    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(BookTagCrossRef::class)
    )
    val tags: List<Tag>
)