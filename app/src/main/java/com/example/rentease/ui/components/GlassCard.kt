package com.example.rentease.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rentease.ui.theme.White

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun GlassCardSurface(
    modifier: Modifier = Modifier,
    radius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = modifier, radius = radius) {
        content()
    }
}
