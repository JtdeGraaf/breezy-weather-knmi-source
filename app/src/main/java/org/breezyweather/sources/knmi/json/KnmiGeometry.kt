package org.breezyweather.sources.knmi.json

import kotlinx.serialization.Serializable

@Serializable
data class KnmiGeometry(
    val type: String,
    val coordinates: Pair<Double, Double>
)
