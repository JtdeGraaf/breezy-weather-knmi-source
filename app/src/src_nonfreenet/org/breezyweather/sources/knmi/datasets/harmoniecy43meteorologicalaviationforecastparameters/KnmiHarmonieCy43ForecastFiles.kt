package org.breezyweather.sources.knmi.datasets.harmoniecy43meteorologicalaviationforecastparameters

import org.breezyweather.sources.knmi.datasets.KnmiDatasetFiles


enum class KnmiHarmonieCy43ForecastFiles(override val filename: String, override val variables: List<KnmiHarmonieCy43ForecastVariables>) : KnmiDatasetFiles{
    AIR_TEMPERATURE_HAGL("air-temperature-hagl", KnmiHarmonieCy43ForecastVariables.entries),
}
