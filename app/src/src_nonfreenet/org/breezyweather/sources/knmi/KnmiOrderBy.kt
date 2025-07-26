package org.breezyweather.sources.knmi

enum class KnmiOrderBy(val queryParam: String) {
    FILENAME("filename"),
    LAST_MODIFIED("lastModified"),
    CREATED("created")
}
