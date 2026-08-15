package com.samasar.app.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samasar.app.data.NewsRepository
import com.samasar.app.data.RemoteNewsRepository
import com.samasar.app.data.model.Feed
import com.samasar.app.data.model.FeedFilter
import com.samasar.app.data.model.FeedScope
import com.samasar.app.data.model.FilterDto
import com.samasar.app.data.model.Place
import com.samasar.app.data.model.PlaceIndex
import com.samasar.app.data.model.Story
import com.samasar.app.data.remote.StoryDto
import com.samasar.app.data.remote.toDomain
import com.samasar.app.data.remote.toDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class FeedUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val index: PlaceIndex = PlaceIndex(),
    val scope: FeedScope = FeedScope.DISTRICT,
    val lang: String = "hi",
    val stateCode: String? = null,
    val districtSlug: String? = null,
    val feed: Feed? = null,
    /**
     * Saved stories are stored whole, not as ids. A saved story usually belongs to
     * a feed the reader is no longer looking at, so an id alone would have nothing
     * to resolve against — and the list has to work offline.
     */
    val saved: List<Story> = emptyList(),
    val filter: FeedFilter = FeedFilter(),
    /** Denser feed: smaller thumbnails, no carousel, more headlines per screen. */
    val compact: Boolean = false,
    /** True until the reader has chosen a district for the first time. */
    val needsOnboarding: Boolean = false,
    /** Most recently hidden story, offered back as an undo for a few seconds. */
    val undoHidden: Story? = null,
) {
    val savedIds: Set<String> get() = saved.mapTo(mutableSetOf()) { it.id }

    /** Everything the reader has not asked to hide. */
    val stories: List<Story> get() = feed?.stories.orEmpty().filter { filter.allows(it) }

    /** True when the feed is non-empty but the reader's own filters emptied it. */
    val emptiedByFilter: Boolean
        get() = stories.isEmpty() && feed?.stories?.isNotEmpty() == true

    /** The three scope tabs, in local → wide order. Missing feeds are dropped. */
    val tabs: List<Pair<FeedScope, String>>
        get() = buildList {
            districtPlace()?.let { add(FeedScope.DISTRICT to it.title) }
            statePlace()?.let { add(FeedScope.STATE to it.title) }
            nationalPlace()?.let { add(FeedScope.NATIONAL to it.title) }
        }

    fun districtPlace(): Place? = index.districts.firstOrNull {
        it.state == stateCode && it.district == districtSlug && it.lang == lang
    }

    fun statePlace(): Place? = stateCode?.let { index.stateFeed(it, lang) }

    fun nationalPlace(): Place? = index.nationalFeed(lang)

    fun placeFor(scope: FeedScope): Place? = when (scope) {
        FeedScope.DISTRICT -> districtPlace()
        FeedScope.STATE -> statePlace()
        FeedScope.NATIONAL -> nationalPlace()
    }

    val currentTitle: String
        get() = placeFor(scope)?.title ?: "Samasar"
}

/**
 * [JvmOverloads] is load-bearing: a Kotlin default parameter does not produce a
 * plain `(Application)` constructor, and that is the only signature the default
 * ViewModel factory knows how to call. Without it this crashes at first
 * composition. The repository stays injectable for tests.
 */
class FeedViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: NewsRepository = RemoteNewsRepository(app),
) : AndroidViewModel(app) {

    private var undoJob: Job? = null

    private val prefs = app.getSharedPreferences("samasar", android.content.Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        FeedUiState(
            lang = prefs.getString(KEY_LANG, "hi") ?: "hi",
            stateCode = prefs.getString(KEY_STATE, null),
            districtSlug = prefs.getString(KEY_DISTRICT, null),
            saved = readSaved(),
            filter = readFilter(),
            compact = prefs.getBoolean(KEY_COMPACT, false),
            // No saved district means this is a first launch. Ask, rather than
            // silently picking whichever district sorts first alphabetically.
            needsOnboarding = prefs.getString(KEY_DISTRICT, null) == null,
        )
    )
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.placeIndex()
                .onSuccess { index ->
                    val current = _state.value
                    // First launch: settle on a sensible default rather than an
                    // empty screen — the first live state, and its first district.
                    val stateCode = current.stateCode
                        ?: index.activeStates.firstOrNull()
                        ?: index.states.firstOrNull()?.state

                    _state.update { it.copy(index = index, stateCode = stateCode, loading = false) }

                    // On a first launch there is nothing to load until the reader
                    // has told us where they are.
                    if (!_state.value.needsOnboarding) loadFeed(_state.value.scope)
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(loading = false, error = err.message ?: "Could not reach the feed")
                    }
                }
        }
    }

    fun selectScope(scope: FeedScope) {
        if (_state.value.scope == scope) return
        _state.update { it.copy(scope = scope) }
        loadFeed(scope)
    }

    fun selectDistrict(place: Place) {
        prefs.edit()
            .putString(KEY_STATE, place.state)
            .putString(KEY_DISTRICT, place.district)
            .apply()
        _state.update {
            it.copy(
                stateCode = place.state,
                districtSlug = place.district,
                scope = FeedScope.DISTRICT,
                needsOnboarding = false,
            )
        }
        loadFeed(FeedScope.DISTRICT)
    }

    fun setCompact(value: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT, value).apply()
        _state.update { it.copy(compact = value) }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        loadFeed(_state.value.scope)
    }

    fun retry() = bootstrap()

    private fun loadFeed(scope: FeedScope) {
        val place = _state.value.placeFor(scope)
        if (place == null) {
            _state.update { it.copy(loading = false, refreshing = false, feed = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = it.feed == null, error = null) }
            repo.feed(place)
                .onSuccess { feed ->
                    _state.update { it.copy(feed = feed, loading = false, refreshing = false, error = null) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = err.message ?: "Could not load ${place.title}",
                        )
                    }
                }
        }
    }

    fun toggleSaved(id: String) {
        val story = storyById(id) ?: return
        _state.update { current ->
            val next = if (current.saved.any { it.id == id }) {
                current.saved.filterNot { it.id == id }
            } else {
                listOf(story) + current.saved
            }
            writeSaved(next)
            current.copy(saved = next)
        }
    }

    /** Looks in the live feed first, then in saved — a saved story outlives its feed. */
    fun storyById(id: String): Story? =
        _state.value.stories.firstOrNull { it.id == id }
            ?: _state.value.saved.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- filters
    fun hideStory(story: Story) {
        updateFilter { it.copy(hiddenIds = it.hiddenIds + story.id, hiddenUrls = it.hiddenUrls + story.url) }
        _state.update { it.copy(undoHidden = story) }
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(6_000)
            _state.update { it.copy(undoHidden = null) }
        }
    }

    fun undoHide() {
        val story = _state.value.undoHidden ?: return
        undoJob?.cancel()
        updateFilter { it.copy(hiddenIds = it.hiddenIds - story.id, hiddenUrls = it.hiddenUrls - story.url) }
        _state.update { it.copy(undoHidden = null) }
    }

    fun dismissUndo() {
        undoJob?.cancel()
        _state.update { it.copy(undoHidden = null) }
    }

    fun muteSource(name: String) {
        if (name.isBlank()) return
        updateFilter { it.copy(mutedSources = it.mutedSources + name) }
    }

    fun unmuteSource(name: String) = updateFilter { it.copy(mutedSources = it.mutedSources - name) }

    fun muteKeyword(word: String) {
        val clean = word.trim()
        if (clean.length < 2) return
        updateFilter { it.copy(mutedKeywords = it.mutedKeywords + clean) }
    }

    fun unmuteKeyword(word: String) = updateFilter { it.copy(mutedKeywords = it.mutedKeywords - word) }

    fun clearHidden() = updateFilter { it.copy(hiddenIds = emptySet(), hiddenUrls = emptySet()) }

    /** Every publisher seen in the current feed, so Profile can offer them for muting. */
    fun knownSources(): List<String> {
        val live = _state.value.feed?.stories.orEmpty()
            .flatMap { s -> s.sources.map { it.name }.ifEmpty { listOf(s.source) } }
        return (live + _state.value.filter.mutedSources).filter { it.isNotBlank() }.distinct().sorted()
    }

    private fun updateFilter(transform: (FeedFilter) -> FeedFilter) {
        _state.update { current ->
            val next = transform(current.filter)
            writeFilter(next)
            current.copy(filter = next)
        }
    }

    private fun readFilter(): FeedFilter {
        val raw = prefs.getString(KEY_FILTER, null) ?: return FeedFilter()
        return runCatching {
            val d = json.decodeFromString(FilterDto.serializer(), raw)
            FeedFilter(d.hiddenIds.toSet(), d.hiddenUrls.toSet(), d.mutedSources.toSet(), d.mutedKeywords.toSet())
        }.getOrDefault(FeedFilter())
    }

    private fun writeFilter(filter: FeedFilter) {
        // Hidden ids are capped: stories age out of the feed within weeks, so an
        // unbounded list would grow forever to suppress things that no longer exist.
        val dto = FilterDto(
            hiddenIds = filter.hiddenIds.toList().takeLast(500),
            hiddenUrls = filter.hiddenUrls.toList().takeLast(500),
            mutedSources = filter.mutedSources.toList(),
            mutedKeywords = filter.mutedKeywords.toList(),
        )
        runCatching { json.encodeToString(FilterDto.serializer(), dto) }
            .getOrNull()?.let { prefs.edit().putString(KEY_FILTER, it).apply() }
    }

    private fun readSaved(): List<Story> {
        val raw = prefs.getString(KEY_SAVED, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoryDto.serializer()), raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun writeSaved(stories: List<Story>) {
        val dto = stories.map { it.toDto() }
        val raw = runCatching {
            json.encodeToString(ListSerializer(StoryDto.serializer()), dto)
        }.getOrNull() ?: return
        prefs.edit().putString(KEY_SAVED, raw).apply()
    }

    private companion object {
        const val KEY_LANG = "lang"
        const val KEY_STATE = "state"
        const val KEY_DISTRICT = "district"
        const val KEY_SAVED = "saved_stories"
        const val KEY_FILTER = "feed_filter"
        const val KEY_COMPACT = "compact_feed"
        val json = Json { ignoreUnknownKeys = true }
    }
}
