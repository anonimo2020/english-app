package com.example.englishapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.englishapp.ui.theme.FluentTheme

@Composable
fun DailyGoalRing(
  todayXp: Int,
  goalXp: Int,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  ringColor: Color = MaterialTheme.colorScheme.primary,
) {
  val safeGoal = goalXp.coerceAtLeast(1)
  val targetFraction = (todayXp.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
  val animatedFraction by animateFloatAsState(
    targetValue = targetFraction,
    animationSpec = tween(durationMillis = 600),
    label = "goalRing",
  )
  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val percent = (targetFraction * 100).toInt()

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val stroke = size.toPx() * 0.12f
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
      drawArc(
        color = ringColor,
        startAngle = -90f,
        sweepAngle = 360f * animatedFraction,
        useCenter = false,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
    }
    Text(
      text = "$percent%",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
fun StreakPill(
  streakDays: Int,
  modifier: Modifier = Modifier,
) {
  val flameColor = FluentTheme.colors.manuAccent
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = flameColor.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, flameColor.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.LocalFireDepartment,
        contentDescription = "Racha",
        tint = flameColor,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "${streakDays}d",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun XpBadge(
  totalXp: Int,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = primary.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = "XP",
        tint = primary,
        modifier = Modifier.size(15.dp),
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "$totalXp XP",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun StatusModeChip(
  offlineDemoMode: Boolean,
  hasBackendUrl: Boolean,
  modifier: Modifier = Modifier,
) {
  val (label, dotColor) = when {
    offlineDemoMode -> "Modo Demo" to FluentTheme.colors.correction
    hasBackendUrl -> "IA En Vivo" to FluentTheme.colors.success
    else -> "Sin URL" to FluentTheme.colors.recording
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(dotColor),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
