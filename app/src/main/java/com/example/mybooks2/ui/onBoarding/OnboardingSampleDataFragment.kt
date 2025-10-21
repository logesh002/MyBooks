package com.example.mybooks2.ui.onBoarding

import android.content.Context
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mybooks2.databinding.FragmentOnboardingSampleDataBinding

class OnboardingSampleDataFragment : Fragment() {

    private var _binding: FragmentOnboardingSampleDataBinding? = null
    private val binding get() = _binding!!

    private var listener: OnboardingActionListener? = null

    interface OnboardingActionListener {
        fun onSampleDataAdded()
        fun onSampleDataSkipped()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnboardingActionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnboardingActionListener")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingSampleDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonAddSampleData.setOnClickListener {
            listener?.onSampleDataAdded()
        }

        binding.buttonSkipSampleData.setOnClickListener {
            listener?.onSampleDataSkipped()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        fun newInstance(): OnboardingSampleDataFragment {
            return OnboardingSampleDataFragment()
        }
    }
}