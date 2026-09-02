package com.example.util.simpletimetracker.domain.base

data class Coordinates(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {

    val width: Int get() = right - left
    val height: Int get() = bottom - top
}