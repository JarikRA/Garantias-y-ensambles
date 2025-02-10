package com.intesc.controldegarantiasyensambles.main.dao.interfaces

import com.intesc.controldegarantiasyensambles.main.model.Card

interface CardDao {

    suspend fun getLastSpecificSerialNumber(
        abbreviateCardModelName: String,
        month: Int,
        year: Int
    ): String?

    suspend fun getCardsByAdvanceSearch(
        idModel: Int?,
        month: String,
        year: String,
        category: String
    ): List<Card>

    suspend fun insertCard(card: Card): Int

    suspend fun insertCards(cards: List<Card>): List<Int>
    suspend fun updateCard(card: Card): Boolean

    suspend fun updateMultipleCards(cards: List<Card>): Boolean
}


