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
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "book_database1"
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
        return status?.name
    }

    @TypeConverter
    fun toReadingStatus(value: String?): ReadingStatus? {
        return try {
            value?.let { ReadingStatus.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    @TypeConverter
    fun fromBookFormat(format: BookFormat?): String? {
        return format?.name
    }

    @TypeConverter
    fun toBookFormat(value: String?): BookFormat? {
        return value?.let { BookFormat.valueOf(it) }
    }
}