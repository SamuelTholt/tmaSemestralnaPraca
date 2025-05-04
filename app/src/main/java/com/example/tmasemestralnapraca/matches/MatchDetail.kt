package com.example.tmasemestralnapraca.matches

import com.example.tmasemestralnapraca.matches.matchEvent.EventWithPlayer


data class MatchDetail(
    val match: MatchModel = MatchModel(),
    var events: List<EventWithPlayer> = emptyList()
)