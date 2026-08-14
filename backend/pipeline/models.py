"""Core types shared across the pipeline."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Feed:
    """One pollable RSS/Atom endpoint."""

    id: str
    url: str
    lang: str  # en | hi | gu
    scope: str  # national | state | district
    # Publisher name for display. Feed channel titles are SEO strings
    # ("Latest And Breaking Hindi News Headlines…", "mint - news") and are not
    # fit to show a reader, so the registry carries a human name instead.
    name: str = ""
    state: str | None = None
    district: str | None = None
    ua_required: bool = False
    primary_source: bool = False

    @property
    def poll_key(self) -> str:
        return self.scope


@dataclass
class Article:
    """A single story as published by one source."""

    id: str  # sha1 of the canonical URL
    url: str
    title: str
    summary: str
    image_url: str | None
    published_at: int  # epoch seconds
    fetched_at: int
    feed_id: str
    source: str
    lang: str
    scope: str
    state: str | None
    district: str | None
    simhash: int = 0
    cluster_id: str | None = None
    primary_source: bool = False


@dataclass
class Cluster:
    """A story as told by one or more sources."""

    id: str
    articles: list[Article] = field(default_factory=list)

    @property
    def lead(self) -> Article:
        """Most complete article wins the headline — longest summary, then newest."""
        return max(self.articles, key=lambda a: (len(a.summary), a.published_at))

    @property
    def source_count(self) -> int:
        return len({a.source for a in self.articles})

    @property
    def published_at(self) -> int:
        return max(a.published_at for a in self.articles)

    @property
    def has_primary_source(self) -> bool:
        return any(a.primary_source for a in self.articles)
