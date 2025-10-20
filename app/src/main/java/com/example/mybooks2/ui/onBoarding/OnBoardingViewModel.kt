package com.example.mybooks2.ui.onBoarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.data.BookDao
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.lifecycle.viewModelScope
import com.example.mybooks2.R
import com.example.mybooks2.model.Book
import com.example.mybooks2.ui.addBook2.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
class OnBoardingViewModel(private val bookDao: BookDao) : ViewModel() {

    fun addSampleBooks(context: Context): Job {
        return viewModelScope.launch(Dispatchers.IO) {

            val hobbitCoverPath = copyDrawableToInternalStorage(context, R.drawable.sample_cover_hobbit, UUID.randomUUID().toString())
            val duneCoverPath = copyDrawableToInternalStorage(context, R.drawable.sample_cover_dune, UUID.randomUUID().toString())
            val prideCoverPath = copyDrawableToInternalStorage(context, R.drawable.pp, UUID.randomUUID().toString())
            val sampleBooks = listOf(
                Book(
                    title = "The Hobbit",
                    author = "J.R.R. Tolkien",
                    status = ReadingStatus.FOR_LATER,
                    totalPages = 310,
                    coverImagePath = hobbitCoverPath // Use the path
                ),
                Book(
                    title = "Dune",
                    author = "Frank Herbert",
                    status = ReadingStatus.IN_PROGRESS,
                    totalPages = 412,
                    currentPage = 50,
                    coverImagePath = duneCoverPath // Use the path
                ),
                Book(
                    title = "Pride and Prejudice",
                    author = "Jane Austen",
                    status = ReadingStatus.FINISHED,
                    totalPages = 279,
                    personalRating = 4.0f,
                    coverImagePath = prideCoverPath
                )
            )

            sampleBooks.forEach { book ->
                try {
                    bookDao.insertBook(book)
                } catch (e: Exception) {
                    Log.e("SampleData", "Error inserting sample book: ${book.title}", e)
                }
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        Log.i("onboard123", "viewmodel cleared")
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                OnBoardingViewModel(application.database.bookDao())
            }
        }
    }
}
fun copyDrawableToInternalStorage(context: Context, @DrawableRes drawableResId: Int, uniqueName: String): String? {
    val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
    if (bitmap == null) return null // Could not decode drawable

    val fileName = "cover_${uniqueName}.jpg" // Or .png if your drawable is PNG
    val directory = context.filesDir
    val file = File(directory, fileName)

    try {
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream) // Use PNG if needed
            stream.flush()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }

    return file.absolutePath
}