package com.intesc.controldegarantiasyensambles.main.view.cardstages.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.intesc.controldegarantiasyensambles.databinding.HistoryItemListBinding
import com.intesc.controldegarantiasyensambles.main.model.History

class HistoryAdapter(private var historyList: List<History>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(private val binding: HistoryItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(history: History) {
            binding.tvDate.text = "Fecha: ${history.date}"
            binding.tvCategory.text = "Categoria: ${
                history.category.ifEmpty { "Sin categoría" }
            }"
            binding.tvAssemblerName.text = "Nombre del responsable: ${
                history.assemblerName.ifEmpty { "Sin ensamblador" }
            }"
            binding.tvComments.text = "Comentarios: ${
                history.record.ifEmpty { "Sin comentarios" }
            }"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = HistoryItemListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun getItemCount(): Int = historyList.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setHistories(newHistoryList: List<History>) {
        historyList = newHistoryList
        notifyDataSetChanged()
    }
}
