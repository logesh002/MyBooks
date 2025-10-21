package com.example.mybooks2.ui.onBoarding

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mybooks2.MainActivity
import com.example.mybooks2.R
import com.example.mybooks2.databinding.ActivityOnboardingBinding
import com.example.mybooks2.ui.detailScreen.BookDetailViewModel
import com.example.mybooks2.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue

class OnboardingActivity : AppCompatActivity(),OnboardingSampleDataFragment.OnboardingActionListener {

    private lateinit var binding: ActivityOnboardingBinding
    val viewModel by viewModels<OnBoardingViewModel> { OnBoardingViewModel.Companion.factory }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter

        binding.dotsIndicator.attachTo(binding.viewPager)

        // --- Button Logic ---
        binding.buttonSkip.setOnClickListener { finishOnboarding() }

        binding.buttonNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < adapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                finishOnboarding()
                // We're on the last slide (Sample Data fragment)
                // The finish logic is handled by the fragment's listener now.
                // You could optionally trigger the finish here too if needed,
                // but it's better handled by the fragment's buttons.
            }
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == adapter.itemCount - 1) { // Last page
                    binding.buttonNext.visibility = View.INVISIBLE
                    binding.buttonSkip.visibility=View.INVISIBLE
                } else {
                 //   binding.buttonNext.text = "Next"
                    binding.buttonNext.visibility = View.VISIBLE
                    binding.buttonSkip.visibility=View.VISIBLE
                    // binding.buttonSkip.visibility = View.VISIBLE
                }
            }
        })

        val window = window
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = decorView.windowInsetsController
            if (controller != null) {
                if (!isDarkTheme()) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        }
    }

    override fun onSampleDataAdded() {
        lifecycleScope.launch {
            // Show a loading indicator here if desired

            binding.progressOverlay.visibility = View.VISIBLE
            // 1. Call the ViewModel function and get the Job
            val insertionJob = viewModel.addSampleBooks(this@OnboardingActivity)

            insertionJob.join()
            binding.progressOverlay.visibility = View.GONE

            // 3. Once complete, switch back to the main thread to finish
            withContext(Dispatchers.Main) {
                // Hide loading indicator here
                finishOnboarding()
            }
        }
      //  viewModel.addSampleBooks(this)
        //finishOnboarding()
    }

    override fun onSampleDataSkipped() {
        finishOnboarding()
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 4 // Number of slides

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OnboardingSlideFragment.newInstance("Track Your Reading",
                    "Welcome! Effortlessly track every book you read.",
                    R.drawable.house_bookshelves_animate
                )
                1 -> OnboardingSlideFragment.newInstance("Organize Your Shelves",
                    "See your library at a glance. Easily manage books by status: Reading, Finished, To Be Read, or Dropped.",
                    R.drawable.learning_animate)
                2 -> OnboardingSlideFragment.newInstance("Add Books Instantly",
                    "Quickly add new books by searching online with just a title or ISBN.",R.drawable.web_search_animate)
                3 -> OnboardingSampleDataFragment.newInstance()
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
    fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

}