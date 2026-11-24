package com.example.englishapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.ui.theme.FluentTheme

/** Obsidian-Aurora studio background with radial nebula lighting and subtle flowing aurora ribbon. */
@Composable
fun AuroraScreenBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDark = FluentTheme.colors.isDark
  val bg = MaterialTheme.colorScheme.background
  val violet = FluentTheme.colors.violetGlow
  val cyan = FluentTheme.colors.cyanGlow

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(bg),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      if (isDark) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(violet.copy(alpha = 0.24f), Color.Transparent),
            center = Offset(size.width * 0.22f, size.height * 0.14f),
            radius = size.width * 0.78f,
          ),
          radius = size.width * 0.78f,
          center = Offset(size.width * 0.22f, size.height * 0.14f),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(cyan.copy(alpha = 0.15f), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.24f),
            radius = size.width * 0.68f,
          ),
          radius = size.width * 0.68f,
          center = Offset(size.width * 0.88f, size.height * 0.24f),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Color(0xFF4C1D95).copy(alpha = 0.20f), Color.Transparent),
            center = Offset(size.width * 0.55f, size.height * 0.78f),
            radius = size.width * 0.85f,
          ),
          radius = size.width * 0.85f,
          center = Offset(size.width * 0.55f, size.height * 0.78f),
        )

        // Subtle curved top aurora wave
        val ribbon = Path().apply {
          moveTo(0f, size.height * 0.20f)
          cubicTo(
            size.width * 0.30f, size.height * 0.12f,
            size.width * 0.68f, size.height * 0.25f,
            size.width, size.height * 0.15f,
          )
        }
        drawPath(
          path = ribbon,
          brush = Brush.horizontalGradient(
            listOf(
              violet.copy(alpha = 0.18f),
              Color(0xFFA855F7).copy(alpha = 0.28f),
              cyan.copy(alpha = 0.22f),
            ),
          ),
          style = Stroke(width = 32f, cap = StrokeCap.Round),
        )
      }
    }
    content()
  }
}

/** Frosted obsidian-glass surface card with top-left subtle sheen and crisp border. */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: RoundedCornerShape = RoundedCornerShape(22.dp),
  highlighted: Boolean = false,
  glowColor: Color = MaterialTheme.colorScheme.primary,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDark = FluentTheme.colors.isDark
  val baseColor = if (isDark) FluentTheme.colors.glassSurface else MaterialTheme.colorScheme.surface
  val borderColor = if (highlighted) {
    glowColor.copy(alpha = 0.88f)
  } else {
    FluentTheme.colors.cardBorder.copy(alpha = if (isDark) 0.75f else 1f)
  }

  val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

  Surface(
    modifier = modifier
      .clip(shape)
      .then(clickModifier),
    shape = shape,
    color = baseColor,
    border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, borderColor),
  ) {
    Box {
      if (isDark) {
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Brush.linearGradient(
                colors = listOf(
                  glowColor.copy(alpha = if (highlighted) 0.14f else 0.06f),
                  Color.Transparent,
                  FluentTheme.colors.cyanGlow.copy(alpha = if (highlighted) 0.06f else 0.02f),
                ),
              ),
            ),
        )
      }
      content()
    }
  }
}

/** Glowing 5-bar FluentVoice spectrum emblem + two-tone title + circular header button. */
@Composable
fun FluentVoiceBrandHeader(
  onActionClick: () -> Unit,
  actionIcon: ImageVector,
  actionContentDescription: String,
  modifier: Modifier = Modifier,
  subtitle: String = "Aprende inglés con IA",
) {
  val cyan = FluentTheme.colors.cyanGlow
  val violet = FluentTheme.colors.violetGlow

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f),
    ) {
      Canvas(modifier = Modifier.size(34.dp)) {
        val heights = listOf(0.40f, 0.72f, 1.0f, 0.68f, 0.36f)
        val barW = size.width * 0.12f
        val gap = (size.width - heights.size * barW) / (heights.size - 1)
        val barColors = listOf(
          cyan,
          Color(0xFF38BDF8),
          Color(0xFF818CF8),
          violet,
          Color(0xFFC084FC),
        )
        heights.forEachIndexed { idx, frac ->
          val h = size.height * frac
          val top = (size.height - h) / 2f
          val x = idx * (barW + gap)
          drawRoundRect(
            color = barColors[idx],
            topLeft = Offset(x, top),
            size = Size(barW, h),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f),
          )
        }
      }

      Column {
        Text(
          text = buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
              append("Fluent")
            }
            withStyle(SpanStyle(color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)) {
              append("Voice")
            }
          },
          fontSize = 21.sp,
          letterSpacing = (-0.4).sp,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    GlowingCircleIconButton(
      icon = actionIcon,
      contentDescription = actionContentDescription,
      onClick = onActionClick,
      size = 42.dp,
    )
  }
}

