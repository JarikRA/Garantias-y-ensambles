package com.intesc.controldegarantiasyensambles.main.repository

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.CardModelDao
import com.intesc.controldegarantiasyensambles.main.model.CardModel

class CardModelRepository(private val cardModelDao: CardModelDao) {

    suspend fun getAllCardModels(): List<CardModel> {
        return cardModelDao.getAllCardModels()
    }

    suspend fun getCarModelById(id: Int): CardModel? {
        return cardModelDao.getCarModelById(id)
    }

    suspend fun insertCardModel(cardModel: CardModel): Boolean {
        return cardModelDao.insertCardModel(cardModel)
    }

    suspend fun updateCardModel(cardModel: CardModel): Boolean {
        return cardModelDao.updateCardModel(cardModel)
    }

    suspend fun deleteCardModel(id: Int): Boolean {
        return cardModelDao.deleteCardModel(id)
    }
}
