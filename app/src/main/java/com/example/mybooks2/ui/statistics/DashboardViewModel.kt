package com.example.mybooks2.ui.statistics

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.preference.PreferenceManager
import com.example.mybooks2.MyBooksApplication
import com.example.mybooks2.data.BookDao
import com.example.mybooks2.model.Book
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DashboardViewModel(bookDao: BookDao,application: MyBooksApplication) : ViewModel() {

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)

    val totalBooksRead: LiveData<Int> = bookDao.getTotalBooksRead().asLiveData()
    val totalPagesRead: LiveData<Long?> = bookDao.getTotalPagesRead().asLiveData()
    val booksInProgress: LiveData<Int> = bookDao.getBooksInProgressCount().asLiveData()
    val averageRating: LiveData<Float?> = bookDao.getAverageRating().asLiveData()
    val favoriteTag: LiveData<String?> = bookDao.getFavoriteTag().asLiveData()
    val formatBreakdown: LiveData<List<FormatCount>> = bookDao.getBookCountByFormat().asLiveData()

    val yearlyGoal: LiveData<Int> =
        SharedPreferenceLiveData(prefs, "yearly_reading_goal", 25)

    val booksReadThisYear: LiveData<Int> = bookDao.getBooksReadBetween(
        getStartOfYearTimestamp(),
        getStartOfNextYearTimestamp()
    ).asLiveData()

    private fun getStartOfYearTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfNextYearTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, 1)
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        return calendar.timeInMillis
    }

    val goalProgress: LiveData<Pair<Int, Int>> = MediatorLiveData<Pair<Int, Int>>().apply {
        var currentBooks = 0
        var currentGoal = 25

        addSource(booksReadThisYear) { books ->
            currentBooks = books
            value = Pair(currentBooks, currentGoal)
        }
        addSource(yearlyGoal) { goal ->
            currentGoal = goal
            value = Pair(currentBooks, currentGoal)
        }
    }
        val formatBreakdownForChart: LiveData<List<PieEntry>> =
            formatBreakdown.map { breakdownList ->
                breakdownList.map { formatCount ->
                    PieEntry(formatCount.count.toFloat(), formatCount.format.displayName)
                }
            }

        val booksByDecade: LiveData<String> =
            bookDao.getPublicationYearsOfFinishedBooks().map { years ->
                val decadeCounts = years.groupBy { (it / 10) * 10 }
                    .mapValues { it.value.size }
                    .toSortedMap()

                decadeCounts.entries.joinToString("\n") { "${it.key}s: ${it.value} books" }
            }.asLiveData()


        val booksReadPerMonth: LiveData<List<BarEntry>> =
            bookDao.getFinishDatesOfFinishedBooks().map { timestamps ->
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)

                val monthlyCounts = timestamps.map {
                    calendar.timeInMillis = it
                    calendar
                }.filter {
                    it.get(Calendar.YEAR) == currentYear
                }.groupBy {
                    it.get(Calendar.MONTH)
                }.mapValues {
                    it.value.size
                }
                val entries = mutableListOf<BarEntry>()
                for (month in 0..11) {
                    val count = monthlyCounts[month]?.toFloat() ?: 0f
                    entries.add(BarEntry(month.toFloat(), count))
                }
                entries
            }.asLiveData()

        val averageReadingTime: LiveData<Long> =
            bookDao.getFinishedBooksWithDates().map { finishedBooks ->
                if (finishedBooks.isEmpty()) {
                    0L
                } else {
                    val totalTimeInMillis =
                        finishedBooks.sumOf { it.finishedDate!! - it.startDate!! }
                    val averageTimeInMillis = totalTimeInMillis / finishedBooks.size
                    TimeUnit.MILLISECONDS.toDays(averageTimeInMillis)
                }
            }.asLiveData()

        val longestBookByPages: LiveData<Book?> = bookDao.getLongestBookByPages().asLiveData()
        val shortestBookByPages: LiveData<Book?> = bookDao.getShortestBookByPages().asLiveData()
        val longestRead: LiveData<Book?> = bookDao.getLongestRead().asLiveData()
        val shortestRead: LiveData<Book?> = bookDao.getShortestRead().asLiveData()

        companion object {
        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyBooksApplication
                DashboardViewModel(application.database.bookDao(), application)
            }
        }
    }

}

    class SharedPreferenceLiveData(
        private val prefs: SharedPreferences,
        private val key: String,
        private val defaultValue: Int
    ) : LiveData<Int>() {
        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                updateValue()
            }
        }

        override fun onActive() {
            super.onActive()
            updateValue()
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onInactive() {
            super.onInactive()
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }

        private fun updateValue() {
            value = try {
                prefs.getString(key, defaultValue.toString())?.toInt() ?: defaultValue
            } catch (e: NumberFormatException) {
                defaultValue
            }
        }
    }