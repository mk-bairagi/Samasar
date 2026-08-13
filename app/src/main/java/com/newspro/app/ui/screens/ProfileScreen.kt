package com.newspro.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.SectionHeader
import com.newspro.app.ui.components.contentSurface
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.GlassToggle
import com.newspro.app.ui.glass.PillCorner
import com.newspro.app.ui.glass.backdrop
import com.newspro.app.ui.glass.rememberBackdrop
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

@Immutable
data class Preference(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onChange: (Boolean) -> Unit,
)

@Composable
fun ProfileScreen(
    readCount: Int,
    savedCount: Int,
    streakDays: Int,
    preferences: List<Preference>,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
        }.collect { chrome.report(it) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "identity") {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .contentSurface(26.dp)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(PillCorner))
                        .background(Brush.linearGradient(colors.accentGradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "MB",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Mayank",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Reading since March 2026",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary,
                    )
                }
            }
        }

        item(key = "stats") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile("Read", readCount.toString(), Modifier.weight(1f))
                StatTile("Saved", savedCount.toString(), Modifier.weight(1f))
                StatTile("Streak", "${streakDays}d", Modifier.weight(1f))
            }
        }

        item(key = "prefs-header") {
            SectionHeader(
                title = "Preferences",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        item(key = "prefs") {
            PreferenceCard(
                preferences = preferences,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item(key = "tail") { Box(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    Column(
        modifier = modifier
            .contentSurface(20.dp)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary,
        )
    }
}

/**
 * Settings rows whose switches are real glass.
 *
 * The card paints its own wash and row dividers into a *local* backdrop, and each [GlassToggle]
 * refracts that layer. So the divider lines visibly bend as they pass under a thumb — the same
 * optics as the chrome, at control scale, without the toggles needing to reach the screen backdrop
 * they are nested inside.
 */
@Composable
private fun PreferenceCard(
    preferences: List<Preference>,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    val local = rememberBackdrop()
    val rowHeight = 72.dp

    Box(modifier.clip(RoundedCornerShape(26.dp))) {
        Box(
            Modifier
                .matchParentSize()
                .backdrop(local),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val top: Color
                val bottom: Color
                if (colors.isDark) {
                    top = colors.textPrimary.copy(alpha = 0.085f)
                    bottom = colors.textPrimary.copy(alpha = 0.040f)
                } else {
                    top = Color.White.copy(alpha = 0.68f)
                    bottom = Color.White.copy(alpha = 0.48f)
                }
                drawRect(Brush.verticalGradient(listOf(top, bottom)))
                // A pass of accent light, so the toggles have colour to bend as well as lines.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.accent.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.16f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.82f, size.height * 0.16f),
                )

                val step = rowHeight.toPx()
                var y = step
                while (y < size.height - 1f) {
                    drawLine(
                        color = colors.textPrimary.copy(alpha = if (colors.isDark) 0.16f else 0.20f),
                        start = Offset(18.dp.toPx(), y),
                        end = Offset(size.width - 18.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                    )
                    y += step
                }
            }
        }

        Column {
            preferences.forEach { pref ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    M3Icon(
                        imageVector = pref.icon,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = pref.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = pref.subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textTertiary,
                        )
                    }
                    GlassToggle(
                        backdrop = local,
                        checked = pref.checked,
                        onCheckedChange = pref.onChange,
                    )
                }
            }
        }
    }
}

