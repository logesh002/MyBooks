package com.example.mybooks2.ui.setting

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.data.BookDao
import com.example.mybooks2.model.Book
import com.example.mybooks2.ui.addBook2.Event
import com.example.mybooks2.ui.addBook2.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class SettingsViewModel(
    private val bookDao: BookDao,
    private val context: Context
) : ViewModel() {

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>> = _toastMessage
    fun exportToCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val booksWithTags = bookDao.getAllBooksWithTags().first()

                val csvBuilder = StringBuilder()
                csvBuilder.append("title,author,isbn,status,rating,startDate,finishDate,review,tags\n")

                booksWithTags.forEach { bookWithTags ->
                    val book = bookWithTags.book
                    val tagsString = bookWithTags.tags.joinToString("|") { it.name }

                    csvBuilder.append("\"${book.title}\",")
                    csvBuilder.append("\"${book.author}\",")
                    csvBuilder.append("\"${book.isbn ?: ""}\",")
                    csvBuilder.append("${book.status.name},")
                    csvBuilder.append("${book.personalRating ?: ""},")
                    csvBuilder.append("${book.startDate ?: ""},")
                    csvBuilder.append("${book.finishedDate ?: ""},")
                    csvBuilder.append("\"${book.review ?: ""}\",")
                    csvBuilder.append("\"$tagsString\"\n")
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvBuilder.toString().toByteArray())
                }
                _toastMessage.postValue(Event("Successfully exported library."))

            }
            catch (e: Exception) {
                _toastMessage.postValue(Event("Error: Failed to export data."))
                e.printStackTrace()
            }
        }
    }

    fun importFromCsv(uri: Uri) {
        var importedCount = 0
        var skippedCount = 0
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    reader.readLine()

                    var line: String?
                    val csvSplitRegex = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")

                    while (reader.readLine().also { line = it } != null) {
                        val tokens = line!!.split(csvSplitRegex)

                        if (tokens.size >= 9) {
                            val isbn = tokens[2].removeSurrounding("\"").ifEmpty { null }
                            var shouldImport = true

                            if (!isbn.isNullOrBlank()) {
                                val existingBook = bookDao.getBookByIsbn(isbn)
                                if (existingBook != null) {
                                    shouldImport = false
                                    skippedCount++
                                }
                            }
                            if (shouldImport) {
                                val book = Book(
                                    title = tokens[0].removeSurrounding("\""),
                                    author = tokens[1].removeSurrounding("\""),
                                    isbn = tokens[2].removeSurrounding("\"").ifEmpty { null },
                                    status = ReadingStatus.valueOf(tokens[3]),
                                    personalRating = tokens[4].toFloatOrNull(),
                                    startDate = tokens[5].toLongOrNull(),
                                    finishedDate = tokens[6].toLongOrNull(),
                                    review = tokens[7].removeSurrounding("\"").ifEmpty { null }
                                )
                                val tagNames = tokens[8].removeSurrounding("\"").split("|")
                                    .filter { it.isNotBlank() }

                                bookDao.saveBookWithTags(book, tagNames.toSet())
                                importedCount++
                            }
                        }
                    }
                }
                val message = "Import complete: $importedCount books added, $skippedCount skipped."

                _toastMessage.postValue(Event(message))
            }
            catch (e: Exception) {
                _toastMessage.postValue(Event("Error: Failed to import data. Check file format."))
                e.printStackTrace()
            }
        }
    }
    companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                SettingsViewModel(application.database.bookDao(), application.applicationContext)
            }
        }
    }

}