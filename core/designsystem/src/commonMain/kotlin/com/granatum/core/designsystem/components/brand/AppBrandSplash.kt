package com.granatum.core.designsystem.components.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import granatumsuite.core.designsystem.generated.resources.Res
import granatumsuite.core.designsystem.generated.resources.brand_wordmark
import granatumsuite.core.designsystem.generated.resources.granatum_symbol_circle
import granatumsuite.core.designsystem.generated.resources.granatum_symbol_star
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * The animated splash that follows the system one (brand guide, 1.2 s): the coral circle grows
 * from 80 % with a soft bounce, the star opens, the circle pulses once and the wordmark appears.
 * It continues the system splash on the same ground and with the symbol at the same size, and
 * never holds anyone back: whoever shows it removes it as soon as the app is ready.
 */
@Composable
fun AppBrandSplash(contentDescription: String, modifier: Modifier = Modifier) {
    val circleScale = remember { Animatable(0.8f) }
    val starScale = remember { Animatable(0.4f) }
    val starAlpha = remember { Animatable(0f) }
    val starRotation = remember { Animatable(-45f) }
    val wordmarkAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { circleScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
            delay(300)
            launch { starAlpha.animateTo(1f, tween(300)) }
            launch { starScale.animateTo(1f, tween(600)) }
            launch { starRotation.animateTo(0f, tween(600)) }
            delay(300)
            circleScale.animateTo(1.06f, tween(150))
            circleScale.animateTo(1f, tween(150))
            wordmarkAlpha.animateTo(1f, tween(300))
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Image(
                    imageVector = vectorResource(Res.drawable.granatum_symbol_circle),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp).graphicsLayer {
                        scaleX = circleScale.value
                        scaleY = circleScale.value
                    }
                )
                Image(
                    imageVector = vectorResource(Res.drawable.granatum_symbol_star),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp).graphicsLayer {
                        alpha = starAlpha.value
                        scaleX = starScale.value * circleScale.value
                        scaleY = starScale.value * circleScale.value
                        rotationZ = starRotation.value
                    }
                )
            }
            Text(
                text = stringResource(Res.string.brand_wordmark),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer { alpha = wordmarkAlpha.value }
            )
        }
    }
}
