package com.intesc.controldegarantiasyensambles.main.model

import java.io.Serializable
import java.util.Date

data class Card(
    val id: Int,
    val idModel: Int?,
    val serialNumber: String,
    val creationDate: Date,
    val updateDate: Date,
    val status: String,
    val subStatus: String?,
    val category: String,
    val userId: Int?
): Serializable



