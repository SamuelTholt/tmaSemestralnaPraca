package com.example.tmasemestralnapraca.matches



data class MatchDetail(
    val match: MatchModel = MatchModel(),
    val startingLineup: List<PlayerWithStats> = emptyList(),
    val substitutes: List<PlayerWithStats> = emptyList(),
    val events: List<EventWithPlayer> = emptyList()
)