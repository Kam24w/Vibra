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
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt
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
        Direction.BACK -> {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 150, 500, 150, 500),
                    -1
                )
            )
        }
    }
}
class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val rotationListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                event.values
            )

            SensorManager.getOrientation(
                rotationMatrix,
                orientationAngles
            )

            val azimuth = (
                    Math.toDegrees(
                        orientationAngles[0].toDouble()
                    ) + 360
                    ) % 360

            currentAzimuth = azimuth.toFloat()

            println("VIBRA ORIENTACION: ${azimuth.toInt()}°")
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int
        ) {}
    }

    private var currentLocation by mutableStateOf<Location?>(null)
    private var currentAzimuth by mutableStateOf<Float?>(null)
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location

            println(
                "VIBRA GPS: ${location.latitude}, ${location.longitude}, bearing=${location.bearing}"
            )
        }
    }
    override fun onResume() {
        super.onResume()

        rotationSensor?.let { sensor ->
            sensorManager.registerListener(
                rotationListener,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(rotationListener)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        rotationSensor = sensorManager.getDefaultSensor(
            Sensor.TYPE_ROTATION_VECTOR
        )

        println(
            "VIBRA SENSOR: ${
                if (rotationSensor != null) "Disponible" else "No disponible"
            }"
        )

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
            WearApp(
                greetingName = "Vibra",
                vibrator = vibrator,
                currentLocation = currentLocation,
                currentAzimuth = currentAzimuth
            )
        }
    }
}
@Composable
fun WearApp( greetingName: String, vibrator: android.os.Vibrator?, currentLocation: Location?, currentAzimuth: Float?) {
    val navigationEngine = remember { NavigationEngine() }

    var direction by remember {
        mutableStateOf(Direction.STRAIGHT)
    }

    LaunchedEffect(currentLocation, currentAzimuth) {
        currentLocation?.let { location ->

            val targetLatitude = 37.4225
            val targetLongitude = -122.0835

            val targetBearing = navigationEngine.calculateBearing(
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                targetLatitude = targetLatitude,
                targetLongitude = targetLongitude
            )

            currentAzimuth?.let { azimuth ->
                direction = navigationEngine.calculateDirection(
                    currentBearing = azimuth.toDouble(),
                    targetBearing = targetBearing
                )
            }

            println(
                "VIBRA NAV: current=$currentAzimuth, target=$targetBearing, direction=$direction"
            )
        }
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
                        Direction.BACK -> "↓"
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
    WearApp(
        greetingName = "Preview Android",
        vibrator = null,
        currentLocation = null,
        currentAzimuth = null
    )
}