/** Smooth cyan-to-violet gradient progress bar. */
@Composable
fun GradientProgressBar(
  progress: Float,
  modifier: Modifier = Modifier,
  height: Dp = 8.dp,
) {
  val clamped = progress.coerceIn(0f, 1f)
  val cyan = FluentTheme.colors.cyanGlow
  val violet = FluentTheme.colors.violetGlow
  val trackColor = if (FluentTheme.colors.isDark) Color(0xFF222748) else MaterialTheme.colorScheme.surfaceVariant

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(999.dp))
      .background(trackColor),
  ) {
    if (clamped > 0f) {
      Box(
        modifier = Modifier
          .fillMaxWidth(clamped)
          .fillMaxHeight()
          .clip(RoundedCornerShape(999.dp))
          .background(
            Brush.horizontalGradient(
              colors = listOf(cyan, Color(0xFF6366F1), violet),
            ),
          ),
      )
    }
  }
}

/** Electric violet-to-indigo pill CTA button with soft top rim highlight. */
@Composable
fun GradientPillButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  height: Dp = 52.dp,
  enabled: Boolean = true,
) {
  val brush = if (enabled) {
    Brush.horizontalGradient(
      colors = listOf(
        Color(0xFF6D28D9),
        Color(0xFF8B5CF6),
        Color(0xFF6366F1),
      ),
    )
  } else {
    Brush.horizontalGradient(
      colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant,
      ),
    )
  }
  val contentColor = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(999.dp))
      .clickable(enabled = enabled, onClick = onClick),
    shape = RoundedCornerShape(999.dp),
    color = Color.Transparent,
    border = BorderStroke(
      width = 1.dp,
      color = if (enabled) Color(0x66DDD6FE) else FluentTheme.colors.cardBorder,
    ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(brush)
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = contentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/** Circular frosted-glass icon button used for headers and studio call controls. */
@Composable
fun GlowingCircleIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 42.dp,
  containerColor: Color? = null,
  borderColor: Color? = null,
  iconTint: Color? = null,
) {
  val bg = containerColor ?: if (FluentTheme.colors.isDark) {
    Color(0xFF1A1F3D)
  } else {
    MaterialTheme.colorScheme.surface
  }
  val border = borderColor ?: FluentTheme.colors.cardBorder.copy(alpha = 0.85f)
  val tint = iconTint ?: MaterialTheme.colorScheme.onSurface

  Surface(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .clickable(onClick = onClick),
    shape = CircleShape,
    color = bg,
    border = BorderStroke(1.dp, border),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size * 0.48f),
      )
    }
  }
}

