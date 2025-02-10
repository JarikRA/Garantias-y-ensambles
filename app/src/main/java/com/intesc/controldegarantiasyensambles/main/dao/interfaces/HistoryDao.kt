package com.intesc.controldegarantiasyensambles.main.dao.interfaces

import com.intesc.controldegarantiasyensambles.main.model.History

interface HistoryDao {
    suspend fun getHistoriesByCardId(cardId: Int): List<History>
    suspend fun insertHistory(history: History): Boolean
    suspend fun insertHistories(histories: List<History>): Boolean
}