package com.intesc.controldegarantiasyensambles.main.view.cardstages.cardfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.intesc.controldegarantiasyensambles.databinding.FragmentHistoriesBinding
import com.intesc.controldegarantiasyensambles.main.model.History
import com.intesc.controldegarantiasyensambles.main.view.cardstages.adapters.HistoryAdapter
import com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel.SharedTabViewModel

class HistoriesFragment : Fragment() {

    private lateinit var binding: FragmentHistoriesBinding
    private lateinit var sharedTabViewModel: SharedTabViewModel
    private val historyAdapter = HistoryAdapter(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoriesBinding.inflate(inflater, container, false)
        sharedTabViewModel = ViewModelProvider(requireActivity())[SharedTabViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.rvHistories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun setupObservers() {
        sharedTabViewModel.histories.observe(viewLifecycleOwner) { histories ->
            if (histories.isNullOrEmpty()) {
                showLoading()
            } else {
                showHistories(histories)
            }
        }

        sharedTabViewModel.card.observe(viewLifecycleOwner) { card ->
            card?.let {
                if (sharedTabViewModel.histories.value.isNullOrEmpty()) {
                    sharedTabViewModel.loadHistories(it.id)
                }
            }
        }
    }

    private fun showLoading() {
        binding.apply {
            pbHistories.visibility = View.VISIBLE
            rvHistories.visibility = View.GONE
        }
    }

    private fun showHistories(histories: List<History>) {
        binding.apply {
            pbHistories.visibility = View.GONE
            rvHistories.visibility = View.VISIBLE
        }
        historyAdapter.setHistories(histories)
    }
}
