package com.example.tmasemestralnapraca.matches.matchLineup

data class LineupPlayer(
    var id: String? = null,
    val matchId: String = "",
    val playerId: String = "",
    val isStarting: Boolean = false,
    val position: String = "",
    val orderNumber: Int = 0
)