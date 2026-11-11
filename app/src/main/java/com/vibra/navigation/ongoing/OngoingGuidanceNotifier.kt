package com.vibra.navigation.ongoing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.vibra.navigation.R
import com.vibra.navigation.presentation.MainActivity
import com.vibra.navigation.presentation.components.formatDistanceText

/**
 * Publica la ruta activa como Ongoing Activity de Wear OS: un icono en la esfera y en
 * el lanzador que devuelve a la guía con un toque (como hace una app de mapas).
 */
class OngoingGuidanceNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)

    fun show(guidance: OngoingGuidance) {
        if (!canNotify()) return
        GuidanceChannel.ensure(appContext)
        val title = guidance.destination?.let { appContext.getString(R.string.ongoing_title_to, it) }
            ?: appContext.getString(R.string.ongoing_title)
        val text = statusText(guidance)
        val openGuidance = PendingIntent.getActivity(
            appContext,
            0,
            MainActivity.openGuidanceIntent(appContext),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(appContext, GuidanceChannel.ID)
            .setSmallIcon(R.drawable.ic_vibration)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openGuidance)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .addAction(R.drawable.ic_vibration, appContext.getString(R.string.ongoing_open), openGuidance)

        OngoingActivity.Builder(appContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_vibration)
            .setTouchIntent(openGuidance)
            .setStatus(Status.Builder().addTemplate(text).build())
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()
            .apply(appContext)

        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso de notificaciones", e)
        }
    }

    fun cancel() {
        manager.cancel(NOTIFICATION_ID)
    }

    private fun statusText(guidance: OngoingGuidance): String = when {
        guidance.paused -> appContext.getString(R.string.ongoing_paused)
        guidance.remainingMeters != null ->
            appContext.getString(R.string.ongoing_remaining, formatDistanceText(appContext, guidance.remainingMeters))
        else -> appContext.getString(R.string.home_route_active)
    }

    /** En Wear OS 4+ (API 33) publicar notificaciones exige POST_NOTIFICATIONS. */
    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "OngoingGuidance"
        const val NOTIFICATION_ID = 7310
    }
}
