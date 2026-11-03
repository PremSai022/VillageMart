package com.mart.villagemart.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

fun Throwable.userMessage(json: Json): String = when (this) {
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val parsed = body?.let {
            runCatching { json.decodeFromString(ApiError.serializer(), it) }
                .getOrElse { error ->
                    if (error !is SerializationException) throw error
                    null
                }
        }
        parsed?.let { "${it.message} (${it.code})" } ?: "Request failed with HTTP ${code()}"
    }
    else -> message?.takeIf(String::isNotBlank) ?: "The request could not be completed"
}
