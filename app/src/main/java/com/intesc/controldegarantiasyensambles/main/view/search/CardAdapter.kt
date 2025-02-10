package com.intesc.controldegarantiasyensambles.main.view.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.intesc.controldegarantiasyensambles.databinding.CardItemListBinding
import com.intesc.controldegarantiasyensambles.main.model.Card

class CardAdapter(internal var cardList: List<Card>, private val onClickListener: (Card) -> Unit) :
    RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(private val binding: CardItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(card: Card) {
            binding.tvSerialNumber.text = card.serialNumber
            binding.tvCategory.text = "Categoria: ${card.category}"
            binding.tvDate.text = "Fecha de creación: ${card.creationDate}"
            itemView.setOnClickListener { onClickListener(card) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding =
            CardItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun getItemCount(): Int = cardList.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cardList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCards(newCardList: List<Card>) {
        cardList = newCardList
        notifyDataSetChanged()
    }
}
