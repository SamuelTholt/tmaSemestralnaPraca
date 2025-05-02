package com.example.tmasemestralnapraca.matches

import com.example.tmasemestralnapraca.player.PlayerModel

data class PlayerWithStats(
    val player: PlayerModel,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val minutesIn: Int? = null,  // kedy prišiel na ihrisko
    val minutesOut: Int? = null  // kedy odišiel z ihriska
)