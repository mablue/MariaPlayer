package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class EmojiParticle(
    val emoji: String,
    val startX: Float,
    val startY: Float,
    val angleRad: Double,
    val speed: Float,
    val size: Float,
    val rotationSpeed: Float
)

@Composable
fun EmojiExplosion(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    if (!isActive) return

    val particles = remember {
        val emojis = listOf("🌸", "🎂", "🎉", "💖", "🎵", "🎈", "✨", "💐", "🎶", "👑")
        List(45) {
            val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
            val speed = Random.nextFloat() * 450f + 250f
            EmojiParticle(
                emoji = emojis.random(),
                startX = 0.5f,
                startY = 0.45f,
                angleRad = angle,
                speed = speed,
                size = Random.nextFloat() * 24f + 26f,
                rotationSpeed = Random.nextFloat() * 360f - 180f
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3200, easing = LinearOutSlowInEasing)
        )
        onFinished()
    }

    val paint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = progress.value

        particles.forEach { p ->
            val originX = p.startX * w
            val originY = p.startY * h
            val distance = p.speed * t
            val gravity = 350f * t * t

            val currentX = originX + (distance * cos(p.angleRad)).toFloat()
            val currentY = originY + (distance * sin(p.angleRad)).toFloat() + gravity
            val alpha = (1f - (t * 1.15f)).coerceIn(0f, 1f)

            paint.textSize = p.size * (1f + 0.3f * (1f - t))
            paint.alpha = (alpha * 255).toInt()

            drawContext.canvas.nativeCanvas.drawText(
                p.emoji,
                currentX,
                currentY,
                paint
            )
        }
    }
}
