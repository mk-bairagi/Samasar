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
  screens/     Home, Discover, Saved, Profile, Article
  theme/       Palette, typography
data/          Article model + placeholder feed
```

Four tabs plus an article route. Saved state, category filter, search, and the theme toggle are
all wired and functional.

---

## Placeholders to replace

- **`data/SampleFeed.kt`** — invented publishers and bylines, so nothing reads as a real story
  from a real outlet. Swap for the live repository.
- **`ui/components/Artwork.kt`** — artwork is generated from a hash of the article id rather than
  downloaded, so the app currently carries no image dependency. Replace the composable body with
  an image loader; everything else keeps working.
- Bookmarks live in memory only.

## Notes

- The icon set is hand-built on a 24dp grid (`NewsIcons.kt`), so there is no icon dependency.
- Release builds clean under R8 at ~1.1 MB.
- Measured on an API 36 emulator, debug build: ~7% janky frames while flinging the feed. Real
  hardware with a release build will be well clear of that; the emulator's GPU is the bottleneck.
  If you need headroom on low-end devices, lower `GlassStyle.blur` — it dominates the cost.
