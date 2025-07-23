package org.breezyweather.sources.knmi.json

import kotlinx.serialization.Serializable

@Serializable
data class KnmiDataset(
    val isTruncated: Boolean,
    val resultCount: Long,
    val files: List<KnmiFile>,
    val maxResults: Long,
    val startAfterFileName: String,
    val nextPageToken: String,
)
