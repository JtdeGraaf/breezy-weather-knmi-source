package org.breezyweather.sources.knmi.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KnmiProperties(
    val name: String,
    val wmoId: String,
    @SerialName("height_above_mean_sea_level")
    val heightAboveMeanSeaLevel: Double
)
