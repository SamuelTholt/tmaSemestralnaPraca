package com.example.tmasemestralnapraca.matches.matchEvent


data class MatchEvent(
    var id: String? = null,
    val matchId: String = "",
    val playerId: String = "",
    val eventType: EventType = EventType.GOAL,
    val minute: Int = 0,
    val playerAssistId: String? = null
)