package com.example.gymrank.domain.model

data class Gym(
    val id: String,
    val name: String,
    val city: String
) {
    val initials: String
        get() = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
}
