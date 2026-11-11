package com.vibra.navigation.presentation.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import com.vibra.navigation.R
import kotlinx.coroutines.delay

@Composable
fun formatDistance(meters: Double): String =
    if (meters < 1_000) {
        stringResource(R.string.distance_meters, meters.toInt())
    } else {
        stringResource(R.string.distance_km, meters / 1_000)
    }

/** Versión sin Compose de [formatDistance], para notificaciones y servicios. */
fun formatDistanceText(context: Context, meters: Double): String =
    if (meters < 1_000) {
        context.getString(R.string.distance_meters, meters.toInt())
    } else {
        context.getString(R.string.distance_km, meters / 1_000)
    }

/** Reloj que se actualiza cada segundo mientras el composable está visible. */
@Composable
fun rememberNow(intervalMs: Long = 1_000L): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(intervalMs)
            value = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
fun formatElapsed(sinceMs: Long?, now: Long): String {
    if (sinceMs == null) return stringResource(R.string.sync_never)
    val seconds = ((now - sinceMs) / 1_000).coerceAtLeast(0)
    return when {
        seconds < 5 -> stringResource(R.string.sync_now)
        seconds < 60 -> stringResource(R.string.sync_seconds, seconds.toInt())
        else -> stringResource(R.string.sync_minutes, (seconds / 60).toInt())
    }
}
