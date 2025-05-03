package com.example.tmasemestralnapraca.matches.matchLineup

data class LineupPlayer(
    var id: String? = null,
    val matchId: String = "",
    val playerId: String = "",
    val isStarting: Boolean = true,
    val minutesIn: Int? = null,
    val minutesOut: Int? = null
)