package com.intesc.controldegarantiasyensambles.main.repository

import com.intesc.controldegarantiasyensambles.main.dao.interfaces.UserDao
import com.intesc.controldegarantiasyensambles.main.model.User

class UserRepository(private val userDao: UserDao) {

    suspend fun login(pass: String): Boolean {
        val storedPassword = userDao.getAdminPassword()
        return storedPassword != null && storedPassword == pass
    }

    suspend fun getUserById(id: Int): User?{
        return userDao.getUserById(id)
    }

    suspend fun getAllUsers(): List<User> {
        return userDao.getAllUsers()
    }

    suspend fun insertUser(user: User): Int? {
        return userDao.insertUser(user)
    }

    suspend fun updateUser(user: User): Boolean {
        return userDao.updateUser(user)
    }
}