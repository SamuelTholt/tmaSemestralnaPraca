package com.example.tmasemestralnapraca.matches

import com.example.tmasemestralnapraca.player.PlayerModel

data class EventWithPlayer(
    var id: String? = null,
    val event: MatchEvent,
    val player: PlayerModel,
    val assistPlayer: PlayerModel? = null
)