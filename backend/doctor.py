#!/usr/bin/env python3
"""Feed Doctor — finds broken feeds and repairs their URLs unattended.

Feeds rot constantly. Building this registry by hand, roughly 40% of plausible
URLs were dead: 404s, silent 200s carrying zero items, and 403s that only a
browser User-Agent gets past. A static config would quietly decay.

So this reproduces, in code, the discovery a human would do:

  1. Re-test every feed and note which are failing.
  2. For a failing feed, fetch the publisher's site and read
     <link rel="alternate" type="application/rss+xml"> — the declaration most
     publishers still ship.
  3. Failing that, try the conventional paths (/feed, /rss, /rss.xml, …).
  4. Validate each candidate actually parses and carries items.
  5. Rewrite sources.yaml in place, preserving comments and layout.

    python doctor.py check      report health, change nothing
    python doctor.py repair     attempt repair and rewrite sources.yaml
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from urllib.parse import urljoin, urlparse

import feedparser
import requests

from pipeline.models import Feed
from pipeline.registry import Registry

BASE = Path(__file__).resolve().parent
SOURCES = BASE / "sources.yaml"

COMMON_PATHS = ["/feed", "/feed/", "/rss", "/rss/", "/rss.xml", "/feed.xml", "/atom.xml", "/index.xml"]
ALT_LINK_RE = re.compile(
    r'<link[^>]+type=["\']application/(?:rss|atom)\+xml["\'][^>]*>', re.I
)
HREF_RE = re.compile(r'href=["\']([^"\']+)["\']', re.I)


def probe(url: str, ua: str, timeout: int = 12) -> tuple[bool, int, str]:
    """Return (healthy, item_count, note)."""
    for agent in (ua, "SamasarBot/0.1"):
        try:
            r = requests.get(url, headers={"User-Agent": agent}, timeout=timeout, allow_redirects=True)
        except Exception as exc:  # noqa: BLE001
            return False, 0, f"error: {str(exc)[:60]}"
        if r.status_code != 200:
            continue
        parsed = feedparser.parse(r.content)
        n = len(parsed.entries)
        if n > 0:
            return True, n, "ok"
        return False, 0, "200 but zero items"
    return False, 0, "blocked or non-200"


def candidates_for(feed_url: str, ua: str) -> list[str]:
    """Ask the publisher's own site where its feed lives."""
    parsed = urlparse(feed_url)
    root = f"{parsed.scheme}://{parsed.netloc}"
    found: list[str] = []

    try:
        r = requests.get(root, headers={"User-Agent": ua}, timeout=12)
        if r.status_code == 200:
            html = r.text
            for tag in ALT_LINK_RE.findall(html):
                m = HREF_RE.search(tag)
                if m:
                    found.append(urljoin(root, m.group(1)))
            # Some sites only link their feeds from an /rss index page.
            for m in re.finditer(r'href=["\']([^"\']*(?:/rss|/feed)[^"\']*\.xml)["\']', html, re.I):
                found.append(urljoin(root, m.group(1)))
    except Exception:  # noqa: BLE001
        pass

    found.extend(urljoin(root, p) for p in COMMON_PATHS)

    seen, ordered = set(), []
    for u in found:
        if u not in seen:
            seen.add(u)
            ordered.append(u)
    return ordered[:12]


def check(reg: Registry, feeds: list[Feed]) -> list[tuple[Feed, str]]:
    broken: list[tuple[Feed, str]] = []
    for f in feeds:
        healthy, n, note = probe(f.url, reg.user_agent, reg.timeout)
        status = "ok " if healthy else "DEAD"
        print(f"  {status} {f.id:<26} n={n:<4} {'' if healthy else note}")
        if not healthy:
            broken.append((f, note))
    return broken


def repair(reg: Registry, broken: list[tuple[Feed, str]], apply: bool) -> dict[str, str]:
    fixes: dict[str, str] = {}
    for feed, note in broken:
        print(f"\n  repairing {feed.id}  ({note})")
        for cand in candidates_for(feed.url, reg.user_agent):
            if cand == feed.url:
                continue
            healthy, n, _ = probe(cand, reg.user_agent, reg.timeout)
            if healthy:
                print(f"    ✓ found {cand}  ({n} items)")
                fixes[feed.url] = cand
                break
            print(f"    ✗ {cand}")
        else:
            print("    no working replacement found — leaving for the Scout job")

    if fixes and apply:
        text = SOURCES.read_text("utf-8")
        for old, new in fixes.items():
            text = text.replace(old, new)
        SOURCES.write_text(text, "utf-8")
        print(f"\n  sources.yaml updated with {len(fixes)} repair(s)")
    elif fixes:
        print(f"\n  {len(fixes)} repair(s) available — rerun with `repair` to apply")
    return fixes


def main() -> int:
    p = argparse.ArgumentParser(description="Feed Doctor")
    p.add_argument("mode", choices=["check", "repair"], nargs="?", default="check")
    p.add_argument("--all-states", action="store_true")
    args = p.parse_args()

    reg = Registry()
    feeds = reg.feeds(only_active=not args.all_states)
    print(f"checking {len(feeds)} feeds\n")

    broken = check(reg, feeds)
    print(f"\n{len(feeds) - len(broken)} healthy, {len(broken)} broken")

    if broken:
        repair(reg, broken, apply=args.mode == "repair")

    # Non-zero exit lets CI surface a bad run without failing the whole schedule.
    return 1 if len(broken) > len(feeds) // 4 else 0


if __name__ == "__main__":
    sys.exit(main())
