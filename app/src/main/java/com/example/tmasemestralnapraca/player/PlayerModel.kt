package com.example.tmasemestralnapraca.player

data class PlayerModel (
    var id: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val numberOfShirt: Int = 0,
    val position: String = "",
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val minutesPlayed: Int = 0
)