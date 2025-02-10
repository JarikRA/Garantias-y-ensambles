package com.intesc.controldegarantiasyensambles.main.view.cardstages.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.intesc.controldegarantiasyensambles.databinding.ServiceItemListBinding
import com.intesc.controldegarantiasyensambles.main.model.Service

class ServiceAdapter(private var serviceList: List<Service>) :
    RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(private val binding: ServiceItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(service: Service) {
            binding.tvDate.text = "Fecha: ${service.date}"
            binding.tvFaultDescription.text = "Descripción de la falla: ${service.faultDescription}"
            binding.tvRepair.text = "Reparación: ${service.repair ?: "Sin reparación hasta ahora"}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ServiceItemListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ServiceViewHolder(binding)
    }

    override fun getItemCount(): Int = serviceList.size

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(serviceList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setServices(newServiceList: List<Service>) {
        serviceList = newServiceList
        notifyDataSetChanged()
    }
}
