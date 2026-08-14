#!/usr/bin/env python3
"""News Pro ingestion pipeline.

    python run.py ingest    fetch → parse → dedupe → cluster → publish
    python run.py health    report feeds that are failing
    python run.py stats     database summary

Designed to run unattended on a scheduler. Nothing here waits for a human.
"""

from __future__ import annotations

import argparse
import logging
import sys
import time
from pathlib import Path

from pipeline.cluster import build_clusters, dedupe, is_lead_eligible, is_publishable
from pipeline.fetch import Fetcher
from pipeline.models import Article
from pipeline.parse import parse_feed
from pipeline.publish import Publisher
from pipeline.registry import Registry
from pipeline.store import Store

BASE = Path(__file__).resolve().parent
DB_PATH = BASE / "data" / "news.db"
OUT_DIR = BASE / "public"

log = logging.getLogger("newspro")


def setup_logging(verbose: bool) -> None:
    logging.basicConfig(
        level=logging.DEBUG if verbose else logging.INFO,
        format="%(asctime)s %(levelname)-5s %(message)s",
        datefmt="%H:%M:%S",
    )


def cmd_ingest(args: argparse.Namespace) -> int:
    started = time.time()
    reg = Registry()
    store = Store(DB_PATH)

    all_feeds = reg.feeds(only_active=not args.all_states)
    scopes = {f.id: f.scope for f in all_feeds}

    if args.force:
        due_ids = {f.id for f in all_feeds}
    else:
        due_ids = store.due_feeds([f.id for f in all_feeds], reg.poll_minutes, scopes)

    feeds = [f for f in all_feeds if f.id in due_ids]
    if args.limit:
        feeds = feeds[: args.limit]

    log.info("registry: %d feeds active, %d due this run", len(all_feeds), len(feeds))
    if not feeds:
        log.info("nothing due — exiting")
        return 0

    fetcher = Fetcher(reg.user_agent, reg.timeout, workers=args.workers)
    results = fetcher.fetch_all(feeds, store.http_cache())

    articles: list[Article] = []
    ok = unchanged = failed = 0

    for res in results:
        if res.unchanged:
            unchanged += 1
            store.record_poll(res.feed.id, ok=True)
            continue
        if not res.ok:
            failed += 1
            log.warning("  ✗ %-26s %s", res.feed.id, res.error or f"HTTP {res.status}")
            store.record_poll(res.feed.id, ok=False, error=res.error)
            continue

        try:
            parsed = parse_feed(res.feed, res.body)
        except Exception as exc:  # noqa: BLE001
            failed += 1
            store.record_poll(res.feed.id, ok=False, error=f"parse: {exc}"[:200])
            continue

        ok += 1
        articles.extend(parsed)
        store.record_poll(
            res.feed.id, ok=True, etag=res.etag, last_modified=res.last_modified, count=len(parsed)
        )

    store.db.commit()
    log.info("fetched: %d ok, %d unchanged, %d failed → %d raw articles", ok, unchanged, failed, len(articles))

    if not articles:
        log.info("no new articles")
        return 0

    deduped = dedupe(articles)
    log.info("dedupe: %d → %d (dropped %d)", len(articles), len(deduped), len(articles) - len(deduped))

    added = store.upsert_articles(deduped)
    log.info("stored: %d new rows", added)

    window = int(time.time()) - args.window_hours * 3600
    pool = store.recent(window)
    clusters = build_clusters(pool)
    multi = sum(1 for c in clusters if c.source_count > 1)
    log.info("clustered: %d articles → %d clusters (%d multi-source)", len(pool), len(clusters), multi)

    store.upsert_articles(pool)

    publishable = [c for c in clusters if is_publishable(c)]
    leads = sum(1 for c in publishable if is_lead_eligible(c))
    log.info(
        "quality: %d publishable (%d dropped), %d eligible for headline slots",
        len(publishable), len(clusters) - len(publishable), leads,
    )

    written = Publisher(reg, OUT_DIR).publish(clusters)
    log.info("published: %d files → %s", len(written), OUT_DIR)

    removed = store.prune(args.retain_days)
    if removed:
        log.info("pruned: %d articles older than %d days", removed, args.retain_days)

    log.info("done in %.1fs | %s", time.time() - started, store.stats())
    store.close()
    return 0


def cmd_health(_: argparse.Namespace) -> int:
    store = Store(DB_PATH)
    bad = store.unhealthy_feeds()
    if not bad:
        print("all tracked feeds healthy")
    else:
        print(f"{len(bad)} feeds failing:\n")
        for row in bad:
            print(f"  {row['feed_id']:<28} streak={row['fail_streak']:<3} {row['last_error']}")
    store.close()
    return 0


def cmd_stats(_: argparse.Namespace) -> int:
    store = Store(DB_PATH)
    stats = store.stats()
    for k, v in stats.items():
        print(f"  {k:<16} {v}")
    rows = store.db.execute(
        "SELECT scope, state, district, lang, COUNT(*) c FROM articles "
        "GROUP BY scope, state, district, lang ORDER BY c DESC LIMIT 15"
    ).fetchall()
    print("\n  top buckets:")
    for r in rows:
        place = r["district"] or r["state"] or "—"
        print(f"    {r['scope']:<9} {place:<16} {r['lang']}  {r['c']}")
    store.close()
    return 0


def main() -> int:
    p = argparse.ArgumentParser(description="News Pro ingestion")
    p.add_argument("-v", "--verbose", action="store_true")
    sub = p.add_subparsers(dest="cmd", required=True)

    ing = sub.add_parser("ingest")
    ing.add_argument("--force", action="store_true", help="ignore poll intervals")
    ing.add_argument("--all-states", action="store_true", help="include states not yet live")
    ing.add_argument("--limit", type=int, default=0, help="cap feeds this run (testing)")
    ing.add_argument("--workers", type=int, default=12)
    ing.add_argument("--window-hours", type=int, default=36)
    ing.add_argument("--retain-days", type=int, default=21)
    ing.set_defaults(func=cmd_ingest)

    sub.add_parser("health").set_defaults(func=cmd_health)
    sub.add_parser("stats").set_defaults(func=cmd_stats)

    args = p.parse_args()
    setup_logging(args.verbose)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
