package com.example.mybooks2.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Index
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.addBook2.ReadingStatus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Entity(tableName = "books" )

data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,
    val author: String,
    val status: ReadingStatus,
    val subtitle: String?=null,
    val totalPages: Int?=null,
    val currentPage: Int = 0,
    val addedDate: Long = System.currentTimeMillis(),
    val startDate: Long? = null,
    val finishedDate: Long? = null,
    val personalRating: Float? = null,
    val review: String?=null,
    val notes: String?=null,
    val description: String?=null,
    val isbn: String?=null,
    val tags: String?=null,
    val year: Int? = null,
    val timesRead: Int = 1,
    val coverImagePath: String? = null,
    val format: BookFormat = BookFormat.PAPERBACK
)
