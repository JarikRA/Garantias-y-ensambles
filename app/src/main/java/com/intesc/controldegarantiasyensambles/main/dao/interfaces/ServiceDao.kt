package com.intesc.controldegarantiasyensambles.main.dao.interfaces

import com.intesc.controldegarantiasyensambles.main.model.Service

interface ServiceDao {
    suspend fun getServicesByCardId(cardId: Int): List<Service>
    suspend fun insertService(service: Service): Boolean
    suspend fun insertRepairInLastServiceOfCard(idCard: Int, repair: String): Boolean
}