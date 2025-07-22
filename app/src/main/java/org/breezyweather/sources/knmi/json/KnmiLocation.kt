package org.breezyweather.sources.knmi.json

import kotlinx.serialization.Serializable

@Serializable
data class KnmiLocation(
    val type: String,
    val geometry: KnmiGeometry,
    val properties: KnmiProperties,
    val id: String,
)
