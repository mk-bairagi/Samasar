"""robots.txt compliance.

RSS is published to be syndicated, so fetching a feed needs no permission. Reading
a publisher's HTML pages is different: that is crawling, and the only honest way
to do it is to ask what they allow first and abide by the answer.

This checks robots.txt per host, caches the result for the run, and fails closed —
if the rules cannot be read, the page is not fetched. A source that silently
disappears is a much smaller problem than one crawled against the publisher's
wishes.
"""

from __future__ import annotations

import logging
import threading
from urllib.parse import urlparse
from urllib.robotparser import RobotFileParser

import requests

log = logging.getLogger(__name__)


class RobotsGate:
    def __init__(self, user_agent: str, timeout: int = 10):
        self.user_agent = user_agent
        self.timeout = timeout
        self._cache: dict[str, RobotFileParser | None] = {}
        self._lock = threading.Lock()

    def _parser_for(self, url: str) -> RobotFileParser | None:
        parsed = urlparse(url)
        host = f"{parsed.scheme}://{parsed.netloc}"

        with self._lock:
            if host in self._cache:
                return self._cache[host]

        parser: RobotFileParser | None = None
        try:
            response = requests.get(
                f"{host}/robots.txt",
                headers={"User-Agent": self.user_agent},
                timeout=self.timeout,
            )
            if response.status_code == 200:
                parser = RobotFileParser()
                parser.parse(response.text.splitlines())
            elif response.status_code in (401, 403):
                # Access to the rules themselves is restricted — treat the whole
                # site as off limits rather than guessing.
                parser = None
            else:
                # No robots.txt published means no restrictions stated.
                parser = RobotFileParser()
                parser.parse([])
        except Exception as exc:  # noqa: BLE001
            log.warning("robots.txt unreadable for %s: %s", host, exc)
            parser = None

        with self._lock:
            self._cache[host] = parser
        return parser

    def allows(self, url: str) -> bool:
        parser = self._parser_for(url)
        if parser is None:
            return False
        try:
            return parser.can_fetch(self.user_agent, url)
        except Exception:  # noqa: BLE001
            return False

    def crawl_delay(self, url: str) -> float | None:
        parser = self._parser_for(url)
        if parser is None:
            return None
        try:
            delay = parser.crawl_delay(self.user_agent)
            return float(delay) if delay else None
        except Exception:  # noqa: BLE001
            return None
