package com.intesc.controldegarantiasyensambles.main.dao.interfaces

import com.intesc.controldegarantiasyensambles.main.model.CardModel

interface CardModelDao {
    suspend fun getAllCardModels(): List<CardModel>
    suspend fun getCarModelById(id: Int): CardModel?
    suspend fun insertCardModel(cardModel: CardModel): Boolean
    suspend fun updateCardModel(cardModel: CardModel): Boolean
    suspend fun deleteCardModel(id: Int): Boolean
}