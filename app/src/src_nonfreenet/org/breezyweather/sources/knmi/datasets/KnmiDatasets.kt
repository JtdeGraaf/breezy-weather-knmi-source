package org.breezyweather.sources.knmi.datasets

import org.breezyweather.sources.knmi.datasets.harmoniecy43meteorologicalaviationforecastparameters.KnmiHarmonieCy43ForecastFiles

/**
 * Enum representing a KNMI dataset, a single dataset can contain multiple files.
 */
enum class KnmiDatasets(val datasetName: String, val version: String, val fileExtension: String, val files: List<KnmiDatasetFiles>, val amountOfFiles: Int) {
    HARMONIE_CY43_METEOROLOGICAL_AVIATION_FORECAST_PARAMETERS("uwcw_extra_lv_ha43_nl_2km", "1.0", ".nc", KnmiHarmonieCy43ForecastFiles.entries, 18)
    // Add more datasets as needed
}
