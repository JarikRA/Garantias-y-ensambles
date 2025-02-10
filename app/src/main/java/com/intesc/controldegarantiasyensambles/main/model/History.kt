package com.intesc.controldegarantiasyensambles.main.model

import java.util.Date

data class History(
    val id: Int,
    val idCard: Int,
    val date: Date,
    val status: String,
    val assemblerName: String,
    val record: String,
    val category: String
)
