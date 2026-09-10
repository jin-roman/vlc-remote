package com.vlcsync.remote.sync

import com.vlcsync.remote.data.PlaybackStatus
import com.vlcsync.remote.data.VlcRemoteClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SyncCoordinator(
    private val clientA: VlcRemoteClient,
    private val clientB: VlcRemoteClient
) {
    suspend fun alignToMinimum(): AlignmentResult {
        val statusA = clientA.getStatus()
        val statusB = clientB.getStatus()

        require(statusA.mediaTitle == statusB.mediaTitle) { "Файлы отличаются!" }

        val target = minOf(statusA.positionMs, statusB.positionMs)

        coroutineScope {
            launch { clientA.seek(target) }
            launch { clientB.seek(target) }
        }

        val afterA = clientA.getStatus()
        val afterB = clientB.getStatus()

        return AlignmentResult(target, afterA, afterB)
    }

    suspend fun syncPlay(rttA: Long, rttB: Long) {
        val maxRtt = maxOf(rttA, rttB)
        val delayA = maxRtt - rttA
        val delayB = maxRtt - rttB

        coroutineScope {
            launch {
                delay(delayA)
                clientA.play()
            }
            launch {
                delay(delayB)
                clientB.play()
            }
        }
    }
}

data class AlignmentResult(
    val targetMs: Long,
    val statusA: PlaybackStatus,
    val statusB: PlaybackStatus
)