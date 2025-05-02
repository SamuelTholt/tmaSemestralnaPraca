package com.example.tmasemestralnapraca.teams

data class TeamModel (
    var id: String? = null,

    val position: Int = 0,
    val teamName: String = "",

    val teamImageLogoPath: String? = "",
    val publicId: String = "",


    val playedMatches: Int = 0,
    val goalsScored: Int = 0,
    val goalsConceded: Int = 0,
    val points: Int = 0
)