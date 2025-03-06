package com.example.gyroscopeq4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Blue) {
                    GyroscopeBallScreen()
                }
            }
        }
    }
}

@Composable
fun GyroscopeBallScreen() {
    var ballPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    val ballRadius = 64f

    val obstacles = listOf(
        // Left top right bottom. Bottom and right have to be greater or app crashes!!
        // Probably can add more walls if I have the time
        Rect(100f, 250f, 500f, 270f),
        Rect(600f, 2280f, 900f, 2300f),
        Rect(800f, 100f, 820f, 2000f),
        Rect(200f, 400f, 220f, 1600f)
    )
    val screenWidth = 1080f
    val screenHeight = 1920f
    val sensorManager = LocalContext.current.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    var previousTimestamp by remember { mutableStateOf<Long>(0) }
    DisposableEffect(sensorManager) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val deltaTime = (event.timestamp - previousTimestamp) / 1_000_000_000f
                    previousTimestamp = event.timestamp
                    val deltaX = it.values[1] * 500f * deltaTime
                    val deltaY = -it.values[0] * 500f * deltaTime
                    val newX = (ballPosition.x + deltaX).coerceIn(ballRadius, screenWidth - ballRadius)
                    val newY = (ballPosition.y + deltaY).coerceIn(ballRadius, screenHeight - ballRadius)
                    // Detect collisions
                    if (!obstacles.any { rect -> isCircleCollidingWithRect(newX, newY, ballRadius, rect) }) {
                        ballPosition = Offset(newX, newY)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        obstacles.forEach { rect ->
            drawRect(color = Color.DarkGray, topLeft = Offset(rect.left, rect.top), size = androidx.compose.ui.geometry.Size(rect.width, rect.height))
        }
        drawCircle(color = Color.Blue, radius = ballRadius, center = ballPosition)
    }
}

fun isCircleCollidingWithRect(circleX: Float, circleY: Float, radius: Float, rect: Rect): Boolean {
    // This function was suggested by chatgpt, but can also be found at https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.ranges/coerce-in.html
    // Ensures that the value is within a range, calculates closest points for both ball and wall
    val closestX = circleX.coerceIn(rect.left, rect.right)
    val closestY = circleY.coerceIn(rect.top, rect.bottom)
    val distanceSquared = (circleX - closestX) * (circleX - closestX) + (circleY - closestY) * (circleY - closestY)
    return distanceSquared < radius * radius
}
