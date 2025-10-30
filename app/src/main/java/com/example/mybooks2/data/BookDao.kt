package com.example.mybooks2.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mybooks2.model.Book
import com.example.mybooks2.model.BookTagCrossRef
import com.example.mybooks2.model.BookWithTags
import com.example.mybooks2.model.Tag
import com.example.mybooks2.ui.statistics.FormatCount
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {


    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Int): Flow<Book>

    @Query("SELECT * FROM books ORDER BY addedDate DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY addedDate DESC")
    fun getBooksByStatus(status: String): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBook(book: Book): Long


    @Query("SELECT * FROM books WHERE isbn = :isbn LIMIT 1")
    suspend fun getBookByIsbn(isbn: String): Book?

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): Book?

    @Query("DELETE FROM books WHERE id IN (:bookIds)")
    suspend fun deleteBooksByIds(bookIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookTagCrossRef(crossRef: BookTagCrossRef)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :tagName")
    suspend fun getTagByName(tagName: String): Tag?

    @Transaction
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookWithTags(bookId: Long): Flow<BookWithTags?>

    @Transaction
    suspend fun insertBookWithTags(book: Book, tagNames: List<String>): Long {
        val bookId = insertBook(book)
        for (tagName in tagNames) {
            var tagId = getTagByName(tagName)?.tagId
            if (tagId == null) {
                tagId = insertTag(Tag(name = tagName))
            }
            insertBookTagCrossRef(BookTagCrossRef(bookId, tagId))
        }
        return bookId
    }
    @Query("DELETE FROM book_tag_cross_ref WHERE id = :bookId")
    suspend fun deleteTagsForBook(bookId: Long)

    @Transaction
    suspend fun saveBookWithTags(book: Book, tagNames: Set<String>): Long {
        val bookId = if (book.id == 0L) insertBook(book)
        else {
            updateBook(book)
            book.id
        }

        deleteTagsForBook(bookId)

        for (tagName in tagNames) {
            val tagId = insertTag(Tag(name = tagName))

            val finalTagId = if (tagId == -1L) {
                getTagByName(tagName)!!.tagId
            } else {
                tagId
            }
            insertBookTagCrossRef(BookTagCrossRef(bookId, finalTagId))
        }
        return bookId
    }

    @Query("SELECT DISTINCT author FROM books WHERE author IS NOT '' ORDER BY author ASC")
    fun getAllAuthors(): Flow<List<String>>

    @Query("SELECT * FROM books WHERE title LIKE :query OR author LIKE :query OR isbn LIKE :query")
    suspend fun searchBooks(query: String): List<Book>

    @Transaction
    @Query("SELECT * FROM books")
    fun getAllBooksWithTags(): Flow<List<BookWithTags>>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'FINISHED'")
    fun getTotalBooksRead(): Flow<Int>

    @Query("SELECT SUM(totalPages) FROM books WHERE status = 'FINISHED'")
    fun getTotalPagesRead(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'IN_PROGRESS'")
    fun getBooksInProgressCount(): Flow<Int>

    @Query("SELECT AVG(personalRating) FROM books WHERE status = 'FINISHED' AND personalRating IS NOT NULL AND personalRating IS NOT 0")
    fun getAverageRating(): Flow<Float?>

    @Query("SELECT * FROM books WHERE status = 'FINISHED' AND startDate IS NOT NULL AND finishedDate IS NOT NULL")
    fun getFinishedBooksWithDates(): Flow<List<Book>>

    @Query("""
    SELECT T.name FROM tags AS T
    INNER JOIN book_tag_cross_ref AS BTR ON T.tagId = BTR.tagId
    INNER JOIN books AS B ON BTR.id = B.id
    WHERE B.status = 'FINISHED'
    GROUP BY T.name
    ORDER BY COUNT(T.name) DESC
    LIMIT 1
""")
    fun getFavoriteTag(): Flow<String?>

    @Query("SELECT format, COUNT(*) as count FROM books WHERE status = 'FINISHED' GROUP BY format")
    fun getBookCountByFormat(): Flow<List<FormatCount>>


    @Query("SELECT year FROM books WHERE status = 'FINISHED' AND year IS NOT NULL")
    fun getPublicationYearsOfFinishedBooks(): Flow<List<Int>>

    @Query("SELECT finishedDate FROM books WHERE status = 'FINISHED' AND finishedDate IS NOT NULL")
    fun getFinishDatesOfFinishedBooks(): Flow<List<Long>>

    @Query("SELECT * FROM books WHERE status = 'FINISHED' AND totalPages > 0 ORDER BY totalPages DESC LIMIT 1")
    fun getLongestBookByPages(): Flow<Book?>

    @Query("SELECT * FROM books WHERE status = 'FINISHED' AND totalPages > 0 ORDER BY totalPages ASC LIMIT 1")
    fun getShortestBookByPages(): Flow<Book?>

    @Query("SELECT * FROM books WHERE status = 'FINISHED' AND startDate IS NOT NULL AND finishedDate IS NOT NULL ORDER BY (finishedDate - startDate) DESC LIMIT 1")
    fun getLongestRead(): Flow<Book?>

    @Query("SELECT * FROM books WHERE status = 'FINISHED' AND startDate IS NOT NULL AND finishedDate IS NOT NULL ORDER BY (finishedDate - startDate) ASC LIMIT 1")
    fun getShortestRead(): Flow<Book?>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'FINISHED' AND finishedDate >= :startTimestamp AND finishedDate < :endTimestamp")
    fun getBooksReadBetween(startTimestamp: Long, endTimestamp: Long): Flow<Int>
}