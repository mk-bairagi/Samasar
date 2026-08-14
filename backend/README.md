# News Pro — ingestion pipeline

Fetches Indian news from ~95 verified RSS feeds, deduplicates, clusters the same
story across publishers, and emits precomputed JSON the Android app reads directly.

Runs unattended on a schedule. No human step anywhere in the loop.

```bash
pip install -r requirements.txt

python run.py ingest        # fetch → dedupe → cluster → publish
python run.py ingest --force  # ignore per-tier poll intervals
python run.py stats         # what is in the database
python run.py health        # feeds currently failing
python doctor.py check      # re-verify every feed
python doctor.py repair     # re-verify and fix broken URLs in place
```

## What a run does

```
sources.yaml ──▶ fetch (conditional GET, per-host limit)
                   │
                   ▼
                 parse ──▶ dedupe (SimHash) ──▶ cluster (token overlap)
                                                     │
                                                     ▼
                                          SQLite  +  public/*.json
```

Measured on a real run, MP + national only:

| | |
|---|---|
| Feeds polled | 77 |
| Raw articles | 3,449 |
| After dedup | 3,015 (434 dropped) |
| Clusters | 1,322 |
| Payloads written | 20 JSON files |
| Wall clock | 77 s |
| Database | 4.1 MB at 21-day retention |

## Design decisions worth knowing

**SimHash, not embeddings.** Dedup needs to know "is this the same text", which
a 64-bit SimHash answers in 8 bytes. An embedding would cost ~1.5 KB per article
— at these volumes that difference *is* the entire free-tier storage budget, and
it would buy nothing dedup actually needs.

**Two separate jobs: dedup and clustering.** Dedup catches the same article
arriving twice. Clustering groups the same *event* reported by different
publishers, who word headlines differently — SimHash is far too strict for that,
so clustering uses token overlap over an inverted index to stay near-linear.

Working example from a live run — one event, three publishers, three headlines:

> *Union health minister JP Nadda admitted to AIIMS Delhi*
> — Hindustan Times · News18 · Mint

**Corroboration gates headlines, not publication.** Requiring two independent
sources before publishing anything sounds prudent and measured out at
**suppressing 94% of stories**, because most legitimate reporting is single-source
when it breaks. So every story publishes; corroboration decides what may hold a
*headline slot*, and boosts ranking. District news is exempt entirely — a village
story is usually covered by exactly one local paper, and the rule would empty the
hyperlocal feed. Payloads always carry `source_count` and `single_source` so the
UI can label honestly rather than silently hide.

**Tiered polling.** National every 15 min, state 20, district 60. District news
moves slowly; this cuts daily fetches by roughly two thirds and nobody notices.

**Two freshness windows.** National and state stories are clustered over 36 hours,
districts over 7 days. Judged on the national window, a district feed of 40 items
can span weeks in a small place — 35 of 50 MP districts produced nothing at all
and disappeared from the app's picker entirely. Every registered district now gets
a payload whether or not it had news, and a district with fewer than ten stories is
topped up from the state feed. Topped-up items carry `origin: "state"` so the app
labels them instead of passing state coverage off as local.

**Conditional GET.** ETag and Last-Modified are stored per feed, so unchanged
feeds cost a 304 and no body. Confirmed working — a second run within a minute
returned 304 for every feed re-polled.

**Identity is the URL within a place, not the URL alone.** Amar Ujala serves the
same story from a district feed, the state feed and neighbouring districts. Keyed
on URL alone, whichever feed was parsed first owned it forever and every other
place lost its copy — Bhopal and Indore were capped around 45 stories, and small
districts were emptied. Dedup and clustering are likewise scoped per place. This
keeps the valuable cross-publisher merges, since national feeds share a place.

**Small districts need two publishers, not a wider window.** Amar Ujala's Neemuch
feed holds 40 items spanning four months, so on any given week it offers one
story. Patrika serves a location feed for every MP district, which roughly doubles
local coverage. Widening the freshness window instead would just serve month-old
news as today's.

**Publisher names come from the registry, not the feed.** Channel titles are SEO
strings — "Latest And Breaking Hindi News Headlines…", "mint - news". Not fit to
show a reader.

## Coverage

