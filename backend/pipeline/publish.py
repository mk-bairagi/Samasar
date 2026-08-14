"""Precomputed feed payloads.

A feed is identical for everyone reading the same place in the same language, so
it is computed once here and served as a static blob rather than queried per user.
That is what makes the serving layer fast and effectively free — the app reads a
file from a CDN edge, not a database.

Layout mirrors the scope tabs in the app: district → state → national.
"""

from __future__ import annotations

import json
import math
import time
from pathlib import Path

from .cluster import Cluster, is_lead_eligible, is_publishable
from .registry import Registry

HALF_LIFE_HOURS = 10.0


def rank_score(cluster: Cluster, now: int) -> float:
    """Recency with a corroboration boost.

    Age decays on a half-life; breadth of coverage lifts a story. A cluster ten
    papers agree on outranks a fresher single-source item, which is usually what
    a reader means by "important".
    """
    age_hours = max(0.0, (now - cluster.published_at) / 3600.0)
    freshness = math.pow(0.5, age_hours / HALF_LIFE_HOURS)
    breadth = 1.0 + math.log1p(cluster.source_count - 1) * 0.8
    return freshness * breadth


def cluster_payload(cluster: Cluster) -> dict:
    lead = cluster.lead
    sources = []
    seen = set()
    for a in sorted(cluster.articles, key=lambda x: x.published_at):
        if a.source in seen:
            continue
        seen.add(a.source)
        sources.append({"name": a.source, "url": a.url})

    return {
        "id": cluster.id,
        "title": lead.title,
        # Summary is the publisher's own standfirst, never rewritten here. Full
        # text is deliberately not carried — the app links out to read.
        "summary": lead.summary,
        "url": lead.url,
        "image": lead.image_url,
        "source": lead.source,
        "published_at": cluster.published_at,
        "source_count": cluster.source_count,
        "single_source": cluster.source_count < 2,
        "primary_source": cluster.has_primary_source,
        # Whether this story may occupy a headline slot. The app shows everything;
        # it just does not lead with uncorroborated national claims.
        "lead_eligible": is_lead_eligible(cluster),
        "sources": sources[:8],
    }


class Publisher:
    def __init__(self, registry: Registry, out_dir: Path):
        self.reg = registry
        self.out = out_dir
        self.out.mkdir(parents=True, exist_ok=True)

    def _write(self, name: str, payload: dict) -> Path:
        path = self.out / name
        path.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), "utf-8")
        return path

    def publish(self, clusters: list[Cluster], limit: int = 120) -> list[str]:
        now = int(time.time())
        publishable = [c for c in clusters if is_publishable(c)]

        buckets: dict[tuple, list[Cluster]] = {}
        for c in publishable:
            lead = c.lead
            key = (lead.scope, lead.state, lead.district, lead.lang)
            buckets.setdefault(key, []).append(c)

        written: list[str] = []
        index: dict[str, list] = {"national": [], "states": [], "districts": []}

        for (scope, state, district, lang), items in sorted(buckets.items(), key=lambda kv: str(kv[0])):
            items.sort(key=lambda c: rank_score(c, now), reverse=True)

            if scope == "national":
                name = f"india_{lang}.json"
                title = self.reg.national_name(lang)
                index["national"].append({"lang": lang, "title": title, "file": name})
            elif scope == "state":
                name = f"state_{state}_{lang}.json"
                title = self.reg.state_name(state, lang)
                index["states"].append({"state": state, "lang": lang, "title": title, "file": name})
            else:
                name = f"district_{state}_{district}_{lang}.json"
                title = self.reg.district_name(state, district, lang)
                index["districts"].append(
                    {
                        "state": state,
                        "district": district,
                        "lang": lang,
                        "title": title,
                        "aliases": self.reg.district_aliases(state, district),
                        "file": name,
                    }
                )

            self._write(
                name,
                {
                    "scope": scope,
                    "state": state,
                    "district": district,
                    "lang": lang,
                    "title": title,
                    "generated_at": now,
                    "count": min(len(items), limit),
                    "stories": [cluster_payload(c) for c in items[:limit]],
                },
            )
            written.append(name)

        index["generated_at"] = now
        index["active_states"] = self.reg.active_states
        self._write("index.json", index)
        written.append("index.json")
        return written
