package com.ai.cameracalibration.network

import android.util.Log
import com.ai.cameracalibration.model.WebSocketMessage
import com.ai.cameracalibration.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebSocketManager private constructor() {
    private var webSocket: WebSocketClient? = null
    private val _messageFlow = MutableStateFlow<String?>(null)  // String으로 변경
    val messageFlow: StateFlow<String?> = _messageFlow

    companion object {
        private const val TAG = "WebSocketManager"
        private const val WS_URL = Constants.WS_URL  // 실제 서버 IP로 변경

        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }

    fun connect(deviceId: String, deviceType: String) {
        try {
            val uri = URI(WS_URL)

            val webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    Log.d(TAG, "WebSocket Connection opened")
                    // 연결 후 장치 타입 전송
                    sendMessage("""{"deviceType":"$deviceType"}""")
                }

                override fun onMessage(message: String?) {
                    Log.d(TAG, "Received message: $message")
                    message?.let {
                        _messageFlow.value = it
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Log.d(TAG, "WebSocket Connection closed: $reason")
                }

                override fun onError(ex: Exception?) {
                    Log.e(TAG, "WebSocket Error: ${ex?.message}")
                    ex?.printStackTrace()
                }
            }

            webSocketClient.connect()
            webSocket = webSocketClient

        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            e.printStackTrace()
        }
    }

    // sendMessage 함수 추가
    fun sendMessage(message: String) {
        try {
            webSocket?.send(message)
            Log.d(TAG, "Sent message: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            webSocket?.close()
            webSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }

}