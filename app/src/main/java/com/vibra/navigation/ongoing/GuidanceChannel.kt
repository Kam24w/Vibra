package com.vibra.navigation.ongoing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.vibra.navigation.R

/** Canal silencioso de la ruta activa: la vibración ya la pone cada indicación. */
object GuidanceChannel {
    const val ID = "vibra_active_route"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(ID, context.getString(R.string.ongoing_channel), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.ongoing_channel_description)
                enableVibration(false)
                setSound(null, null)
            },
        )
    }
}
