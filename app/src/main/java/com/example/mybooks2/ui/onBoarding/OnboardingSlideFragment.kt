package com.example.mybooks2.ui.onBoarding

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mybooks2.R // Use your R file
import com.example.mybooks2.databinding.FragmentOnboardingSlideBinding

class OnboardingSlideFragment : Fragment() {

    private var _binding: FragmentOnboardingSlideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingSlideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
             binding.slideImage.setImageResource(it.getInt(ARG_IMAGE_RES)) // Pass image resource if needed
            binding.slideTitle.text = it.getString(ARG_TITLE)
            binding.slideDescription.text = it.getString(ARG_DESCRIPTION)
            val drawable = binding.slideImage.drawable
            if (drawable is Animatable) { // Check if it's an animated drawable
                (drawable as Animatable).start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
         private const val ARG_IMAGE_RES = "image_res"

        fun newInstance(title: String, description: String,imageRes: Int): OnboardingSlideFragment {
            return OnboardingSlideFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_DESCRIPTION, description)
                     putInt(ARG_IMAGE_RES, imageRes)
                }
            }
        }
    }
}