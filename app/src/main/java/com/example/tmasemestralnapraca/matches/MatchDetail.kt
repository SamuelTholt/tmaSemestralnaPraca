package com.example.tmasemestralnapraca.matches



data class MatchDetail(
    val match: MatchModel = MatchModel(),
    var startingLineup: List<PlayerWithStats> = emptyList(),
    var substitutes: List<PlayerWithStats> = emptyList(),
    var events: List<EventWithPlayer> = emptyList()
)