package com.intesc.controldegarantiasyensambles.main.dao.interfaces

import com.intesc.controldegarantiasyensambles.main.model.User

interface UserDao {
    suspend fun getAllUsers(): List<User>
    suspend fun getUserById(id: Int): User?
    suspend fun insertUser(user: User): Int?
    suspend fun updateUser(user: User): Boolean
    suspend fun getAdminPassword(): String?
}