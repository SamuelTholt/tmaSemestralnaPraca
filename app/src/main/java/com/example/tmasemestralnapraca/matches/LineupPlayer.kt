package com.example.tmasemestralnapraca.matches

data class LineupPlayer(
    var id: String? = null,
    val matchId: String = "",
    val playerId: String = "",
    val isStarting: Boolean = false, // true pre základnú zostavu, false pre náhradníkov
    val position: String = "",
    val orderNumber: Int = 0 // poradie v zostave
)