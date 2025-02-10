package com.intesc.controldegarantiasyensambles.main.view.cardmodels

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.intesc.controldegarantiasyensambles.databinding.CardModelItemListBinding
import com.intesc.controldegarantiasyensambles.main.model.CardModel

class CardModelAdapter(
    private var cardModelList: MutableList<CardModel>,
    private val onEditClick: (CardModel) -> Unit,
    private val onDeleteClick: (CardModel) -> Unit
) : RecyclerView.Adapter<CardModelAdapter.CardModelViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        cardModelList.clear()
        notifyDataSetChanged()
    }

    inner class CardModelViewHolder(private val binding: CardModelItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cardModel: CardModel) {
            binding.tvModelName.text = cardModel.modelName
            binding.btnEdit.setOnClickListener { onEditClick(cardModel) }
            binding.btnDelete.setOnClickListener { onDeleteClick(cardModel) }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newCardModels: MutableList<CardModel>) {
        cardModelList.clear()
        cardModelList.addAll(newCardModels)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardModelViewHolder {
        val binding =
            CardModelItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardModelViewHolder(binding)
    }

    override fun getItemCount(): Int = cardModelList.size

    override fun onBindViewHolder(holder: CardModelViewHolder, position: Int) {
        holder.bind(cardModelList[position])
    }

    fun removeCardModel(cardModel: CardModel) {
        val position = cardModelList.indexOf(cardModel)
        if (position != -1) {
            cardModelList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateCardModel(updatedCardModel: CardModel) {
        val position = cardModelList.indexOfFirst { it.id == updatedCardModel.id }
        if (position != -1) {
            cardModelList[position] = updatedCardModel
            notifyItemChanged(position)
        }
    }
}