/** Sweep-gradient cyan-to-violet circular gauge ring for Pronunciation/Accuracy metrics. */
@Composable
fun GlowingCircularGauge(
  percentage: Int,
  modifier: Modifier = Modifier,
  size: Dp = 74.dp,
  strokeWidth: Dp = 7.dp,
  centerLabel: String? = null,
) {
  val pct = percentage.coerceIn(0, 100)
  val cyan = FluentTheme.colors.cyanGlow
  val violet = FluentTheme.colors.violetGlow
  val trackColor = if (FluentTheme.colors.isDark) Color(0xFF23284A) else MaterialTheme.colorScheme.surfaceVariant

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val pxStroke = strokeWidth.toPx()
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = pxStroke),
      )
      drawArc(
        brush = Brush.sweepGradient(
          colors = listOf(violet, cyan, Color(0xFF34D399), cyan, violet),
        ),
        startAngle = -90f,
        sweepAngle = (pct / 100f) * 360f,
        useCenter = false,
        style = Stroke(width = pxStroke, cap = StrokeCap.Round),
      )
    }
    Text(
      text = centerLabel ?: "$pct %",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

/** Vector glowing open-book graphic with sparkles matching the Lecciones hero banner. */
@Composable
fun LuminousOpenBookGraphic(modifier: Modifier = Modifier) {
  val cyan = FluentTheme.colors.cyanGlow
  val violet = FluentTheme.colors.violetGlow

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Outer ambient aura
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(violet.copy(alpha = 0.48f), cyan.copy(alpha = 0.16f), Color.Transparent),
        center = Offset(w * 0.52f, h * 0.56f),
        radius = w * 0.52f,
      ),
      radius = w * 0.52f,
      center = Offset(w * 0.52f, h * 0.56f),
    )

    // Back purple cover shadow
    val cover = Path().apply {
      moveTo(w * 0.10f, h * 0.56f)
      lineTo(w * 0.50f, h * 0.78f)
      lineTo(w * 0.92f, h * 0.50f)
      lineTo(w * 0.54f, h * 0.30f)
      close()
    }
    drawPath(
      path = cover,
      brush = Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF6D28D9))),
    )

    // Left illuminated page
    val leftPage = Path().apply {
      moveTo(w * 0.14f, h * 0.50f)
      quadraticTo(w * 0.34f, h * 0.36f, w * 0.51f, h * 0.54f)
      lineTo(w * 0.51f, h * 0.73f)
      quadraticTo(w * 0.32f, h * 0.58f, w * 0.14f, h * 0.58f)
      close()
    }
    drawPath(
      path = leftPage,
      brush = Brush.linearGradient(listOf(Color(0xFFF3E8FF), Color(0xFFC4B5FD))),
    )

    // Right illuminated page
    val rightPage = Path().apply {
      moveTo(w * 0.51f, h * 0.54f)
      quadraticTo(w * 0.68f, h * 0.30f, w * 0.88f, h * 0.43f)
      lineTo(w * 0.88f, h * 0.52f)
      quadraticTo(w * 0.68f, h * 0.44f, w * 0.51f, h * 0.73f)
      close()
    }
    drawPath(
      path = rightPage,
      brush = Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFFA5B4FC))),
    )

    // Center glowing spine line
    drawLine(
      color = cyan.copy(alpha = 0.85f),
      start = Offset(w * 0.51f, h * 0.54f),
      end = Offset(w * 0.51f, h * 0.73f),
      strokeWidth = 3f,
    )

    // Floating sparks
    drawCircle(color = cyan.copy(alpha = 0.85f), radius = 3.2f, center = Offset(w * 0.34f, h * 0.24f))
    drawCircle(color = Color(0xFFD8B4FE), radius = 2.6f, center = Offset(w * 0.68f, h * 0.20f))
    drawCircle(color = cyan.copy(alpha = 0.75f), radius = 2.2f, center = Offset(w * 0.78f, h * 0.29f))
  }
}

private data class NavTabSpec(
  val label: String,
  val icon: ImageVector,
)

private val bottomNavSpecs = listOf(
  NavTabSpec("Inicio", Icons.Rounded.Home),
  NavTabSpec("Lecciones", Icons.AutoMirrored.Rounded.MenuBook),
  NavTabSpec("Speaking", Icons.Rounded.Mic),
  NavTabSpec("Progreso", Icons.Rounded.BarChart),
  NavTabSpec("Perfil", Icons.Rounded.Person),
)

/** 5-tab Obsidian-Glass bottom navigation bar with glowing violet capsule indicator. */
@Composable
fun FluentGlassBottomBar(
  selectedIndex: Int,
  onSelectTab: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = FluentTheme.colors.isDark
  val barBg = if (isDark) Color(0xF00E1226) else MaterialTheme.colorScheme.surface
  val borderColor = FluentTheme.colors.cardBorder.copy(alpha = 0.8f)
  val violet = FluentTheme.colors.violetGlow

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = barBg,
    border = BorderStroke(0.75.dp, borderColor),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 8.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      bottomNavSpecs.forEachIndexed { index, spec ->
        val selected = index == selectedIndex
        val activeBrush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF7C3AED).copy(alpha = 0.90f),
            Color(0xFF4C1D95).copy(alpha = 0.95f),
          ),
        )
        val itemShape = RoundedCornerShape(16.dp)
        val tint = if (selected) {
          if (isDark) Color.White else MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .clip(itemShape)
            .then(
              if (selected) {
                Modifier.background(
                  if (isDark) activeBrush else Brush.verticalGradient(
                    listOf(
                      MaterialTheme.colorScheme.primaryContainer,
                      MaterialTheme.colorScheme.primaryContainer,
                    ),
                  ),
                )
              } else {
                Modifier
              },
            )
            .clickable { onSelectTab(index) }
            .padding(vertical = 7.dp, horizontal = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector = spec.icon,
              contentDescription = spec.label,
              tint = if (selected && isDark) Color.White else if (selected) violet else tint,
              modifier = Modifier.size(22.dp),
            )
            Text(
              text = spec.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected && isDark) Color.White else if (selected) violet else tint,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
