package ru.cs.roadcheck.auth

import org.springframework.security.core.Authentication

fun Authentication.userId(): Long = principal as Long

fun Authentication.isModerator(): Boolean =
    authorities.any { it.authority == "ROLE_MODERATOR" }
