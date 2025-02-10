package com.intesc.controldegarantiasyensambles.main.view.cardstages.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intesc.controldegarantiasyensambles.main.model.Card
import com.intesc.controldegarantiasyensambles.main.model.History
import com.intesc.controldegarantiasyensambles.main.model.Service
import com.intesc.controldegarantiasyensambles.main.model.User
import com.intesc.controldegarantiasyensambles.main.repository.HistoryRepository
import com.intesc.controldegarantiasyensambles.main.repository.ServiceRepository
import com.intesc.controldegarantiasyensambles.main.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SharedTabViewModel(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository,
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _card = MutableLiveData<Card>()
    val card: LiveData<Card> get() = _card

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _histories = MutableLiveData<List<History>>()
    val histories: LiveData<List<History>> get() = _histories

    private val _services = MutableLiveData<List<Service>>()
    val services: LiveData<List<Service>> get() = _services

    fun setCard(newCard: Card) {
        _card.value = newCard
        if (newCard.userId != null) {
            loadUser(newCard.userId)
        } else {
            _user.postValue(null)
        }
    }

    private fun loadUser(id: Int) {
        if (_user.value != null) return
        viewModelScope.launch {
            try {
                val user = withContext(Dispatchers.IO) { userRepository.getUserById(id) }
                _user.postValue(user)
            } catch (e: Exception) {
                Timber.e(e, "Error loading user with ID: $id")
            }
        }
    }

    fun loadHistories(cardId: Int) {
        if (_histories.value.isNullOrEmpty()) {
            viewModelScope.launch {
                try {
                    val historyList = withContext(Dispatchers.IO) {
                        historyRepository.getHistoriesByCardId(cardId)
                    }
                    _histories.postValue(historyList)
                } catch (e: Exception) {
                    Timber.e(e, "Error loading histories for card ID: $cardId")
                }
            }
        }
    }

    fun loadServices(cardId: Int) {
        if (_services.value.isNullOrEmpty()) {
            viewModelScope.launch {
                try {
                    val serviceList = withContext(Dispatchers.IO) {
                        serviceRepository.getServicesByCardId(cardId)
                    }
                    _services.postValue(serviceList)
                } catch (e: Exception) {
                    Timber.e(e, "Error loading services for card ID: $cardId")
                }
            }
        }
    }

    fun cancelJobs() {
        viewModelScope.cancel()
    }
}
