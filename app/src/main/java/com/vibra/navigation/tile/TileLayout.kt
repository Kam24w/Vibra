package com.vibra.navigation.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import com.vibra.navigation.R
import com.vibra.navigation.presentation.MainActivity
import com.vibra.navigation.presentation.components.formatDistanceText

/**
 * Diseño del Tile con los colores de marca (marfil, marino, naranja): icono, título,
 * una línea de estado y un botón que abre la guía o la app.
 */
object TileLayout {
    const val ICON_ID = "vibra_icon"

    private const val IVORY = 0xFFF4F1EA.toInt()
    private const val NAVY = 0xFF1B365D.toInt()
    private const val MUTED = 0xFF5B6B80.toInt()
    private const val ORANGE = 0xFFF7931E.toInt()

    fun build(context: Context, content: TileContent): LayoutElementBuilders.LayoutElement {
        val (title, subtitle) = texts(context, content)
        val buttonLabel = context.getString(
            if (content.action == TileAction.OPEN_GUIDANCE) R.string.tile_open_guidance else R.string.tile_open_app,
        )
        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(Modifiers.Builder().setBackground(Background.Builder().setColor(argb(IVORY)).build()).build())
            .addContent(
                Column.Builder()
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        Image.Builder()
                            .setResourceId(ICON_ID)
                            .setWidth(dp(26f))
                            .setHeight(dp(26f))
                            .setColorFilter(LayoutElementBuilders.ColorFilter.Builder().setTint(argb(NAVY)).build())
                            .build(),
                    )
                    .addContent(Spacer.Builder().setHeight(dp(4f)).build())
                    .addContent(text(title, 16f, NAVY, bold = true))
                    .addContent(text(subtitle, 12f, MUTED, bold = false))
                    .addContent(Spacer.Builder().setHeight(dp(10f)).build())
                    .addContent(button(context, buttonLabel, content.action))
                    .build(),
            )
            .build()
    }

    private fun texts(context: Context, content: TileContent): Pair<String, String> = when (content) {
        is TileContent.Route -> {
            val title = content.destination ?: context.getString(R.string.ongoing_title)
            val subtitle = when {
                content.paused -> context.getString(R.string.ongoing_paused)
                content.remainingMeters != null ->
                    context.getString(R.string.ongoing_remaining, formatDistanceText(context, content.remainingMeters))
                else -> context.getString(R.string.home_route_active)
            }
            title to subtitle
        }
        is TileContent.Idle -> context.getString(R.string.tile_no_route) to context.getString(
            if (content.phoneLinked) R.string.tile_phone_linked else R.string.tile_start_hint,
        )
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): Text =
        Text.Builder()
            .setText(value)
            .setMaxLines(1)
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(size))
                    .setColor(argb(color))
                    .setWeight(
                        if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL,
                    )
                    .build(),
            )
            .build()

    private fun button(context: Context, label: String, action: TileAction): Box =
        Box.Builder()
            .setWidth(wrap())
            .setHeight(dp(40f))
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(ORANGE))
                            .setCorner(Corner.Builder().setRadius(dp(20f)).build())
                            .build(),
                    )
                    .setPadding(Padding.Builder().setStart(dp(18f)).setEnd(dp(18f)).build())
                    .setClickable(launch(context, action))
                    .setSemantics(ModifiersBuilders.Semantics.Builder().setContentDescription(label).build())
                    .build(),
            )
            .addContent(text(label, 14f, NAVY, bold = true))
            .build()

    /** Abre MainActivity; con OPEN_GUIDANCE salta directamente a la guía. */
    private fun launch(context: Context, action: TileAction): Clickable {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(context.packageName)
            .setClassName(MainActivity::class.java.name)
        if (action == TileAction.OPEN_GUIDANCE) {
            activity.addKeyToExtraMapping(
                MainActivity.EXTRA_OPEN_GUIDANCE,
                ActionBuilders.AndroidBooleanExtra.Builder().setValue(true).build(),
            )
        }
        return Clickable.Builder()
            .setId(action.name)
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build())
            .build()
    }
}
