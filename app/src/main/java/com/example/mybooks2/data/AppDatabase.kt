package com.example.mybooks2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.mybooks2.model.Book
import com.example.mybooks2.model.BookTagCrossRef
import com.example.mybooks2.model.BookWithTags
import com.example.mybooks2.model.Tag
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.addBook2.ReadingStatus

@Database(entities = [Book::class, Tag::class, BookTagCrossRef::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        // Volatile ensures that the instance is always up-to-date and the same for all execution threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Return the existing instance if it's not null, otherwise create a new one.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "book_database1" // The name of your database file
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
class Converters {
    @TypeConverter
    fun fromReadingStatus(status: ReadingStatus?): String? {
        // Convert the enum to its string name (e.g., ReadingStatus.FINISHED -> "FINISHED")
        return status?.name
    }

    @TypeConverter
    fun toReadingStatus(value: String?): ReadingStatus? {
        return try {
            value?.let { ReadingStatus.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            null // Or return a default value like ReadingStatus.UNFINISHED
        }
    }
    @TypeConverter
    fun fromBookFormat(format: BookFormat?): String? {
        return format?.name // Converts enum to string (e.g., "PAPERBACK")
    }

    @TypeConverter
    fun toBookFormat(value: String?): BookFormat? {
        return value?.let { BookFormat.valueOf(it) }
    }
}