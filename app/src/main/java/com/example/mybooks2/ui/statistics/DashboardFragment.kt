package com.example.mybooks2.ui.statistics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.example.mybooks2.R
import com.example.mybooks2.databinding.FragmentDashboardBinding
import com.example.mybooks2.ui.addBook2.BookFormat
import com.example.mybooks2.ui.detailScreen.DetailActivity
import com.example.mybooks2.ui.setting.SettingsActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.concurrent.TimeUnit
import kotlin.getValue

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by activityViewModels { DashboardViewModel.factory }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observeViewModel()
        setupClickListeners()



    }
    private fun observeViewModel() {
        viewModel.totalBooksRead.observe(viewLifecycleOwner) { count ->
            binding.textTotalBooksRead.text = count.toString()
        }

        viewModel.totalPagesRead.observe(viewLifecycleOwner) { total ->
            binding.textTotalPagesRead.text = formatLargeNumber(total?:0)
        }

        viewModel.booksInProgress.observe(viewLifecycleOwner) { count ->
            binding.textBooksInProgress.text = "Currently reading: $count books"
        }

        viewModel.averageRating.observe(viewLifecycleOwner) { avg ->
            val avgRating = avg ?: 0f
            binding.textAvgRating.text = "Average rating: ${String.format("%.1f", avgRating)} stars"
        }

        viewModel.averageReadingTime.observe(viewLifecycleOwner) { days ->
            binding.textAvgTime.text = "Average time to finish: $days days"
        }
        viewModel.favoriteTag.observe(viewLifecycleOwner) { tag ->
            binding.textFavoriteTag.text = "Favorite genre: ${tag ?: "N/A"}"
        }
        viewModel.booksByDecade.observe(viewLifecycleOwner) { decadeText ->
            binding.textBooksByDecade.text = decadeText
        }
        viewModel.booksReadPerMonth.observe(viewLifecycleOwner) { entries ->
            if (entries.isNotEmpty()) {
                setupMonthlyChart(entries)
            }
        }
        viewModel.formatBreakdownForChart.observe(viewLifecycleOwner) { entries ->
            if (entries.isNotEmpty()) {
                setupFormatPieChart(entries)
            }
        }
        viewModel.longestBookByPages.observe(viewLifecycleOwner) { book ->
            if (book != null) {
                binding.layoutLongestBook.visibility = View.VISIBLE
                binding.textLongestBookTitle.text = book.title
                binding.textLongestBookValue.text = "${book.totalPages} pages"
            } else {
                binding.layoutLongestBook.visibility = View.GONE
            }
        }

        viewModel.shortestBookByPages.observe(viewLifecycleOwner) { book ->
            if (book != null) {
                binding.layoutShortestBook.visibility = View.VISIBLE
                binding.textShortestBookTitle.text = book.title
                binding.textShortestBookValue.text = "${book.totalPages} pages"
            } else {
                binding.layoutShortestBook.visibility = View.GONE
            }
        }
        viewModel.longestRead.observe(viewLifecycleOwner) {
            book ->
            if (book != null && book.startDate != null && book.finishedDate != null) {
                binding.layoutLongestBookTime.visibility = View.VISIBLE
                binding.textLongestTimeBookTitle.text = book.title

                val durationInMillis = book.finishedDate - book.startDate

                val durationInDays = TimeUnit.MILLISECONDS.toDays(durationInMillis) + 1
                binding.textLongestTimeBookValue.text = "$durationInDays days"
            } else {
                binding.layoutLongestBookTime.visibility = View.GONE
            }
        }
        viewModel.shortestRead.observe(viewLifecycleOwner){
            book ->
            if (book != null && book.startDate != null && book.finishedDate != null) {
                binding.layoutShortestBookTime.visibility = View.VISIBLE

                val durationInMillis = book.finishedDate - book.startDate

                val durationInDays = TimeUnit.MILLISECONDS.toDays(durationInMillis) + 1

                binding.textShortestTimeBookTitle.text = book.title
                binding.textShortestTimeBookValue.text = "$durationInDays days"
            } else {
                binding.layoutShortestBookTime.visibility = View.GONE
            }
        }

        viewModel.goalProgress.observe(viewLifecycleOwner) { (current, goal) ->
            val progressPercent = if (goal > 0) (current * 100 / goal) else 0
            binding.progressGoal.setProgress(progressPercent,true)
            binding.textGoalProgress.text = current.toString()
            binding.textGoalLabel.text = "books read out of $goal"
        }
    }
    private fun setupClickListeners() {
        val detailIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* ... */ }

        binding.layoutLongestBook.setOnClickListener {
            viewModel.longestBookByPages.value?.let { book ->
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("EXTRA_BOOK_ID", book.id)
                }
                detailIntentLauncher.launch(intent)
            }
        }

        binding.layoutShortestBook.setOnClickListener {
            viewModel.shortestBookByPages.value?.let { book ->
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("EXTRA_BOOK_ID", book.id)
                }
                detailIntentLauncher.launch(intent)
            }
        }
        binding.layoutShortestBookTime.setOnClickListener {
            viewModel.shortestRead.value?.let { book ->
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("EXTRA_BOOK_ID", book.id)
                }
                detailIntentLauncher.launch(intent)
            }
        }
        binding.layoutLongestBookTime.setOnClickListener {
            viewModel.longestRead.value?.let { book ->
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("EXTRA_BOOK_ID", book.id)
                }
                detailIntentLauncher.launch(intent)
            }
        }

        parentFragmentManager.setFragmentResultListener(SetGoalDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val newGoalStr = bundle.getString(SetGoalDialogFragment.RESULT_KEY)
            if (newGoalStr != null) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                prefs.edit().putString("yearly_reading_goal", newGoalStr).apply()
            }
        }

        binding.buttonSetGoal.setOnClickListener {
            SetGoalDialogFragment.newInstance().show(parentFragmentManager, SetGoalDialogFragment.TAG)
        }

    }

    private fun setupFormatPieChart(entries: List<PieEntry>) {
        val pieChart = binding.pieChartFormat

        val dataSet = PieDataSet(entries, "Book Formats")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface)

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface))

        pieChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface)

        pieChart.invalidate()
    }
    private fun setupMonthlyChart(entries: List<BarEntry>) {
        val barChart = binding.barChartMonthly

        val dataSet = BarDataSet(entries, "Books Read")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.md_theme_primary)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface)


        barChart.data = BarData(dataSet)

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.setFitBars(true)

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setDrawGridLines(false)

        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f

        barChart.invalidate()
    }
    private fun formatLargeNumber(number: Long): String {
        return when {
            number < 1000 -> number.toString()
            number < 1_000_000 -> String.format("%.1fk", number / 1000.0)
            else -> String.format("%.1fM", number / 1_000_000.0)
        }
    }
    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.myToolbar)
        setHasOptionsMenu(true)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard_menu, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
                true
            }
            else ->super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
data class FormatCount(
    val format: BookFormat,
    val count: Int
)
