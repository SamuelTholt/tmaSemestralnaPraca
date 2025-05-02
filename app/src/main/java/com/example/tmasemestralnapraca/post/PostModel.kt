package com.example.tmasemestralnapraca.post

data class PostModel (
    var id: String? = null,
    val postHeader: String = "",
    val postText: String = "",
    val postDate: Long = System.currentTimeMillis()
)