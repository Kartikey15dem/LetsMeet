package com.myworldtech.meet.presentation.model

data class Participant(
    val id: String,
    val name: String,
    val photoUrl: String,
    val videoEnabled: Boolean = true,
    val audioEnabled: Boolean = true
)