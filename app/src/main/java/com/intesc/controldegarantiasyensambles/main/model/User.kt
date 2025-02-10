package com.intesc.controldegarantiasyensambles.main.model

import java.io.Serializable

data class User(
    val id: Int,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val address: String,
    val zipCode: String
): Serializable
