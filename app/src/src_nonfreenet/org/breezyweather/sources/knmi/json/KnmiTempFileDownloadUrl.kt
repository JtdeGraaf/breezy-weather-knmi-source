package org.breezyweather.sources.knmi.json

import kotlinx.serialization.Serializable

@Serializable
data class KnmiTempFileDownloadUrl(
    val contentType: String,
    val lastModified: String,
    val size: Long,
    val temporaryDownloadUrl: String,
)
