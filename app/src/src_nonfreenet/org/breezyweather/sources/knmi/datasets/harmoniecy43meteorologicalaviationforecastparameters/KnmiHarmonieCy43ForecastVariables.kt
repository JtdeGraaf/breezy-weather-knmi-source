package org.breezyweather.sources.knmi.datasets.harmoniecy43meteorologicalaviationforecastparameters

import org.breezyweather.sources.knmi.datasets.KnmiNetcdfVariable
import ucar.ma2.DataType

/**
 * Enum representing specific variables found within a KNMI UWCW NetCDF file,
 * based on the provided metadata (likely from an air_temperature file).
 *
 * This enum lists the variables exactly as they appear in the metadata snippet.
 */
enum class KnmiHarmonieCy43ForecastVariables(
    override val variableName: String,
    override val description: String,
    override val unitDescription: String,
    override val dataType: DataType
) : KnmiNetcdfVariable {
    AIR_TEMPERATURE_HAGL(
        variableName = "air-temperature-hagl",
        description = "Air temperature at height above ground level",
        unitDescription = "C",
        dataType = DataType.FLOAT
    ),
    FORECAST_REFERENCE_TIME(
        variableName = "forecast_reference_time",
        description = "Forecast reference time",
        unitDescription = "seconds since 1970-01-01T00:00:00+00:00",
        dataType = DataType.LONG
    ),
    LATITUDE_LONGITUDE(
        variableName = "latitude_longitude",
        description = "Grid mapping for latitude and longitude",
        unitDescription = "+proj=longlat +a=6367470 +e=0 +no_defs", // proj4_params, whatever that means
        dataType = DataType.LONG // Unsure
    ),
    LONGITUDE(
        variableName = "longitude",
        description = "Longitude",
        unitDescription = "degrees_east",
        dataType = DataType.DOUBLE
    ),
    LATITUDE(
        variableName = "latitude",
        description = "Latitude",
        unitDescription = "degrees_north",
        dataType = DataType.DOUBLE
    ),
    TEMP_AT_HAGL(
        variableName = "temp_at_hagl",
        description = "Height above ground level in m",
        unitDescription = "m", // Deduced from long_name
        dataType = DataType.LONG
    ),
    TIME(
        variableName = "time",
        description = "Time",
        unitDescription = "seconds since 1970-01-01T00:00:00+00:00",
        dataType = DataType.LONG
    );
}
