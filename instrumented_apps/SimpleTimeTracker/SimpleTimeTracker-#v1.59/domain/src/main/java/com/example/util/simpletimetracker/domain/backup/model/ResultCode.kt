package com.example.util.simpletimetracker.domain.backup.model

sealed interface ResultCode {
    val message: String?

    data class Success(override val message: String?) : ResultCode
    data class Error(override val message: String?) : ResultCode
}