package com.intesc.controldegarantiasyensambles.main.repository

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.HistoryDao
import com.intesc.controldegarantiasyensambles.main.model.History

class HistoryRepository(private val historyDao: HistoryDao) {

    suspend fun insertHistory(history: History): Boolean {
        return historyDao.insertHistory(history)
    }

    suspend fun getHistoriesByCardId(cardId: Int): List<History> {
        return historyDao.getHistoriesByCardId(cardId)
    }

    suspend fun insertHistories(histories: List<History>): Boolean {
        return historyDao.insertHistories(histories)
    }
}