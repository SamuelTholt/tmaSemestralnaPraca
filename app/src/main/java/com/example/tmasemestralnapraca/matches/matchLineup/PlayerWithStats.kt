package com.example.tmasemestralnapraca.matches.matchLineup

import com.example.tmasemestralnapraca.player.PlayerModel

data class PlayerWithStats(
    val player: PlayerModel,
    var goals: Int = 0,
    val assists: Int = 0,
    var yellowCards: Int = 0,
    var redCards: Int = 0,
    val minutesIn: Int? = null,  // kedy prišiel na ihrisko
    val minutesOut: Int? = null  // kedy odišiel z ihriska
)