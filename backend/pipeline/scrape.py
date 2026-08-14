"""HTML extraction for publishers with no feed.

Some of the best hyperlocal reporting never gets an RSS feed. Neemuch Today runs
a custom PHP site; Naidunia, the big Indore daily, publishes nothing machine
readable either. Ignoring them means the districts that most need local coverage
get the least.

This reads a listing page and pulls out headline, link, image and date. It is
deliberately thin:

* **Headline and link only.** No article bodies are fetched or stored, so the
  app shows a headline and sends the reader to the publisher — the same deal the
  RSS sources get, and the only version that is defensible.
* **robots.txt is checked before anything is requested**, and a host that
  disallows us is skipped entirely.
* **Config, not code.** A new site is a few lines in sources.yaml. Selectors live
  in configuration because they are what breaks when a site is redesigned, and
  editing YAML is a smaller job than editing a parser.

It is still the most fragile part of the pipeline. A redesign silently drops the
source, and unlike a moved feed URL the Doctor cannot guess its way back.
"""

from __future__ import annotations

import hashlib
import logging
import re
import time
from datetime import datetime
from urllib.parse import urljoin

from bs4 import BeautifulSoup

from .models import Article, Feed
from .parse import canonical_url, clean_text

log = logging.getLogger(__name__)

DATE_FORMATS = ("%B %d, %Y", "%d %B %Y", "%d-%m-%Y", "%Y-%m-%d")


def _parse_date(text: str, fallback: int) -> int:
    cleaned = " ".join(text.split())
    for fmt in DATE_FORMATS:
        try:
            return int(datetime.strptime(cleaned, fmt).timestamp())
        except ValueError:
            continue
    return fallback


def scrape_listing(feed: Feed, body: bytes, config: dict) -> list[Article]:
    """Turn a listing page into articles using the selectors in `config`."""
    soup = BeautifulSoup(body, "html.parser")
    base = config.get("base_url") or feed.url
    link_pattern = re.compile(config["link_pattern"])
    min_title = int(config.get("min_title_length", 15))
    now = int(time.time())

    # Images and dates are collected page-wide rather than per-article: these
    # layouts rarely nest them inside the headline's own element, and a wrong
    # image is worse than none, so they are matched by position and only used
    # when the counts line up.
    images = [
        urljoin(base, img["src"])
        for img in soup.find_all("img", src=True)
        if re.search(config.get("image_pattern", r"(?!)"), img["src"])
    ]
    dates = [
        _parse_date(node.get_text(" ", strip=True), now)
        for node in soup.select(config.get("date_selector", "")) if config.get("date_selector")
    ]

    seen: dict[str, Article] = {}
    for index, anchor in enumerate(soup.find_all("a", href=link_pattern)):
        title = " ".join(anchor.get_text(" ", strip=True).split())
        # Strip the promotional handle these sites append to every headline.
        for suffix in config.get("strip_suffixes", []):
            title = title.split(suffix)[0].strip(" |·-–—")
        if len(title) < min_title:
            continue

        url = canonical_url(urljoin(base, anchor["href"]))
        if url in seen:
            continue

        position = len(seen)
        place_key = f"{url}|{feed.scope}|{feed.state or ''}|{feed.district or ''}"

        seen[url] = Article(
            id=hashlib.sha1(place_key.encode("utf-8")).hexdigest(),
            url=url,
            title=clean_text(title, 300),
            # No summary: the listing page has none, and fetching each article to
            # manufacture one would mean pulling content we have no business
            # storing.
            summary="",
            image_url=images[position] if position < len(images) else None,
            published_at=dates[position] if position < len(dates) else now,
            fetched_at=now,
            feed_id=feed.id,
            source=feed.name or feed.id,
            lang=feed.lang,
            scope=feed.scope,
            state=feed.state,
            district=feed.district,
            primary_source=feed.primary_source,
        )

    return list(seen.values())
