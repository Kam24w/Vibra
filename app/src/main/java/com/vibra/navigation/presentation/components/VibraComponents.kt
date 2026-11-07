package com.vibra.navigation.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.vibra.navigation.R
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraGradients

/** Fondo con degradado crema (sin negro plano) para todas las pantallas. */
@Composable
fun VibraBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibraGradients.Screen),
        content = content,
    )
}

/** Tarjeta marina redondeada con degradado. */
@Composable
fun NavyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(VibraGradients.NavyCard, RoundedCornerShape(cornerRadius)),
        content = content,
    )
}

/** Logo + palabra "Vibra", como en la cabecera del mockup. */
@Composable
fun VibraHeader(modifier: Modifier = Modifier, logoSize: Dp = 26.dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(R.drawable.vibra_logo),
            contentDescription = null,
            modifier = Modifier.size(logoSize),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.app_name),
            color = VibraColors.Navy,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Píldora marina con texto crema (p. ej. "Destino: Pasto"). */
@Composable
fun NavyPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = VibraColors.Cream,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .background(VibraColors.Navy, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** Punto de estado de color. */
@Composable
fun StatusDot(color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

object VibraButtons {
    /** Acción principal: naranja con texto marino. */
    @Composable
    fun primary(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = VibraColors.Orange,
        contentColor = VibraColors.Navy,
        disabledContainerColor = VibraColors.Beige,
        disabledContentColor = VibraColors.NavyMuted,
    )

    /** Acción secundaria: marino con texto crema. */
    @Composable
    fun navy(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = VibraColors.Navy,
        contentColor = VibraColors.Cream,
        disabledContainerColor = VibraColors.Beige,
        disabledContentColor = VibraColors.NavyMuted,
    )
}
