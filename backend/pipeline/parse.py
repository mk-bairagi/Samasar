"""Feed bytes → Article objects."""

from __future__ import annotations

import hashlib
import html
import re
import time
from calendar import timegm
from urllib.parse import urlparse, urlunparse

import feedparser

from .models import Article, Feed

TAG_RE = re.compile(r"<[^>]+>")
WS_RE = re.compile(r"\s+")

# Tracking parameters that change per-visit and would otherwise defeat dedup by
# making the same story look like several different URLs.
JUNK_PARAMS = ("utm_", "fbclid", "gclid", "ref", "source", "src", "amp")


def canonical_url(url: str) -> str:
    try:
        p = urlparse(url.strip())
    except ValueError:
        return url.strip()
    query = "&".join(
        part
        for part in p.query.split("&")
        if part and not part.split("=")[0].lower().startswith(JUNK_PARAMS)
    )
    netloc = p.netloc.lower().removeprefix("www.")
    path = p.path.rstrip("/") or "/"
    return urlunparse((p.scheme or "https", netloc, path, "", query, ""))


def clean_text(raw: str | None, limit: int = 400) -> str:
    if not raw:
        return ""
    text = html.unescape(TAG_RE.sub(" ", raw))
    text = WS_RE.sub(" ", text).strip()
    return text[:limit]


def _entry_image(entry) -> str | None:
    for key in ("media_content", "media_thumbnail"):
        media = entry.get(key) or []
        if media and isinstance(media, list) and media[0].get("url"):
            return media[0]["url"]
    for link in entry.get("links", []) or []:
        if str(link.get("type", "")).startswith("image/") and link.get("href"):
            return link["href"]
    for key in ("summary", "description", "content"):
        blob = entry.get(key)
        if isinstance(blob, list) and blob:
            blob = blob[0].get("value", "")
        if isinstance(blob, str):
            m = re.search(r'<img[^>]+src=["\']([^"\']+)', blob)
            if m:
                return m.group(1)
    return None


def _published(entry, fallback: int) -> int:
    for key in ("published_parsed", "updated_parsed"):
        parsed = entry.get(key)
        if parsed:
            try:
                return timegm(parsed)
            except Exception:  # noqa: BLE001
                continue
    return fallback


def parse_feed(feed: Feed, body: bytes) -> list[Article]:
    parsed = feedparser.parse(body)
    source = feed.name or clean_text(parsed.feed.get("title"), 80) or feed.id
    now = int(time.time())
    out: list[Article] = []

    for entry in parsed.entries:
        link = entry.get("link") or ""
        title = clean_text(entry.get("title"), 300)
        if not link or not title:
            continue

        url = canonical_url(link)
        published = _published(entry, now)
        # Guard against feeds that publish absurd future dates, which would
        # otherwise pin junk to the top of every ranking forever.
        if published > now + 3600:
            published = now

        out.append(
            Article(
                id=hashlib.sha1(url.encode("utf-8")).hexdigest(),
                url=url,
                title=title,
                summary=clean_text(entry.get("summary") or entry.get("description")),
                image_url=_entry_image(entry),
                published_at=published,
                fetched_at=now,
                feed_id=feed.id,
                source=source,
                lang=feed.lang,
                scope=feed.scope,
                state=feed.state,
                district=feed.district,
                primary_source=feed.primary_source,
            )
        )
    return out
