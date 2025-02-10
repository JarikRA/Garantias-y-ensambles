package com.intesc.controldegarantiasyensambles.main.model

import java.util.Date

data class Service(
    val id: Int,
    val idCard: Int,
    val date: Date,
    val faultDescription: String,
    val cardFailure: String,
    val reasonForFailure: String?,
    val repair: String?
)
