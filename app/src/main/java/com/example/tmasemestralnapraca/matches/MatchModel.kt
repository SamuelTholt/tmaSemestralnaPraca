package com.example.tmasemestralnapraca.matches

data class MatchModel (
    var id: String? = null,
    val opponentTeamId: String = "",
    val opponentName: String = "",
    val opponentLogo: String = "",
    val ourScore: Int = 0,
    val opponentScore: Int = 0,
    val date: String = "",
    val played: Boolean = false,
    val playedHome: Boolean = true
)