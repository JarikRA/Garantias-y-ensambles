package com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.repository.ServiceRepository
import com.intesc.controldegarantiasyensambles.main.repository.UserRepository

class SharedTabViewModelFactory(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository,
    private val serviceRepository: ServiceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedTabViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedTabViewModel(
                userRepository,
                historyRepository,
                serviceRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}





