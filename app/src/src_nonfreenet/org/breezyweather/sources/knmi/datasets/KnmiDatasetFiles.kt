package org.breezyweather.sources.knmi.datasets

import org.breezyweather.sources.knmi.datasets.harmoniecy43meteorologicalaviationforecastparameters.KnmiHarmonieCy43ForecastVariables

/**
 * Interface representing a file within a KNMI dataset.
 */
interface KnmiDatasetFiles {
    val filename: String
    val variables: List<KnmiHarmonieCy43ForecastVariables>
}
