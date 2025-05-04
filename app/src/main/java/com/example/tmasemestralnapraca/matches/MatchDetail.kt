package com.example.tmasemestralnapraca.matches

import com.example.tmasemestralnapraca.matches.matchEvent.EventWithPlayer
import com.example.tmasemestralnapraca.matches.matchLineup.PlayerWithStats


data class MatchDetail(
    val match: MatchModel = MatchModel(),
    var events: List<EventWithPlayer> = emptyList()
)