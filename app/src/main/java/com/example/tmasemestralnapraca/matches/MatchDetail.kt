package com.example.tmasemestralnapraca.matches

import com.example.tmasemestralnapraca.matches.matchEvent.EventWithPlayer
import com.example.tmasemestralnapraca.matches.matchLineup.PlayerWithStats


data class MatchDetail(
    val match: MatchModel = MatchModel(),
    var startingLineup: List<PlayerWithStats> = emptyList(),
    var substitutes: List<PlayerWithStats> = emptyList(),
    var events: List<EventWithPlayer> = emptyList()
)