package com.vlcsync.remote.data

enum class ConnectionState { DISCONNECTED, AUTH_NEEDED, CONNECTED, ERROR }
enum class PlaybackState { PLAYING, PAUSED, STOPPED, UNKNOWN }

data class PlaybackStatus(
    val state: PlaybackState,
    val positionMs: Long,
    val durationMs: Long,
    val mediaTitle: String?
)

data class VlcDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val connectionState: ConnectionState,
    val status: PlaybackStatus? = null,
    val lastRoundTripMs: Long? = null
) {
    val baseUrl: String get() = "https://$host:$port"
}

fun normalizeAddress(input: String): String? {
    val cleanInput = input.trim().removePrefix("http://").removePrefix("https://")
    val regex = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:[0-9]{1,5}$")
    return if (regex.matches(cleanInput)) "https://$cleanInput" else null
}