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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mybooks2.R
import com.example.mybooks2.model.Book
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.addBook2.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
class OnBoardingViewModel(private val bookDao: BookDao) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    fun addSampleBooks(context: Context): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            val hobbitCoverPath = copyDrawableToInternalStorage(context, R.drawable.sample_cover_hobbit, UUID.randomUUID().toString())
            val duneCoverPath = copyDrawableToInternalStorage(context, R.drawable.sample_cover_dune, UUID.randomUUID().toString())
            val prideCoverPath = copyDrawableToInternalStorage(context, R.drawable.pp, UUID.randomUUID().toString())
            val book1984CoverPath = copyDrawableToInternalStorage(context, R.drawable.the1984, UUID.randomUUID().toString())

            val mockingbirdCoverPath = copyDrawableToInternalStorage(context, R.drawable.mockingbird, UUID.randomUUID().toString())
            val gatsbyCoverPath = copyDrawableToInternalStorage(context, R.drawable.gatsby, UUID.randomUUID().toString())
            val fellowshipCoverPath = copyDrawableToInternalStorage(context, R.drawable.fellwoship, UUID.randomUUID().toString())
            val alchemistCoverPath = copyDrawableToInternalStorage(context, R.drawable.alchemist, UUID.randomUUID().toString())
            val mobyCoverPath = copyDrawableToInternalStorage(context, R.drawable.mobydick, UUID.randomUUID().toString())
            val sapiensCoverPath = copyDrawableToInternalStorage(context, R.drawable.sapiens, UUID.randomUUID().toString())
            val atomicCoverPath = copyDrawableToInternalStorage(context, R.drawable.atomichabits, UUID.randomUUID().toString())

            val sampleBooks = listOf(
                Book(
                    title = "The Hobbit",
                    subtitle = "There and Back Again",
                    author = "J.R.R. Tolkien",
                    status = ReadingStatus.FOR_LATER,
                    format = BookFormat.AUDIOBOOK,
                    totalPages = 310,
                    year = 1937,
                    startDate = null,
                    finishedDate = null,
                    coverImagePath = hobbitCoverPath
                ),
                Book(
                    title = "Dune",
                    subtitle = "Book One in the Dune Saga",
                    author = "Frank Herbert",
                    status = ReadingStatus.IN_PROGRESS,
                    totalPages = 412,
                    currentPage = 50,
                    year = 1965,
                    startDate = 1735689600000L,
                    finishedDate = null,
                    coverImagePath = duneCoverPath
                ),
                Book(
                    title = "Pride and Prejudice",
                    author = "Jane Austen",
                    status = ReadingStatus.FINISHED,
                    totalPages = 279,
                    personalRating = 4.0f,
                    review = "A delightful classic with strong characters and wit.",
                    year = 1813,
                    startDate = 1735862400000L,
                    finishedDate = 1736294400000L,
                    coverImagePath = prideCoverPath
                ),
                Book(
                    title = "1984",
                    subtitle = "A Dystopian Novel",
                    author = "George Orwell",
                    status = ReadingStatus.IN_PROGRESS,
                    format = BookFormat.EBOOK,
                    totalPages = 328,
                    currentPage = 120,
                    year = 1949,
                    startDate = 1736467200000L,
                    finishedDate = null,
                    coverImagePath = book1984CoverPath
                ),
                Book(
                    title = "To Kill a Mockingbird",
                    author = "Harper Lee",
                    status = ReadingStatus.FINISHED,
                    totalPages = 281,
                    personalRating = 4.5f,
                    review = "Powerful storytelling and moral depth that stays with you.",
                    year = 1960,
                    startDate = 1736726400000L,
                    finishedDate = 1737072000000L,
                    coverImagePath = mockingbirdCoverPath
                ),
                Book(
                    title = "The Catcher in the Rye",
                    author = "J.D. Salinger",
                    status = ReadingStatus.FOR_LATER,
                    totalPages = 234,
                    year = 1951,
                    startDate = null,
                    finishedDate = null,
                ),
                Book(
                    title = "The Great Gatsby",
                    subtitle = "A Novel of the Jazz Age",
                    author = "F. Scott Fitzgerald",
                    status = ReadingStatus.FINISHED,
                    totalPages = 180,
                    personalRating = 3.5f,
                    review = "A short but impactful reflection on ambition and love.",
                    year = 1925,
                    startDate = 1737580800000L,
                    finishedDate = 1737936000000L,
                    coverImagePath = gatsbyCoverPath
                ),
                Book(
                    title = "Brave New World",
                    subtitle = "A Vision of the Future",
                    author = "Aldous Huxley",
                    status = ReadingStatus.IN_PROGRESS,
                    totalPages = 268,
                    currentPage = 90,
                    year = 1932,
                    startDate = 1738454400000L,
                    finishedDate = null,
                ),
                Book(
                    title = "The Fellowship of the Ring",
                    subtitle = "The Lord of the Rings: Part One",
                    author = "J.R.R. Tolkien",
                    status = ReadingStatus.FOR_LATER,
                    totalPages = 423,
                    year = 1954,
                    startDate = null,
                    finishedDate = null,
                    coverImagePath = fellowshipCoverPath
                ),
                Book(
                    title = "The Alchemist",
                    subtitle = "A Fable About Following Your Dream",
                    author = "Paulo Coelho",
                    status = ReadingStatus.FINISHED,
                    totalPages = 197,
                    personalRating = 4.0f,
                    review = "Simple yet deeply inspiring. A timeless spiritual journey.",
                    year = 1988,
                    startDate = 1732579200000L,
                    finishedDate = 1733097600000L,
                    coverImagePath = alchemistCoverPath,
                    format = BookFormat.EBOOK,
                ),
                Book(
                    title = "Moby Dick",
                    subtitle = "Or, The Whale",
                    author = "Herman Melville",
                    status = ReadingStatus.FOR_LATER,
                    totalPages = 585,
                    year = 1851,
                    startDate = null,
                    finishedDate = null,
                    coverImagePath = mobyCoverPath,
                    format = BookFormat.EBOOK,
                ),
                Book(
                    title = "The Name of the Wind",
                    subtitle = "The Kingkiller Chronicle: Day One",
                    author = "Patrick Rothfuss",
                    status = ReadingStatus.UNFINISHED,
                    totalPages = 662,
                    currentPage = 310,
                    year = 2007,
                    startDate = 1733616000000L,
                    finishedDate = null,
                    format = BookFormat.EBOOK,
                ),
                Book(
                    title = "The Road",
                    author = "Cormac McCarthy",
                    status = ReadingStatus.FINISHED,
                    totalPages = 287,
                    personalRating = 4.5f,
                    review = "Hauntingly beautiful and bleak. A masterpiece of minimalism.",
                    year = 2006,
                    startDate = 1738886400000L,
                    finishedDate = 1739318400000L,
                    format = BookFormat.AUDIOBOOK,
                ),
                Book(
                    title = "Sapiens",
                    subtitle = "A Brief History of Humankind",
                    author = "Yuval Noah Harari",
                    status = ReadingStatus.UNFINISHED,
                    totalPages = 443,
                    currentPage = 150,
                    year = 2011,
                    startDate = 1734652800000L,
                    finishedDate = null,
                    coverImagePath = sapiensCoverPath,
                    format = BookFormat.EBOOK,
                ),
                Book(
                    title = "Atomic Habits",
                    subtitle = "An Easy & Proven Way to Build Good Habits and Break Bad Ones",
                    author = "James Clear",
                    status = ReadingStatus.FINISHED,
                    totalPages = 320,
                    personalRating = 4.9f,
                    review = "Highly practical and life-changing. A must-read for personal growth.",
                    year = 2018,
                    startDate = 1741305600000L,
                    finishedDate = 1741737600000L,
                    coverImagePath = atomicCoverPath,
                    format = BookFormat.AUDIOBOOK,
                )
            )


            sampleBooks.forEach { book ->
                try {
                    if(book.title == "The Hobbit"){
                        bookDao.insertBookWithTags(book,listOf("Fantasy","Adventure"))
                    }
                    else if(book.title == "Atomic Habits" || book.title == "Sapiens"){
                        bookDao.insertBookWithTags(book,listOf("Non-Fiction"))
                    }
                    else bookDao.insertBook(book)
                } catch (e: Exception) {
                    Log.e("SampleData", "Error inserting sample book: ${book.title}", e)
                }
            }
            _isLoading.postValue(false)
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
    if (bitmap == null) return null

    val fileName = "cover_${uniqueName}.jpg"
    val directory = context.filesDir
    val file = File(directory, fileName)

    try {
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }

    return file.absolutePath
}