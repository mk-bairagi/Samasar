package com.samasar.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.samasar.app.data.model.FeedFilter
import com.samasar.app.ui.components.NewsIcons
import com.samasar.app.ui.components.SectionHeader
import com.samasar.app.ui.components.contentSurface
import com.samasar.app.ui.glass.ChromeState
import com.samasar.app.ui.glass.GlassIconButton
import com.samasar.app.ui.glass.GlassSearchField
import com.samasar.app.ui.glass.GlassToggle
import com.samasar.app.ui.glass.pressBounce
import com.samasar.app.ui.glass.PillCorner
import com.samasar.app.ui.glass.backdrop
import com.samasar.app.ui.glass.rememberBackdrop
import androidx.compose.ui.text.style.TextOverflow
import com.samasar.app.ui.theme.NewsTheme
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
    savedCount: Int,
    placeName: String,
    stateName: String?,
    sourceCount: Int,
    preferences: List<Preference>,
    filter: FeedFilter,
    knownSources: List<String>,
    onMuteSource: (String) -> Unit,
    onUnmuteSource: (String) -> Unit,
    onMuteKeyword: (String) -> Unit,
    onUnmuteKeyword: (String) -> Unit,
    onClearHidden: () -> Unit,
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
                        // Devanagari matras hang off the preceding consonant, so a
                        // single codepoint can slice a letter in half. Take the
                        // first grapheme cluster instead.
                        text = placeName.firstGrapheme(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = placeName,
                        // titleLarge, not headlineMedium: Devanagari district names
                        // carry matras above and below the line, so they need more
                        // vertical and horizontal room per letter than the Latin
                        // placeholder this card was designed around. At the larger
                        // size "इंदौर" broke across two lines inside its own card.
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = stateName ?: "Your district",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        item(key = "stats") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile("Saved", savedCount.toString(), Modifier.weight(1f))
                StatTile("Sources", sourceCount.toString(), Modifier.weight(1f))
                StatTile("Filtered", filter.totalFiltered.toString(), Modifier.weight(1f))
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

        item(key = "muted-header") {
            SectionHeader(
                title = "Muted",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        item(key = "muted") {
            MutedCard(
                filter = filter,
                knownSources = knownSources,
                onMuteSource = onMuteSource,
                onUnmuteSource = onUnmuteSource,
                onMuteKeyword = onMuteKeyword,
                onUnmuteKeyword = onUnmuteKeyword,
                onClearHidden = onClearHidden,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item(key = "tail") { Box(Modifier.height(8.dp)) }
    }
}

/**
 * Everything the reader has muted, and a way back out of it.
 *
 * Muting is only safe to offer if it is visibly reversible — otherwise a reader
 * who mutes a publisher by accident quietly loses a chunk of their news and has
 * no idea why.
 */
@Composable
private fun MutedCard(
    filter: FeedFilter,
    knownSources: List<String>,
    onMuteSource: (String) -> Unit,
    onUnmuteSource: (String) -> Unit,
    onMuteKeyword: (String) -> Unit,
    onUnmuteKeyword: (String) -> Unit,
    onClearHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    var draft by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .contentSurface(26.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "KEYWORDS",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
            Text(
                text = "Stories containing these words are hidden. More precise than any " +
                    "topic filter — mute a name, a tournament, a scandal.",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassSearchField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = "Add a word to mute",
                    modifier = Modifier.weight(1f),
                )
                GlassIconButton(
                    icon = NewsIcons.Check,
                    contentDescription = "Mute keyword",
                    onClick = {
                        onMuteKeyword(draft)
                        draft = ""
                    },
                    diameter = 44.dp,
                )
            }
            if (filter.mutedKeywords.isNotEmpty()) {
                ChipFlow(filter.mutedKeywords.toList()) { onUnmuteKeyword(it) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SOURCES",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
            if (filter.mutedSources.isEmpty()) {
                Text(
                    text = "No muted sources. Use ⋯ on any story to mute its publisher.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary,
                )
            } else {
                ChipFlow(filter.mutedSources.toList()) { onUnmuteSource(it) }
            }
        }

        val hiddenCount = filter.hiddenUrls.size
        if (hiddenCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressBounce(pressedScale = 0.98f, onClick = onClearHidden),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (hiddenCount == 1) "1 hidden story" else "$hiddenCount hidden stories",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Restore all",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent,
                )
            }
        }
    }
}

/** Removable chips — tapping one un-mutes it. */
@Composable
private fun ChipFlow(items: List<String>, onRemove: (String) -> Unit) {
    val colors = NewsTheme.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { value ->
            Row(
                modifier = Modifier
                    .pressBounce(pressedScale = 0.92f) { onRemove(value) }
                    .clip(RoundedCornerShape(PillCorner))
                    .background(colors.accent.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
                M3Icon(
                    imageVector = NewsIcons.Close,
                    contentDescription = "Remove",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

/**
 * The first *visible* character of a string.
 *
 * "इंदौर" begins with इ followed by a combining sign; taking one char would render
 * a bare consonant or an orphaned matra. BreakIterator walks grapheme clusters,
 * which is what a reader sees as one letter.
 */
private fun String.firstGrapheme(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return "?"
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(trimmed)
    val end = it.next()
    return if (end == java.text.BreakIterator.DONE) trimmed else trimmed.substring(0, end)
}
