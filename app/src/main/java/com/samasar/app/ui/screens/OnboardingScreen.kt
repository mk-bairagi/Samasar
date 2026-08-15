package com.samasar.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samasar.app.data.model.Place
import com.samasar.app.data.model.PlaceIndex
import com.samasar.app.ui.components.NewsIcons
import com.samasar.app.ui.components.contentSurface
import com.samasar.app.ui.glass.GlassButton
import com.samasar.app.ui.glass.GlassSearchField
import com.samasar.app.ui.glass.pressBounce
import com.samasar.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

/**
 * First launch: ask where the reader is.
 *
 * This is the only question the app ever asks, and it is worth asking. Defaulting
 * silently to whichever district sorts first alphabetically means a reader in
 * Rewa opens the app to news from Agar Malwa and concludes it is broken.
 *
 * No location permission — a picker is more accurate anyway. People want news from
 * home, not from wherever they happen to be standing.
 */
@Composable
fun OnboardingScreen(
    index: PlaceIndex,
    lang: String,
    loading: Boolean,
    error: String?,
    onSelect: (Place) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewsTheme.colors
    var query by rememberSaveable { mutableStateOf("") }

    val states = index.activeStates.ifEmpty { index.states.mapNotNull { it.state }.distinct() }
    val districts = states
        .flatMap { index.districtsIn(it, lang) }
        .filter { it.matches(query) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Samasar",
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "आपका जिला चुनें",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "Choose your district. Local news comes first, then your state, " +
                    "then India. You can change this any time.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }

        GlassSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search district or city",
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (loading && districts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Loading districts…",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                )
            }
            return@Column
        }

        // An empty list means two very different things, and conflating them told
        // the reader their search failed when actually the app could not reach the
        // feed at all.
        if (districts.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (query.isBlank()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Could not load districts",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = error ?: "Check your internet connection and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        GlassButton(
                            text = "Try again",
                            onClick = onRetry,
                            icon = NewsIcons.Refresh,
                            prominent = true,
                        )
                    }
                } else {
                    Text(
                        text = "No district matches “$query”",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(districts, key = { it.key }) { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressBounce(pressedScale = 0.98f) { onSelect(place) }
                        .contentSurface(18.dp)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = place.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        if (place.aliases.isNotEmpty()) {
                            Text(
                                text = place.aliases.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textTertiary,
                            )
                        }
                    }
                    M3Icon(
                        imageVector = NewsIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}
