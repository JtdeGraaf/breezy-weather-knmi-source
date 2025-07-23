package org.breezyweather.sources.knmi.json

import kotlinx.serialization.Serializable

@Serializable
data class KnmiFile(
    val filename: String,
    val size: Long,
    val created: String,
    val lastModified: String
)
