package com.samasar.app.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samasar.app.ui.components.NewsIcons
import com.samasar.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

/*
 * Small controls use [Modifier.frostedSurface] rather than a full refraction pass.
 *
 * They sit *on* the chrome, not floating independently over the feed, so a real backdrop sample
 * buys almost nothing visually while costing the backdrop an extra walk of its whole draw tree
 * each frame. They stay in the same material family — same tint, bevel and rim — for free.
 */

/** Primary action. [prominent] pours the accent gradient into the pane. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    prominent: Boolean = false,
) {
    val colors = NewsTheme.colors
    val contentColor = if (prominent) Color.White else colors.textPrimary

    Box(
        modifier = modifier
            .height(52.dp)
            .defaultMinSize(minWidth = 120.dp)
            .pressBounce(onClick = onClick)
            .frostedSurface(
                cornerRadius = PillCorner,
                tintAlpha = if (prominent) 0.06f else 0.16f,
                rimAlpha = 0.70f,
                fill = if (prominent) Brush.linearGradient(colors.accentGradient) else null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            if (icon != null) {
                M3Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

/** Circular control for chrome actions. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
    tint: Color? = null,
) {
    val colors = NewsTheme.colors
    Box(
        modifier = modifier
            .size(diameter)
            .pressBounce(pressedScale = 0.90f, onClick = onClick)
            .frostedSurface(cornerRadius = PillCorner, tintAlpha = 0.16f, rimAlpha = 0.70f),
        contentAlignment = Alignment.Center,
    ) {
        M3Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textPrimary,
            modifier = Modifier.size(diameter * 0.42f),
        )
    }
}

/** Filter chip. Selection floods the pane with accent light rather than swapping a background. */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
        label = "chipGlow",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.textPrimary else colors.textSecondary,
        label = "chipLabel",
    )
    val accent = colors.accent

    Box(
        modifier = modifier
            .height(38.dp)
            .pressBounce(pressedScale = 0.93f, onClick = onClick)
            .frostedSurface(
                cornerRadius = PillCorner,
                tintAlpha = 0.16f,
                rimAlpha = 0.70f,
                fill = if (glow <= 0.01f) {
                    null
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = 0.85f * glow),
                            accent.copy(alpha = 0.55f * glow),
                        ),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

/** Search input built into a capsule. */
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search stories, topics, sources",
) {
    val colors = NewsTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .frostedSurface(cornerRadius = PillCorner, tintAlpha = 0.16f, rimAlpha = 0.70f),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            M3Icon(
                imageVector = NewsIcons.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textTertiary,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                M3Icon(
                    imageVector = NewsIcons.Close,
                    contentDescription = "Clear search",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(17.dp)
                        .pressBounce(pressedScale = 0.85f) { onValueChange("") },
                )
            }
        }
    }
}
