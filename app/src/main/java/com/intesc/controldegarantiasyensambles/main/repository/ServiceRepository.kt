package com.intesc.controldegarantiasyensambles.main.repository

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.ServiceDao
import com.intesc.controldegarantiasyensambles.main.model.Service

class ServiceRepository(private val serviceDao: ServiceDao) {

    suspend fun getServicesByCardId(cardId: Int): List<Service> {
        return serviceDao.getServicesByCardId(cardId)
    }

    suspend fun insertService(service: Service): Boolean {
        return serviceDao.insertService(service)
    }

    suspend fun insertRepairInLastServiceOfCard(idCard: Int, repair: String): Boolean {
        return serviceDao.insertRepairInLastServiceOfCard(idCard, repair)
    }
}