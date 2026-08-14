"""Loads sources.yaml and places.yaml into usable objects."""

from __future__ import annotations

from pathlib import Path

import yaml

from .models import Feed

BACKEND_DIR = Path(__file__).resolve().parent.parent


class Registry:
    def __init__(self, sources_path: Path | None = None, places_path: Path | None = None):
        self._sources = yaml.safe_load((sources_path or BACKEND_DIR / "sources.yaml").read_text("utf-8"))
        self._places = yaml.safe_load((places_path or BACKEND_DIR / "places.yaml").read_text("utf-8"))

    # ---------------------------------------------------------------- feeds
    @property
    def user_agent(self) -> str:
        return self._sources["defaults"]["user_agent"]

    @property
    def timeout(self) -> int:
        return int(self._sources["defaults"]["timeout_seconds"])

    @property
    def poll_minutes(self) -> dict[str, int]:
        return self._sources["poll_minutes"]

    @property
    def active_states(self) -> list[str]:
        return self._sources.get("active_states", [])

    def scrapers(self, only_active: bool = True) -> list[tuple[Feed, dict]]:
        """Sites read from HTML because they publish no feed."""
        out: list[tuple[Feed, dict]] = []
        for row in self._sources.get("scrapers", []):
            feed = Feed(
                id=row["id"],
                url=row["url"],
                lang=row["lang"],
                scope=row["scope"],
                name=row.get("name", ""),
                state=row.get("state"),
                district=row.get("district"),
                ua_required=bool(row.get("ua_required", False)),
            )
            if only_active and feed.state is not None and feed.state not in set(self.active_states):
                continue
            out.append((feed, row))
        return out

    def feeds(self, only_active: bool = True) -> list[Feed]:
        out: list[Feed] = []

        for row in self._sources.get("feeds", []):
            out.append(
                Feed(
                    id=row["id"],
                    url=row["url"],
                    lang=row["lang"],
                    scope=row["scope"],
                    name=row.get("name", ""),
                    state=row.get("state"),
                    district=row.get("district"),
                    ua_required=bool(row.get("ua_required", False)),
                    primary_source=bool(row.get("primary_source", False)),
                )
            )

        for tpl in self._sources.get("templates", []):
            for slug in tpl["slugs"]:
                out.append(
                    Feed(
                        id=f"{tpl['id_prefix']}_{slug}",
                        url=tpl["base"].format(slug=slug),
                        lang=tpl["lang"],
                        scope=tpl["scope"],
                        name=tpl.get("name", ""),
                        state=tpl.get("state"),
                        district=slug,
                        ua_required=bool(tpl.get("ua_required", False)),
                    )
                )

        if only_active:
            active = set(self.active_states)
            # National feeds are always in scope; state/district feeds only for live states.
            out = [f for f in out if f.state is None or f.state in active]
        return out

    # --------------------------------------------------------------- places
    def state_name(self, code: str, lang: str) -> str:
        row = self._places["states"].get(code, {})
        return row.get(lang) or row.get("en") or code

    def national_name(self, lang: str) -> str:
        row = self._places["national"]
        return row.get(lang) or row["en"]

    def district_name(self, state: str, slug: str, lang: str) -> str:
        row = self._places.get("districts", {}).get(state, {}).get(slug, {})
        return row.get(lang) or row.get("en") or slug.replace("-", " ").title()

    def district_aliases(self, state: str, slug: str) -> list[str]:
        row = self._places.get("districts", {}).get(state, {}).get(slug, {})
        return list(row.get("aliases", []))

    def districts_for(self, state: str) -> list[str]:
        return sorted(self._places.get("districts", {}).get(state, {}).keys())

    def expected_districts(self, only_active: bool = True) -> list[tuple[str, str, str]]:
        """Every (state, district, lang) that must get a payload.

        Taken from the registry rather than from whatever happened to publish this
        run. A district that goes quiet for a day must not disappear from the
        picker — least of all under a reader who has already chosen it.
        """
        seen: list[tuple[str, str, str]] = []
        for feed in self.feeds(only_active=only_active):
            if feed.scope != "district" or not feed.state or not feed.district:
                continue
            key = (feed.state, feed.district, feed.lang)
            if key not in seen:
                seen.append(key)
        return seen
