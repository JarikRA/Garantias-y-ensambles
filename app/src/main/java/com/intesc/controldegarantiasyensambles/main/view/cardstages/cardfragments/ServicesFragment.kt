package com.intesc.controldegarantiasyensambles.main.view.cardstages.cardfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.intesc.controldegarantiasyensambles.databinding.FragmentServicesBinding
import com.intesc.controldegarantiasyensambles.main.model.Service
import com.intesc.controldegarantiasyensambles.main.view.cardstages.adapters.ServiceAdapter
import com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel.SharedTabViewModel

class ServicesFragment : Fragment() {

    private lateinit var binding: FragmentServicesBinding
    private lateinit var sharedTabViewModel: SharedTabViewModel
    private val serviceAdapter = ServiceAdapter(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentServicesBinding.inflate(inflater, container, false)
        sharedTabViewModel = ViewModelProvider(requireActivity())[SharedTabViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.rvServices.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = serviceAdapter
        }
    }

    private fun setupObservers() {
        sharedTabViewModel.services.observe(viewLifecycleOwner) { services ->
            when {
                services == null -> showLoading()
                services.isEmpty() -> showEmptyStatus()
                else -> showServices(services)
            }
        }

        sharedTabViewModel.card.observe(viewLifecycleOwner) { card ->
            card?.let {
                if (sharedTabViewModel.services.value == null) {
                    sharedTabViewModel.loadServices(it.id)
                }
            }
        }
    }

    private fun showLoading() {
        binding.apply {
            pbServices.visibility = View.VISIBLE
            rvServices.visibility = View.GONE
            imPet.visibility = View.GONE
            tvServiceListInfo.visibility = View.GONE
        }
    }

    private fun showEmptyStatus() {
        binding.apply {
            pbServices.visibility = View.GONE
            rvServices.visibility = View.GONE
            imPet.visibility = View.VISIBLE
            tvServiceListInfo.visibility = View.VISIBLE
        }
    }

    private fun showServices(services: List<Service>) {
        binding.apply {
            pbServices.visibility = View.GONE
            rvServices.visibility = View.VISIBLE
            imPet.visibility = View.GONE
            tvServiceListInfo.visibility = View.GONE
        }
        serviceAdapter.setServices(services)
    }
}

