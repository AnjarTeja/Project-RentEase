package com.example.rentease.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Star(
    var x: Float,
    var y: Float,
    val alpha: Float,
    val radius: Float,
    val speed: Float
)

@Composable
fun StarFieldOverlay(
    modifier: Modifier = Modifier,
    starCount: Int = 50,
    alpha: Float = 0.3f,
    baseColor: Color = Color.White
) {
    val stars = remember {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                alpha = Random.nextFloat() * 0.8f + 0.2f,
                radius = Random.nextFloat() * 2f + 0.5f,
                speed = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(50)
            time += 0.016f
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (star in stars) {
            val twinkle = ((kotlin.math.sin(time * 2 + star.x * 100 + star.y * 100) + 1) / 2)
            val starAlpha = star.alpha * twinkle
            drawCircle(
                color = baseColor.copy(alpha = starAlpha * alpha),
                radius = star.radius,
                center = Offset(star.x * w, (star.y * h + time * star.speed * 20) % h)
            )
        }
    }
}
