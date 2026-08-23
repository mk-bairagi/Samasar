"""Recover article images that the feed itself does not carry.

Several major publishers ship RSS with no image whatsoever — no media:content,
no enclosure, no <img> in the description. Amar Ujala is the important case:
it supplies half of every district feed, so without this the district view, the
whole point of the app, is a wall of generated gradients. Aaj Tak is the same.

The image is on the article page as an og:image meta tag, so this fetches the
page and reads that tag. It is deliberately the last resort:

  * only articles that are actually about to be published are fetched, not the
    whole ingest pool — that is tens of requests per run rather than thousands;
  * robots.txt is consulted first and failure is treated as refusal;
  * requests reuse the Fetcher's per-host pacing, so a publisher still sees a
    slow trickle rather than a burst;
  * results are written back to the articles table, and COALESCE there means a
    URL once found is never looked up again.

Only the meta tag is read. No article text is extracted or stored.
"""

from __future__ import annotations

import logging
import re
import threading
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urljoin, urlparse

import requests

from .fetch import BOT_UA, Fetcher
from .models import Article
from .robots import RobotsGate

log = logging.getLogger("samasar.images")

# og:image is the near-universal spelling; twitter:image is the common fallback
# and a few Indian publishers set only that one.
_META_RE = re.compile(
    r"""<meta[^>]+(?:property|name)\s*=\s*["'](og:image(?::url)?|twitter:image(?::src)?)["'][^>]*>""",
    re.I,
)
_CONTENT_RE = re.compile(r"""content\s*=\s*["']([^"']+)["']""", re.I)

# The tag lives in <head>; reading the whole page of a heavy news site to find it
# wastes bandwidth on both ends.
_HEAD_BYTES = 200_000

# The cap only bites while catching up: an image, once found, is stored, so a
# settled run only looks up the articles that arrived since the last one, which
# is a few dozen. Sized against the workflow's 20-minute timeout — 1000 lookups
# take roughly six minutes — so a backlog drains in a couple of runs while a
# runaway still cannot eat the job.
DEFAULT_BUDGET = 1000

# A thumbnail is a nice-to-have, so a slow page is not worth waiting on. The feed
# timeout is generous because a missing feed costs a whole publisher; here a
# missing image costs one picture, and 20 stalled pages at that timeout dominated
# the entire run.
IMAGE_TIMEOUT_SECONDS = 8


def extract_og_image(html: str, base_url: str) -> str | None:
    """Pull the social-preview image out of a page's <head>."""
    for match in _META_RE.finditer(html):
        content = _CONTENT_RE.search(match.group(0))
        if not content:
            continue
        url = content.group(1).strip()
        if not url:
            continue
        url = urljoin(base_url, url)  # a few publishers use protocol-relative or root-relative
        if urlparse(url).scheme in ("http", "https"):
            return url
    return None


class ImageBackfill:
    def __init__(
        self,
        fetcher: Fetcher,
        gate: RobotsGate | None = None,
        budget: int = DEFAULT_BUDGET,
        workers: int = 8,
    ):
        self.fetcher = fetcher
        self.gate = gate
        self.budget = budget
        self.workers = workers
        self._lock = threading.Lock()
        self._robots_cache: dict[str, bool] = {}

    def _allowed(self, url: str) -> bool:
        if self.gate is None:
            return True
        host = urlparse(url).netloc
        with self._lock:
            cached = self._robots_cache.get(host)
        if cached is not None:
            return cached
        try:
            verdict = self.gate.allows(url)
        except Exception:  # noqa: BLE001 - a robots lookup must not kill the run
            verdict = False
        with self._lock:
            self._robots_cache[host] = verdict
        return verdict

    def _one(self, article: Article) -> bool:
        if not self._allowed(article.url):
            return False
        try:
            self.fetcher._wait_turn(article.url)
            r = requests.get(
                article.url,
                headers={"User-Agent": BOT_UA, "Accept": "text/html,application/xhtml+xml"},
                timeout=IMAGE_TIMEOUT_SECONDS,
                allow_redirects=True,
                stream=True,
            )
            if r.status_code != 200:
                return False
            # Read only enough to cover <head> rather than the whole article.
            chunk = r.raw.read(_HEAD_BYTES, decode_content=True) or b""
            r.close()
        except Exception:  # noqa: BLE001 - one dead page must not kill the run
            return False

        image = extract_og_image(chunk.decode("utf-8", errors="replace"), article.url)
        if not image:
            return False
        article.image_url = image
        return True

    def fill(self, articles: list[Article]) -> int:
        """Populate image_url in place. Returns how many were recovered."""
        todo = [a for a in articles if not a.image_url and a.url][: self.budget]
        if not todo:
            return 0
        log.info("images: looking up %d article pages for a missing image", len(todo))
        with ThreadPoolExecutor(max_workers=self.workers) as pool:
            found = sum(pool.map(self._one, todo))
        log.info("images: recovered %d of %d", found, len(todo))
        return found


def borrow_within_cluster(clusters) -> int:
    """Give an imageless lead a picture from another source on the same story.

    Costs no requests at all: when three outlets carry a story and only one
    shipped a thumbnail, every telling of it can use that thumbnail.
    """
    filled = 0
    for cluster in clusters:
        lead = cluster.lead
        if lead.image_url:
            continue
        for other in cluster.articles:
            if other.image_url:
                lead.image_url = other.image_url
                filled += 1
                break
    return filled
