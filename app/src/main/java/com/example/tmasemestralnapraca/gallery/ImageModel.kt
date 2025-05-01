package com.example.tmasemestralnapraca.gallery

data class ImageModel (
    var id: String? = null,
    val imagePath : String = "",
    val publicId: String = "",

    val imageDate: Long = System.currentTimeMillis()
)