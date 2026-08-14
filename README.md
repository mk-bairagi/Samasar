# News Pro

A news reader for Android built around a real liquid-glass material — not a translucent
rectangle, but an optical model with refraction, chromatic dispersion and a lit rim.

**Author:** Mayank Bairagi ([@mk-bairagi](https://github.com/mk-bairagi))

Kotlin · Jetpack Compose · minSdk 26 · targetSdk 36 · compileSdk 36

```
./gradlew :app:installDebug
```

---

## The glass

Everything interesting lives in `ui/glass/`. The material is an AGSL fragment shader
(`GlassShader.kt`) that re-samples a blurred copy of whatever is behind a panel:

1. A **signed-distance field** describes the panel's rounded rectangle.
2. The **gradient of that field** gives the surface normal — which way the glass tilts.
3. A **cubic bevel profile** keeps the middle flat and loads all the bending into the last few
   millimetres of the rim. This is the single detail that separates glass from frosting: real
   glass looks undistorted in the centre and bends violently at the edge.
4. Samples are pushed **outward** along the normal, squeezing the surrounding world into the rim
   band — the lensing you see where a shape passes under the edge of a panel.
5. Red and blue are displaced by slightly different amounts, giving the **chromatic fringe**.
6. A **specular band** rides the rim where the normal faces the light, with a weaker
   counter-highlight opposite it.

`GlassStyle` exposes these as material properties (`refraction`, `thickness`, `dispersion`,
`glare`, …) with presets for `Chrome`, `Regular`, `Control` and `Lens`.

### The constraint that shapes the architecture

The obvious design — record the screen into one `GraphicsLayer`, then have each panel `drawLayer`
that into its own effect layer — **does not work**, and fails silently by rendering solid black.

A `RuntimeShader` that samples its child at anything other than the identity coordinate forces
Skia to rasterise that child into an offscreen texture, and **nested RenderNodes are dropped when
it does**. Primitive draw commands survive; a `drawLayer` call does not. The panel samples an
empty texture and every pane comes out black.

So the flow is inverted. Panels *register* with `BackdropState`, and the backdrop records content
**directly into each panel's own layer** during its own draw pass (`Backdrop.kt`). Every panel
layer then contains nothing but primitive draw commands, which materialise correctly.

Two further traps worth knowing:

- **Use the `DrawScope`-scoped `record(size) { }`.** It retargets the outer scope's canvas at the
  layer, which is what makes `drawContent()` land inside the recording. The four-argument
  `record(density, layoutDirection, size) { }` builds its own draw scope, so `drawContent()`
  quietly keeps painting to the screen and the layer comes back empty.
- **Panels fill their layer with the page colour before drawing the backdrop.** The recorded
  region reaches past the backdrop near screen edges, and the blur would otherwise drag
  transparent black inward and bruise the rim.

### Cost, and why not everything is glass

Each true glass panel costs the backdrop one extra walk of its draw tree per frame. So:

- **`Modifier.liquidGlass`** — real refraction. Reserved for floating chrome: the top bar, the nav
  bar, the article action bar, the hero caption, the preference toggles.
- **`Modifier.frostedSurface`** — tint, bevel and rim with no backdrop sampling. Used by chips,
  buttons, icon buttons and the search field. They sit *on* the chrome rather than floating over
  the feed, so a full pass buys almost nothing visually.

Content cards are deliberately not glass at all. If everything refracted, nothing would read as
being in front of anything else.

### Refracted content

Anything passed as `refracted` is painted *into* the material, so it blurs and bends with the
backdrop. That is how the nav bar's selection glow reads as embedded rather than stuck on, and how
the preference card's divider lines visibly bend as they pass under a toggle thumb.

Content that animates on its own clock must call `RefractionSync` — backdrop content changing
re-records naturally, but a spring driving something *inside* the glass changes nothing the
backdrop depends on, so without the nudge the glow lags behind its capsule.

### Nested backdrops

`BackdropState` is not a singleton. The hero card and the preference card each create a local one,
so their glass refracts their own artwork rather than the screen behind them.

### Device tiers

| API | Path |
| --- | --- |
| 33+ | Full AGSL shader: refraction, dispersion, specular rim |
| 31–32 | `RenderEffect` blur plus drawn tint and rim |
| 26–30 | Tinted pane with bevel and rim |

---

## Motion

Springs, not curves, wherever something is being *moved* rather than faded.

- **Nav bar** — the glow and capsule both stretch along the direction of travel in proportion to
  the spring's velocity, and recover as it settles.
- **Toggle** — the thumb deforms the same way, flattening across its axis of motion.
- **Press** — controls sink and spring back past their resting size. Glass has weight, not ripple.
- **Top bar** — thickens its blur and tint as content scrolls beneath it. The geometry never
  changes, only the material.
- **Article header** — parallax at 0.42× scroll, with the scrim pinned so contrast holds.
- **Ambient field** — drifting colour blobs on coprime periods over a faint grid. The grid earns
  its place: a straight line bending at a rim is what sells glass instantly.

---

## Layout

```
ui/
  glass/       Backdrop, LiquidGlass, GlassShader, nav bar, top bar, toggle, controls
  components/  Ambient field, generated artwork, story cards, icon set
  screens/     Home, Places, Saved, Profile, Story
  feed/        FeedViewModel — scope, place selection, saved stories
  theme/       Palette, typography
data/          Domain model, repository, remote layer
backend/       Ingestion pipeline (Python)
```

Four tabs plus a story route. Scope tabs, district selection, search, saved stories and the theme
toggle are all wired and functional.

**Not interested** is handled three ways, all local to the device:

- **Hide a story** — swipe it away, or use ⋯ on the row. An undo bar sits above the nav bar for a
  few seconds, because hiding is otherwise silent and irreversible.
- **Mute a publisher** — from the same ⋯ menu. A story is only hidden when *every* source carrying
  it is muted; silencing one outlet should not cost a story three others also ran.
- **Mute a keyword** — in Profile. The pipeline does not classify topics, so a keyword is both more
  honest and more precise than a topic filter would be: mute a name, a tournament, a scandal.

Hidden stories are recorded by id *and* URL. The id is the cluster root — the oldest article in the
cluster — so when that ages out of the freshness window the id changes, and a story hidden yesterday
would walk back into the feed. Everything muted is listed and reversible in Profile.

---

## Data

The app reads precomputed JSON from the ingestion pipeline in [backend/](backend/) — roughly 95
verified Indian RSS feeds, deduplicated and clustered so one event from several publishers
becomes one story carrying all its sources.

- `data/NewsRepository.kt` is the only door to news data; screens never touch the network.
- **No Room.** The payloads are immutable JSON blobs, so OkHttp's disk cache already provides
  offline reads and revalidation. A local database would duplicate that and add migrations.
- Requests are network-first and fall back to cache, because a regional reader is used on patchy
  connections and stale headlines beat an error screen.
- `ui/components/Artwork.kt` still generates artwork, now as the *fallback* when a story has no
  image — plenty of RSS items ship without one, and a grey box reads as broken.
- Saved stories are persisted whole rather than by id, since a saved story usually belongs to a
  feed the reader has since navigated away from.

### Where the feed comes from

The app reads static JSON from GitHub's CDN. There is no server anywhere — a news feed is
identical for everyone reading the same place, so it is computed once by a scheduled job and
saved as a file.

```
122 RSS feeds
     ↓  GitHub Actions, every 15 min (~40s, then the container exits)
  dedupe → cluster → 55 JSON files
     ↓  pushed to the `feeds` branch
raw.githubusercontent.com  →  the app
```

Both build types point at the live feed via `FEED_BASE_URL` in `app/build.gradle.kts`. To develop
against a local pipeline instead, run `python -m http.server 8000 --bind 127.0.0.1` in
`backend/public` and swap the debug URL for `http://10.0.2.2:8000/` — that address is the
emulator's alias for the host loopback. Bind to `127.0.0.1`, not `0.0.0.0`, or the feed is
readable by anyone on the same network.

Running costs are zero: Actions minutes are unlimited on public repositories, and the published
feed is 1.7 MB served from GitHub's CDN. Nothing about a reader — their district, saved stories or
mutes — ever leaves their phone.

## Notes

- The icon set is hand-built on a 24dp grid (`NewsIcons.kt`), so there is no icon dependency.
- Release builds clean under R8 at ~1.6 MB.
- Measured on an API 36 emulator, debug build: ~7% janky frames while flinging the feed. Real
  hardware with a release build will be well clear of that; the emulator's GPU is the bottleneck.
  If you need headroom on low-end devices, lower `GlassStyle.blur` — it dominates the cost.
