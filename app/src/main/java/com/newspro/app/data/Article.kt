package com.newspro.app.data

import androidx.compose.runtime.Immutable

@Immutable
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val body: List<String>,
    val category: String,
    val source: String,
    val author: String,
    val readMinutes: Int,
    val publishedAgo: String,
    val isLive: Boolean = false,
)

@Immutable
data class Topic(
    val name: String,
    val storyCount: Int,
    val category: String,
)
