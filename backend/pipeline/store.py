"""SQLite persistence.

SQLite rather than a hosted database on purpose: the pipeline runs on a scheduler,
so a file is enough, it makes local runs identical to CI runs, and libSQL/Turso
speaks the same dialect if this ever needs to move off a single file.
"""

from __future__ import annotations

import sqlite3
import time
from pathlib import Path

from .models import Article

SCHEMA = """
CREATE TABLE IF NOT EXISTS articles (
    id            TEXT PRIMARY KEY,
    url           TEXT NOT NULL,
    title         TEXT NOT NULL,
    summary       TEXT,
    image_url     TEXT,
    published_at  INTEGER NOT NULL,
    fetched_at    INTEGER NOT NULL,
    feed_id       TEXT NOT NULL,
    source        TEXT NOT NULL,
    lang          TEXT NOT NULL,
    scope         TEXT NOT NULL,
    state         TEXT,
    district      TEXT,
    simhash       INTEGER,
    cluster_id    TEXT,
    primary_source INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_articles_pub     ON articles(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_articles_scope   ON articles(scope, state, district, lang);
CREATE INDEX IF NOT EXISTS idx_articles_cluster ON articles(cluster_id);

CREATE TABLE IF NOT EXISTS feed_state (
    feed_id       TEXT PRIMARY KEY,
    etag          TEXT,
    last_modified TEXT,
    last_polled   INTEGER,
    last_ok       INTEGER,
    fail_streak   INTEGER DEFAULT 0,
    last_error    TEXT,
    last_count    INTEGER DEFAULT 0
);
"""


_UINT64 = 1 << 64
_INT63 = 1 << 63


def _signed(v: int) -> int:
    """SQLite INTEGER is signed 64-bit; simhash is unsigned. Wrap on the way in."""
    return v - _UINT64 if v >= _INT63 else v


def _unsigned(v: int | None) -> int:
    return 0 if v is None else (v + _UINT64 if v < 0 else v)


class Store:
    def __init__(self, path: Path):
        path.parent.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(path)
        self.db.row_factory = sqlite3.Row
        self.db.executescript(SCHEMA)
        self.db.commit()

    def close(self) -> None:
        self.db.close()

    # ------------------------------------------------------------ feed state
    def http_cache(self) -> dict[str, tuple[str | None, str | None]]:
        rows = self.db.execute("SELECT feed_id, etag, last_modified FROM feed_state").fetchall()
        return {r["feed_id"]: (r["etag"], r["last_modified"]) for r in rows}

    def due_feeds(self, feed_ids: list[str], poll_minutes: dict[str, int], scopes: dict[str, str]) -> set[str]:
        """Feeds whose tier interval has elapsed. Unknown feeds are always due."""
        now = int(time.time())
        rows = {r["feed_id"]: r["last_polled"] for r in self.db.execute("SELECT feed_id, last_polled FROM feed_state")}
        due = set()
        for fid in feed_ids:
            last = rows.get(fid)
            if last is None:
                due.add(fid)
                continue
            interval = poll_minutes.get(scopes.get(fid, "national"), 15) * 60
            if now - last >= interval:
                due.add(fid)
        return due

    def record_poll(
        self,
        feed_id: str,
        *,
        ok: bool,
        etag: str | None = None,
        last_modified: str | None = None,
        error: str | None = None,
        count: int = 0,
    ) -> None:
        now = int(time.time())
        row = self.db.execute("SELECT fail_streak FROM feed_state WHERE feed_id=?", (feed_id,)).fetchone()
        streak = 0 if ok else ((row["fail_streak"] if row else 0) + 1)
        self.db.execute(
            """
            INSERT INTO feed_state (feed_id, etag, last_modified, last_polled, last_ok, fail_streak, last_error, last_count)
            VALUES (?,?,?,?,?,?,?,?)
            ON CONFLICT(feed_id) DO UPDATE SET
                etag=COALESCE(excluded.etag, feed_state.etag),
                last_modified=COALESCE(excluded.last_modified, feed_state.last_modified),
                last_polled=excluded.last_polled,
                last_ok=CASE WHEN ? THEN excluded.last_polled ELSE feed_state.last_ok END,
                fail_streak=excluded.fail_streak,
                last_error=excluded.last_error,
                last_count=excluded.last_count
            """,
            (feed_id, etag, last_modified, now, now if ok else None, streak, error, count, 1 if ok else 0),
        )

    def unhealthy_feeds(self, threshold: int = 3) -> list[sqlite3.Row]:
        return self.db.execute(
            "SELECT feed_id, fail_streak, last_error FROM feed_state WHERE fail_streak >= ? ORDER BY fail_streak DESC",
            (threshold,),
        ).fetchall()

    # -------------------------------------------------------------- articles
    def upsert_articles(self, articles: list[Article]) -> int:
        before = self.db.execute("SELECT COUNT(*) c FROM articles").fetchone()["c"]
        self.db.executemany(
            """
            INSERT INTO articles
              (id,url,title,summary,image_url,published_at,fetched_at,feed_id,source,lang,scope,state,district,simhash,cluster_id,primary_source)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
              cluster_id=excluded.cluster_id,
              simhash=excluded.simhash,
              summary=CASE WHEN length(excluded.summary) > length(articles.summary)
                           THEN excluded.summary ELSE articles.summary END,
              image_url=COALESCE(articles.image_url, excluded.image_url)
            """,
            [
                (
                    a.id, a.url, a.title, a.summary, a.image_url, a.published_at, a.fetched_at,
                    a.feed_id, a.source, a.lang, a.scope, a.state, a.district, _signed(a.simhash),
                    a.cluster_id, int(a.primary_source),
                )
                for a in articles
            ],
        )
        self.db.commit()
        after = self.db.execute("SELECT COUNT(*) c FROM articles").fetchone()["c"]
        return after - before

    def recent(self, since: int) -> list[Article]:
        rows = self.db.execute("SELECT * FROM articles WHERE published_at >= ?", (since,)).fetchall()
        return [
            Article(
                id=r["id"], url=r["url"], title=r["title"], summary=r["summary"] or "",
                image_url=r["image_url"], published_at=r["published_at"], fetched_at=r["fetched_at"],
                feed_id=r["feed_id"], source=r["source"], lang=r["lang"], scope=r["scope"],
                state=r["state"], district=r["district"], simhash=_unsigned(r["simhash"]),
                cluster_id=r["cluster_id"], primary_source=bool(r["primary_source"]),
            )
            for r in rows
        ]

    def prune(self, older_than_days: int = 21) -> int:
        """Free-tier storage only stays free with a retention policy."""
        cutoff = int(time.time()) - older_than_days * 86400
        cur = self.db.execute("DELETE FROM articles WHERE published_at < ?", (cutoff,))
        self.db.commit()
        return cur.rowcount

    def stats(self) -> dict[str, int]:
        q = self.db.execute
        return {
            "articles": q("SELECT COUNT(*) c FROM articles").fetchone()["c"],
            "clusters": q("SELECT COUNT(DISTINCT cluster_id) c FROM articles").fetchone()["c"],
            "feeds_tracked": q("SELECT COUNT(*) c FROM feed_state").fetchone()["c"],
        }
