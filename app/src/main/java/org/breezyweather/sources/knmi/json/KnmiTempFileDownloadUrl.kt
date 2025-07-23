package org.breezyweather.sources.knmi.json

data class KnmiTempFileDownloadUrl(
    val contentDisposition: String,
    val lastModified: String,
    val size: Long,
    val temporaryDownloadUrl: String,
)