| Scope | Feeds | Notes |
|---|---|---|
| English national | 11 | incl. PIB, flagged `primary_source` |
| Hindi national | 7 | |
| **MP districts** | **100** | Two per district: Amar Ujala and Patrika |
| MP state | 3 | Bhaskar `category-1739`, Amar Ujala, Patrika |
| Gujarati | 12 | state-level only — see below |
| UP districts | 72 | registered, not yet live |

**Gujarat has no district-level RSS.** Divya Bhaskar publishes topic categories
only; Patrika covers Ahmedabad and Surat and nothing else — Rajkot, Vadodara,
Gandhinagar, Jamnagar, Bhavnagar and Junagadh all return empty. So Gujarat ships
state-level while MP and UP get true hyperlocal. Closing that gap is the Scout
job's problem.

Switching a state on is one line: `active_states` in `sources.yaml`.

## Place names

`places.yaml` maps feed slugs to what places are actually called, in English,
Hindi and Gujarati.

This exists because Amar Ujala still serves **Prayagraj** at `/rss/allahabad.xml`
— the city was renamed in 2018. Showing "Allahabad" to a Prayagraj reader, or
failing to match a search for "Prayagraj", is exactly the detail that makes a
local app feel foreign. `aliases` feeds the search index so both names resolve.
Same handling for Ayodhya/Faizabad, Narmadapuram/Hoshangabad, Varanasi/Banaras.

## Output

`public/` holds one file per scope × language, plus `index.json` listing every
available place with its localised title.

```
index.json
india_en.json  india_hi.json  india_gu.json
state_mp_hi.json
district_mp_bhopal_hi.json   district_mp_indore_hi.json   …
```

Each story carries title, publisher summary, canonical URL, image, source list,
`source_count`, `single_source` and `lead_eligible`. **Full article text is
deliberately not stored or served** — the app shows a summary and links out to
the publisher, which is the defensible aggregator model.

## Running in CI

`.github/workflows/ingest.yml` runs the pipeline every 15 minutes.

Two things to know before enabling it:

1. **The pipeline repo must be public.** Public repos get unlimited Actions
   minutes; private ones get 2,000/month and this schedule needs ~40–60 min/day.
   The Android app repo can stay private.
2. **The database rides in the Actions cache.** Runners are ephemeral. Eviction
   is harmless — the pipeline refills from the feeds. For durable storage, point
   `store.py` at Turso; libSQL speaks the same SQLite dialect, so it is a
   connection change and little else.

Feed payloads are pushed to a `feeds` branch and served over GitHub's CDN. Swap
that step for an R2 upload when you want a custom domain.

## Sites with no feed

Some of the best hyperlocal reporting never gets an RSS feed. `scrape.py` reads a
listing page instead, and `sources.yaml` carries the selectors under `scrapers:`.

Only worth it where the gap is real. Neemuch was the thinnest district in the app —
Amar Ujala's Neemuch feed holds 40 items spanning four months, so it yields about
one story a week. Adding Neemuch Today took the district from **2 local stories to
18**, all of it genuine local reporting.

Three rules this obeys, and they are not optional:

1. **robots.txt is checked before any request**, per host, and a disallow skips the
   source. It fails closed: if the rules cannot be read, nothing is fetched.
   Verified working — the gate permits `NewsProBot` and refuses `GPTBot`, `CCBot`
   and `ClaudeBot` exactly as Neemuch Today's robots.txt specifies.
2. **Headline and link only.** No article bodies are fetched or stored. The reader
   goes to the publisher, which is the same deal every RSS source gets.
3. **Honest User-Agent** carrying a contact URL. No pretending to be a browser to
   get past bot protection — the moment you do that, "we are within their stated
   terms" stops being true.

Neemuch Today's robots.txt states `Allow: /` with
`Content-Signal: search=yes, ai-train=no, use=reference`, which is exactly what
this does.

This is the most fragile part of the pipeline. A site redesign silently drops the
source, and unlike a moved feed URL the Doctor cannot guess its way back.

## Adding a source

Add a row to `sources.yaml` with `id`, `name`, `url`, `lang`, `scope` and — for
non-national feeds — `state`. Then `python doctor.py check` to confirm it is
alive before committing. `known_bad` at the bottom records feeds that failed
verification and why, so nobody re-adds them by guesswork.
