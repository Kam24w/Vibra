package com.vibra.navigation

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class NavigationEngine {

    fun calculateBearing(
        currentLatitude: Double,
        currentLongitude: Double,
        targetLatitude: Double,
        targetLongitude: Double
    ): Double {

        val currentLat = Math.toRadians(currentLatitude)
        val targetLat = Math.toRadians(targetLatitude)

        val deltaLongitude = Math.toRadians(
            targetLongitude - currentLongitude
        )

        val y = sin(deltaLongitude) * cos(targetLat)

        val x =
            cos(currentLat) * sin(targetLat) -
                    sin(currentLat) * cos(targetLat) * cos(deltaLongitude)

        val bearing = Math.toDegrees(
            atan2(y, x)
        )

        return (bearing + 360) % 360
    }

    fun calculateDirection(
        currentBearing: Double,
        targetBearing: Double
    ): Direction {

        var difference = targetBearing - currentBearing

        if (difference > 180) {
            difference -= 360
        }

        if (difference < -180) {
            difference += 360
        }

        return when {
            difference > 30 -> Direction.RIGHT
            difference < -30 -> Direction.LEFT
            else -> Direction.STRAIGHT
        }
    }
}