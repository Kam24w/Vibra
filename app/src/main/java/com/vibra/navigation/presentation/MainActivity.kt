/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.vibra.navigation.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import android.os.VibrationEffect
import com.vibra.navigation.Direction
import com.vibra.navigation.presentation.theme.VibraTheme
import com.vibra.navigation.NavigationEngine
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import android.location.Location
import android.location.LocationListener
fun vibrateForDirection(
    vibrator: android.os.Vibrator,
    direction: Direction
) {
    when (direction) {
        Direction.LEFT -> {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    700,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }

        Direction.RIGHT -> {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    250,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }

        Direction.STRAIGHT -> {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 250, 150, 250),
                    -1
                )
            )
        }
    }
}
class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            println(
                "VIBRA GPS: ${location.latitude}, ${location.longitude}"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                5f,
                locationListener
            )
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }

        val vibrator = getSystemService(
            android.os.Vibrator::class.java
        )

        setContent {
            WearApp("Vibra", vibrator)
        }
    }

    private fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return locationManager.getLastKnownLocation(
            LocationManager.GPS_PROVIDER
        )
    }
}
@Composable
fun WearApp(greetingName: String, vibrator: android.os.Vibrator?) {
    val navigationEngine = remember { NavigationEngine() }

    var direction by remember {
        mutableStateOf(
            navigationEngine.calculateDirection(
                currentBearing = 0.0,
                targetBearing = 90.0
            )
        )
    }
    LaunchedEffect(direction) {
        vibrator?.let {
            vibrateForDirection(it, direction)
        }
    }
    VibraTheme {
        AppScaffold {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (direction) {
                        Direction.LEFT -> "←"
                        Direction.RIGHT -> "→"
                        Direction.STRAIGHT -> "↑"
                    },
                    fontSize = 140.sp
                )
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp("Preview Android", null)
}