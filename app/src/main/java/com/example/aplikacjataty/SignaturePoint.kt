package com.example.aplikacjataty

data class SignaturePoint(
    val time: Long,
    val x: Float,
    val y: Float,
    val pressure: Float,
    val eventType: String,
    val toolType: String,
    val tilt: Float,
    val orientation: Float,
    val distance: Float
)
