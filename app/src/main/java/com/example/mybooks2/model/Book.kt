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
    // Changed this field name for clarity
    val coverImagePath: String? = null,
    val format: BookFormat = BookFormat.PAPERBACK
){

}
fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, bookId: Int): String? {
    // Create a unique filename for the image, e.g., using the book's ID and a timestamp.
    val fileName = "cover_${bookId}_${System.currentTimeMillis()}.jpg"

    // Get the directory for your app's private files.
    // This is a secure location where only your app can access the files.
    val directory = context.filesDir

    // Create a File object for the new image.
    val file = File(directory, fileName)

    try {
        // Create an output stream to write the bitmap data to the file.
        val stream = FileOutputStream(file)

        // Compress the bitmap into JPEG format and write it to the stream.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)

        // Clean up the stream.
        stream.flush()
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
        return null // Return null if there was an error
    }

    // Return the absolute path of the saved file. This is the string you'll store in Room.
    return file.absolutePath
}
