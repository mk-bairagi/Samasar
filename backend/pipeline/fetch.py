"""Concurrent feed fetching.

Two things here are about being a good citizen rather than being fast. Conditional
GET means a feed that has not changed costs a 304 and no body, and a per-host
semaphore stops us opening fifty sockets against one publisher just because they
happen to serve fifty of our district feeds.
"""

from __future__ import annotations

import logging
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from urllib.parse import urlparse

import requests

from .models import Feed

log = logging.getLogger(__name__)

BOT_UA = "NewsProBot/0.1 (+https://github.com/mk-bairagi/News-Pro)"
MAX_PER_HOST = 4

# One publisher can serve fifty of our feeds — Patrika covers every MP district.
# Fired off back to back that reads as an attack, and Patrika starts returning
# empty documents. A small gap between requests to the same host avoids it
# entirely and costs nothing, since hosts are fetched in parallel with each other.
HOST_INTERVAL_SECONDS = 0.4


@dataclass
class FetchResult:
    feed: Feed
    status: int
    body: bytes | None
    etag: str | None = None
    last_modified: str | None = None
    error: str | None = None

    @property
    def ok(self) -> bool:
        return self.status == 200 and bool(self.body)

    @property
    def unchanged(self) -> bool:
        return self.status == 304


class Fetcher:
    def __init__(self, user_agent: str, timeout: int, workers: int = 12):
        self.browser_ua = user_agent
        self.timeout = timeout
        self.workers = workers
        self._host_locks: dict[str, threading.Semaphore] = {}
        self._host_next: dict[str, float] = {}
        self._lock = threading.Lock()

    def _host_gate(self, url: str) -> threading.Semaphore:
        host = urlparse(url).netloc
        with self._lock:
            if host not in self._host_locks:
                self._host_locks[host] = threading.Semaphore(MAX_PER_HOST)
            return self._host_locks[host]

    def _wait_turn(self, url: str) -> None:
        """Space out consecutive requests to the same publisher."""
        host = urlparse(url).netloc
        with self._lock:
            now = time.monotonic()
            earliest = self._host_next.get(host, 0.0)
            wait = max(0.0, earliest - now)
            self._host_next[host] = max(now, earliest) + HOST_INTERVAL_SECONDS
        if wait:
            time.sleep(wait)

    def fetch_one(self, feed: Feed, etag: str | None = None, modified: str | None = None) -> FetchResult:
        headers = {
            # Identify honestly by default; only impersonate a browser where the
            # publisher blocks bots outright, which sources.yaml records per feed.
            "User-Agent": self.browser_ua if feed.ua_required else BOT_UA,
            "Accept": "application/rss+xml, application/atom+xml, application/xml;q=0.9, */*;q=0.8",
            "Accept-Encoding": "gzip, deflate",
        }
        if etag:
            headers["If-None-Match"] = etag
        if modified:
            headers["If-Modified-Since"] = modified

        gate = self._host_gate(feed.url)
        with gate:
            self._wait_turn(feed.url)
            try:
                r = requests.get(feed.url, headers=headers, timeout=self.timeout, allow_redirects=True)
            except Exception as exc:  # noqa: BLE001 - a dead feed must not kill the run
                return FetchResult(feed, 0, None, error=str(exc)[:200])

        if r.status_code == 304:
            return FetchResult(feed, 304, None)
        if r.status_code != 200:
            return FetchResult(feed, r.status_code, None, error=f"HTTP {r.status_code}")

        return FetchResult(
            feed,
            200,
            r.content,
            etag=r.headers.get("ETag"),
            last_modified=r.headers.get("Last-Modified"),
        )

    def fetch_all(self, feeds: list[Feed], cache: dict[str, tuple[str | None, str | None]]) -> list[FetchResult]:
        results: list[FetchResult] = []
        with ThreadPoolExecutor(max_workers=self.workers) as pool:
            futures = [
                pool.submit(self.fetch_one, f, *cache.get(f.id, (None, None)))
                for f in feeds
            ]
            for fut in futures:
                try:
                    results.append(fut.result())
                except Exception as exc:  # noqa: BLE001
                    log.warning("fetch worker failed: %s", exc)
        return results
