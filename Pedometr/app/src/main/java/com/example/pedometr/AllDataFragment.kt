package com.example.pedometr

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pedometr.data.StepViewModel
import com.example.pedometr.data.StepsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class AllDataFragment : Fragment() {

    private val viewModel: StepViewModel by viewModels()
    private lateinit var stepsRecyclerView: RecyclerView
    private val stepsAdapter = StepsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_data, container, false)

        stepsRecyclerView = view.findViewById(R.id.stepsRecyclerView)
        stepsRecyclerView.layoutManager = LinearLayoutManager(context)
        stepsRecyclerView.adapter = stepsAdapter
        loadStepsData()
        return view
    }

    private fun loadStepsData() {
        val stepsData = runBlocking {
            viewModel.getAllSteps().first()
        }
        stepsAdapter.submitList(stepsData)
    }


}