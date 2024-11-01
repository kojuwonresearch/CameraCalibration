package com.ai.cameracalibration.model

import com.google.gson.Gson

data class WebSocketMessage(
    val type: String,
    val deviceId: String? = null,
    val deviceType: String? = null,
    val scheduledTime: Long? = null
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String): WebSocketMessage {
            return gson.fromJson(json, WebSocketMessage::class.java)
        }
    }

    fun toJson(): String {
        return gson.toJson(this)
    }
}