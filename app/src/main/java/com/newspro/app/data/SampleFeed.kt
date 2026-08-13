package com.newspro.app.data

/**
 * Placeholder feed used to build and preview the UI.
 *
 * Publishers and bylines here are invented so nothing in the layout reads as a real story from a
 * real outlet. Swap this object for the live news repository when the API lands.
 */
object SampleFeed {

    val categories = listOf("Top", "World", "Tech", "Business", "Science", "Sports", "Culture")

    private val lorem = listOf(
        "The shift did not arrive all at once. It accumulated over eighteen months of quiet " +
            "procedural changes, each one defensible on its own terms, until the shape of the " +
            "thing became impossible to miss.",
        "Officials briefed on the process described a deliberate sequencing, with the least " +
            "contested measures moving first and the harder questions deferred to a review that " +
            "has now been scheduled twice and held neither time.",
        "What follows is an account assembled from records, published filings and conversations " +
            "with people on both sides of the negotiation, several of whom asked not to be named " +
            "because they were not authorised to discuss it.",
        "The practical effect is narrower than the headline suggests, but the precedent is not. " +
            "Analysts expect the framework to be cited well beyond the sector it was written for.",
        "For now the parties have agreed to keep talking. The next round is expected before the " +
            "end of the quarter, and neither side has signalled a willingness to move first.",
    )

    val articles: List<Article> = listOf(
        Article(
            id = "a1",
            title = "The quiet rewrite of how cities measure their own air",
            summary = "A new sensor standard is spreading faster than the rules written to " +
                "govern it, and the numbers are starting to disagree.",
            body = lorem,
            category = "Science",
            source = "The Meridian",
            author = "R. Advani",
            readMinutes = 8,
            publishedAgo = "12m",
            isLive = true,
        ),
        Article(
            id = "a2",
            title = "Chip demand cools, but only in the places nobody was watching",
            summary = "Foundry bookings slipped for a second quarter while packaging capacity " +
                "stayed sold out through next winter.",
            body = lorem,
            category = "Tech",
            source = "Orbital Review",
            author = "M. Okonjo",
            readMinutes = 6,
            publishedAgo = "48m",
        ),
        Article(
            id = "a3",
            title = "Central banks are running out of room to disagree",
            summary = "Three rate paths, one currency corridor, and a growing sense that the " +
                "spread cannot hold much longer.",
            body = lorem,
            category = "Business",
            source = "Ledger Daily",
            author = "S. Whitfield",
            readMinutes = 11,
            publishedAgo = "1h",
        ),
        Article(
            id = "a4",
            title = "A border town rebuilds twice in one year",
            summary = "The second reconstruction is going faster. Residents say that is not " +
                "the comfort it sounds like.",
            body = lorem,
            category = "World",
            source = "The Meridian",
            author = "L. Haddad",
            readMinutes = 9,
            publishedAgo = "2h",
        ),
        Article(
            id = "a5",
            title = "The transfer window that broke the model",
            summary = "Clubs spent against projections that assumed a ceiling. The ceiling " +
                "turned out to be a suggestion.",
            body = lorem,
            category = "Sports",
            source = "Sideline",
            author = "D. Ferreira",
            readMinutes = 5,
            publishedAgo = "3h",
        ),
        Article(
            id = "a6",
            title = "Museums are quietly returning things nobody asked about",
            summary = "Restitution used to begin with a claim. Increasingly it begins with an " +
                "inventory.",
            body = lorem,
            category = "Culture",
            source = "Longform Weekly",
            author = "A. Bergström",
            readMinutes = 14,
            publishedAgo = "4h",
        ),
        Article(
            id = "a7",
            title = "Grid operators found the flexibility they said did not exist",
            summary = "It was in the water heaters, and it had been there the whole time.",
            body = lorem,
            category = "Science",
            source = "Orbital Review",
            author = "T. Nakamura",
            readMinutes = 7,
            publishedAgo = "5h",
        ),
        Article(
            id = "a8",
            title = "Shipping rates fall while insurance climbs",
            summary = "The two curves have not moved together since spring, and the gap is now " +
                "wider than the freight itself.",
            body = lorem,
            category = "Business",
            source = "Ledger Daily",
            author = "P. Marchetti",
            readMinutes = 6,
            publishedAgo = "6h",
        ),
        Article(
            id = "a9",
            title = "The election that will be decided by three hundred ballots",
            summary = "Both campaigns have already filed. Neither expects the count to be the " +
                "last word.",
            body = lorem,
            category = "World",
            source = "The Meridian",
            author = "J. Okafor",
            readMinutes = 10,
            publishedAgo = "8h",
            isLive = true,
        ),
        Article(
            id = "a10",
            title = "On-device models are getting small enough to be boring",
            summary = "Which is, by every measure that matters to the people shipping them, the " +
                "goal.",
            body = lorem,
            category = "Tech",
            source = "Orbital Review",
            author = "K. Sandoval",
            readMinutes = 8,
            publishedAgo = "10h",
        ),
        Article(
            id = "a11",
            title = "A stadium built for a tournament that moved",
            summary = "Eleven years later, the maintenance contract is still the largest line " +
                "item in the municipal budget.",
            body = lorem,
            category = "Sports",
            source = "Sideline",
            author = "N. Petrov",
            readMinutes = 12,
            publishedAgo = "12h",
        ),
        Article(
            id = "a12",
            title = "The archive that only exists because someone forgot to delete it",
            summary = "Four decades of regional broadcast, recovered from a decommissioned tape " +
                "library in a basement.",
            body = lorem,
            category = "Culture",
            source = "Longform Weekly",
            author = "E. Lindqvist",
            readMinutes = 15,
            publishedAgo = "14h",
        ),
        Article(
            id = "a13",
            title = "Water rights are being traded faster than they are being measured",
            summary = "Regulators approved the exchange in 2019 on the assumption that metering " +
                "would follow.",
            body = lorem,
            category = "World",
            source = "The Meridian",
            author = "C. Mwangi",
            readMinutes = 9,
            publishedAgo = "16h",
        ),
        Article(
            id = "a14",
            title = "Two labs, one result, and a decade of disagreement",
            summary = "The replication finally worked. Nobody can agree on what it proves.",
            body = lorem,
            category = "Science",
            source = "Orbital Review",
            author = "H. Vasquez",
            readMinutes = 13,
            publishedAgo = "18h",
        ),
    )

    val hero: Article = articles.first()

    val trending: List<Topic> = listOf(
        Topic("Grid Storage", 42, "Science"),
        Topic("Rate Decision", 88, "Business"),
        Topic("Transfer Window", 31, "Sports"),
        Topic("On-Device AI", 64, "Tech"),
        Topic("Restitution", 19, "Culture"),
        Topic("Water Rights", 27, "World"),
    )

    val publishers: List<String> = listOf(
        "The Meridian",
        "Orbital Review",
        "Ledger Daily",
        "Sideline",
        "Longform Weekly",
    )

    fun byCategory(category: String): List<Article> =
        if (category == "Top") articles else articles.filter { it.category == category }

    fun byId(id: String): Article = articles.firstOrNull { it.id == id } ?: hero

    /** Naive substring match across the fields a reader would expect to search. */
    fun search(query: String): List<Article> {
        val q = query.trim()
        if (q.isBlank()) return articles
        return articles.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.summary.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                it.source.contains(q, ignoreCase = true) ||
                it.author.contains(q, ignoreCase = true)
        }
    }
}

fun Topic.matches(query: String): Boolean {
    val q = query.trim()
    return q.isBlank() ||
        name.contains(q, ignoreCase = true) ||
        category.contains(q, ignoreCase = true)
}
