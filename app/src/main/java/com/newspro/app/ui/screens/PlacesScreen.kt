package com.newspro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newspro.app.data.model.Place
import com.newspro.app.data.model.PlaceIndex
import com.newspro.app.ui.components.NewsIcons
import com.newspro.app.ui.components.SectionHeader
import com.newspro.app.ui.components.contentSurface
import com.newspro.app.ui.glass.ChromeState
import com.newspro.app.ui.glass.pressBounce
import com.newspro.app.ui.theme.NewsTheme
import androidx.compose.material3.Icon as M3Icon

/**
 * Choose the place you want news from.
 *
 * This is the app's most important setting, so it gets a tab rather than being
 * buried in preferences. Search matches aliases too — someone typing "Prayagraj"
 * finds the district the publisher still files under "allahabad".
 */
@Composable
fun PlacesScreen(
    index: PlaceIndex,
    query: String,
    lang: String,
    selectedState: String?,
    selectedDistrict: String?,
    chrome: ChromeState,
    contentPadding: PaddingValues,
    onSelect: (Place) -> Unit,
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

    val states = index.activeStates.ifEmpty { index.states.mapNotNull { it.state }.distinct() }

    val grouped = states.associateWith { code ->
        index.districtsIn(code, lang).filter { it.matches(query) }
    }.filterValues { it.isNotEmpty() }

    if (grouped.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                M3Icon(
                    imageVector = NewsIcons.Search,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    text = if (query.isBlank()) "No places available yet" else "No place matches “$query”",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        grouped.forEach { (stateCode, districts) ->
            val stateTitle = index.stateFeed(stateCode, lang)?.title ?: stateCode.uppercase()

            item(key = "hdr-$stateCode") {
                SectionHeader(
                    title = stateTitle,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            items(districts, key = { it.key }) { place ->
                val selected = place.state == selectedState && place.district == selectedDistrict
                PlaceRow(place = place, selected = selected, onClick = { onSelect(place) })
            }
        }
    }
}

@Composable
private fun PlaceRow(
    place: Place,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = NewsTheme.colors
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .pressBounce(pressedScale = 0.98f, onClick = onClick)
            .contentSurface(18.dp)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = place.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) colors.accent else colors.textPrimary,
            )
            if (place.aliases.isNotEmpty()) {
                Text(
                    text = place.aliases.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary,
                )
            }
        }
        if (selected) {
            M3Icon(
                imageVector = NewsIcons.Check,
                contentDescription = "Selected",
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
