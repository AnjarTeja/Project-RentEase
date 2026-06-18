package com.example.rentease.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.R
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: (String) -> Unit
) {
    val authManager = remember { FirebaseAuthManager() }
    var taglineAlpha by remember { mutableStateOf(0f) }
    var progressAlpha by remember { mutableStateOf(0f) }
    val taglineAlphaAnim by animateFloatAsState(
        targetValue = taglineAlpha,
        animationSpec = tween(800),
        label = "tagline"
    )
    val progressAlphaAnim by animateFloatAsState(
        targetValue = progressAlpha,
        animationSpec = tween(400),
        label = "progress"
    )

    GalaxyBackground(starAlpha = 0.3f) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_aplikasi),
                contentDescription = "Logo",
                modifier = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sewa Barang, Mudah & Cepat",
                color = TextLight,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(taglineAlphaAnim)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.alpha(progressAlphaAnim)) {
                LinearProgressIndicator(
                    modifier = Modifier.size(width = 120.dp, height = 3.dp),
                    color = Primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(500)
        taglineAlpha = 1f
        delay(500)
        progressAlpha = 1f
        delay(1500)

        if (authManager.isUserLoggedIn()) {
            authManager.getUserRole(
                onSuccess = { role -> onNavigateToDashboard(role) },
                onFailure = { onNavigateToDashboard("user") }
            )
        } else {
            suspendCancellableCoroutine { continuation ->
                authManager.seedPredefinedAccounts {
                    continuation.resume(Unit)
                }
            }
            onNavigateToLogin()
        }
    }
}
