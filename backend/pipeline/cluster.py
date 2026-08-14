"""Deduplication and story clustering.

Two different jobs, deliberately kept apart:

* **Dedup** catches the same article arriving twice — a re-fetch, or verbatim
  syndication. SimHash over the title handles this, and it costs 8 bytes per
  article instead of the ~1.5KB an embedding would. At the volumes here that
  difference is the whole free-tier storage budget.
* **Clustering** groups the same *event* reported by different publishers, who
  word their headlines differently. SimHash is too strict for that, so this uses
  token overlap with an inverted index to keep it from going quadratic.
"""

from __future__ import annotations

import hashlib
import re
from collections import defaultdict

from .models import Article, Cluster

TOKEN_RE = re.compile(r"[\wऀ-ॿ઀-૿]+", re.UNICODE)

STOPWORDS = {
    # english
    "the", "a", "an", "and", "or", "of", "in", "on", "to", "for", "at", "by", "with",
    "is", "are", "was", "were", "be", "been", "from", "as", "that", "this", "it",
    "after", "over", "into", "up", "out", "new", "says", "said", "will", "not",
    # hindi
    "का", "के", "की", "को", "में", "से", "पर", "है", "हैं", "था", "थे", "थी", "और",
    "एक", "यह", "वह", "ने", "कर", "किया", "लिए", "भी", "तक", "हो", "गया", "गई",
    # gujarati
    "ના", "ની", "નું", "માં", "થી", "પર", "છે", "હતો", "હતી", "અને", "એક", "આ", "તે",
    "માટે", "કરી", "થયો", "થઈ",
}

DEDUP_HAMMING = 3          # near-identical text
CLUSTER_JACCARD = 0.42     # same event, different wording
CLUSTER_WINDOW = 36 * 3600  # only cluster stories within a day and a half
MAX_CANDIDATES = 60        # cost ceiling per article


def tokens(text: str) -> list[str]:
    return [t.lower() for t in TOKEN_RE.findall(text) if len(t) > 1]


def content_tokens(article: Article) -> set[str]:
    raw = tokens(article.title) + tokens(article.summary)[:40]
    return {t for t in raw if t not in STOPWORDS}


def simhash(text: str, bits: int = 64) -> int:
    """Charikar simhash over token digests."""
    vector = [0] * bits
    toks = [t for t in tokens(text) if t not in STOPWORDS] or tokens(text)
    if not toks:
        return 0
    for tok in toks:
        digest = int.from_bytes(hashlib.blake2b(tok.encode("utf-8"), digest_size=8).digest(), "big")
        for i in range(bits):
            vector[i] += 1 if (digest >> i) & 1 else -1
    out = 0
    for i, v in enumerate(vector):
        if v > 0:
            out |= 1 << i
    return out


def hamming(a: int, b: int) -> int:
    return bin(a ^ b).count("1")


def jaccard(a: set[str], b: set[str]) -> float:
    if not a or not b:
        return 0.0
    inter = len(a & b)
    return inter / (len(a) + len(b) - inter)


def dedupe(articles: list[Article]) -> list[Article]:
    """Drop near-identical duplicates, keeping the richest copy of each."""
    for a in articles:
        a.simhash = simhash(f"{a.title} {a.summary[:120]}")

    kept: list[Article] = []
    buckets: dict[int, list[Article]] = defaultdict(list)

    for a in sorted(articles, key=lambda x: (-len(x.summary), x.published_at)):
        # Band the hash so we only compare plausible neighbours.
        band_keys = [(a.simhash >> shift) & 0xFFFF for shift in (0, 16, 32, 48)]
        duplicate = False
        for key in band_keys:
            for other in buckets[key]:
                if other.url == a.url or hamming(a.simhash, other.simhash) <= DEDUP_HAMMING:
                    duplicate = True
                    break
            if duplicate:
                break
        if duplicate:
            continue
        kept.append(a)
        for key in band_keys:
            buckets[key].append(a)
    return kept


def build_clusters(articles: list[Article]) -> list[Cluster]:
    """Group articles describing the same event, across publishers."""
    ordered = sorted(articles, key=lambda a: a.published_at, reverse=True)
    token_sets = {a.id: content_tokens(a) for a in ordered}

    parent: dict[str, str] = {a.id: a.id for a in ordered}

    def find(x: str) -> str:
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(x: str, y: str) -> None:
        rx, ry = find(x), find(y)
        if rx != ry:
            parent[ry] = rx

    # Inverted index on content tokens keeps this near-linear instead of O(n^2).
    index: dict[str, list[Article]] = defaultdict(list)

    for a in ordered:
        toks = token_sets[a.id]
        seen: dict[str, int] = defaultdict(int)
        for t in toks:
            for other in index[t]:
                seen[other.id] += 1

        candidates = sorted(seen.items(), key=lambda kv: -kv[1])[:MAX_CANDIDATES]
        by_id = {o.id: o for t in toks for o in index[t]}

        for other_id, shared in candidates:
            if shared < 2:
                continue
            other = by_id[other_id]
            if abs(a.published_at - other.published_at) > CLUSTER_WINDOW:
                continue
            # Never merge across languages — the same event in Hindi and English
            # is two stories for two different readers.
            if other.lang != a.lang:
                continue
            if jaccard(toks, token_sets[other.id]) >= CLUSTER_JACCARD:
                union(a.id, other.id)

        for t in toks:
            index[t].append(a)

    grouped: dict[str, Cluster] = {}
    for a in ordered:
        root = find(a.id)
        cluster = grouped.setdefault(root, Cluster(id=root))
        cluster.articles.append(a)
        a.cluster_id = root
    return list(grouped.values())


def is_publishable(cluster: Cluster) -> bool:
    """Basic quality gate — is this a usable story at all?

    Note what this deliberately is *not*: a corroboration gate. Requiring two
    independent sources before publishing anything sounds prudent and is, in
    practice, ruinous — measured against the live feeds it suppressed 94% of
    stories, because most legitimate reporting is carried by one outlet at the
    moment it breaks. That is not a safety filter, it is a broken product.

    Corroboration belongs in ranking and labelling instead. See
    [is_lead_eligible] for the stricter gate that guards the headline slots.
    """
    lead = cluster.lead
    return bool(lead.title.strip()) and bool(lead.url) and len(lead.title) > 12


def is_lead_eligible(cluster: Cluster) -> bool:
    """May this story hold a headline slot?

    The top of the feed is the one place where a single unverified claim does
    real damage, so it demands corroboration: two independent sources, or a
    designated primary source such as PIB.

    District news is exempt by design. A village story is very often covered by
    exactly one local paper, and applying this rule there would empty the
    hyperlocal feed — the one thing this app is meant to be good at. Every
    payload carries source_count and single_source so the UI can label honestly
    rather than silently hide.
    """
    if cluster.lead.scope == "district":
        return True
    return cluster.source_count >= 2 or cluster.has_primary_source
