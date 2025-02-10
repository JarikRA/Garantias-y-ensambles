package com.intesc.controldegarantiasyensambles.main.repository

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.CardDao
import com.intesc.controldegarantiasyensambles.main.model.Card

class CardRepository(private val cardDao: CardDao) {

    suspend fun insertCard(card: Card): Int {
        return cardDao.insertCard(card)
    }

    suspend fun insertCards(cards: List<Card>): List<Int> {
        return cardDao.insertCards(cards)
    }

    suspend fun getCardsByAdvanceSearch(
        idModel: Int?,
        month: String,
        year: String,
        category: String
    ): List<Card> {
        return cardDao.getCardsByAdvanceSearch(idModel, month, year, category)
    }

    suspend fun getLastSpecificSerialNumber(
        abbreviateCardModelName: String,
        month: Int,
        year: Int
    ): String? {
        return cardDao.getLastSpecificSerialNumber(abbreviateCardModelName, month, year)
    }

    suspend fun updateCard(card: Card): Boolean {
        return cardDao.updateCard(card)
    }

    suspend fun updateMultipleCards(cards: List<Card>): Boolean {
        return cardDao.updateMultipleCards(cards)
    }
}