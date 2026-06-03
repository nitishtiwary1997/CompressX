package com.hire.smartcompress.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    cornerRadius: Dp = 12.dp
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing)),
        label = "shimmer_x"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateX, 0f),
        end = Offset(translateX + 500f, 0f)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun SkeletonStatCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        ShimmerBox(height = 42.dp, cornerRadius = 21.dp, modifier = Modifier.width(42.dp))
        Spacer(Modifier.height(12.dp))
        ShimmerBox(height = 28.dp, modifier = Modifier.fillMaxWidth(0.6f))
        Spacer(Modifier.height(6.dp))
        ShimmerBox(height = 14.dp, modifier = Modifier.fillMaxWidth(0.9f))
    }
}

@Composable
fun SkeletonHistoryItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(height = 48.dp, cornerRadius = 8.dp, modifier = Modifier.size(48.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(height = 16.dp, modifier = Modifier.fillMaxWidth(0.7f))
            ShimmerBox(height = 14.dp, modifier = Modifier.fillMaxWidth(0.5f))
        }
        ShimmerBox(height = 24.dp, modifier = Modifier.width(60.dp))
    }
}
