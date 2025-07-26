package org.breezyweather.sources.knmi

import android.util.Log
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.size
import androidx.compose.ui.autofill.dataType
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Temperature
import breezyweather.domain.weather.wrappers.HourlyWrapper
// Import other models like Precipitation, Wind, etc., as you build parsers for them
import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.ma2.Index
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import ucar.nc2.time.CalendarDate
import ucar.nc2.time.CalendarDateUnit
import java.util.Date
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "KnmiGridParser"

/**
 * Finds the indices of the grid cell closest to the target latitude and longitude.
 *
 * @param ncFile The NetCDF file.
 * @param targetLat Target latitude.
 * @param targetLon Target longitude.
 * @param latVarName Name of the latitude coordinate variable (e.g., "latitude").
 * @param lonVarName Name of the longitude coordinate variable (e.g., "longitude").
 * @return Pair of (latitudeIndex, longitudeIndex) or null if not found or error.
 */
fun findClosestGridPoint(
    ncFile: NetcdfFile,
    targetLat: Double,
    targetLon: Double,
    latVarName: String = "latitude", // From your metadata
    lonVarName: String = "longitude" // From your metadata
): Pair<Int, Int>? {
    val latVar = ncFile.findVariable(latVarName) ?: return null
    val lonVar = ncFile.findVariable(lonVarName) ?: return null

    val latArray: Array = latVar.read()
    val lonArray: Array = lonVar.read()

    var closestLatIdx = -1
    var minLatDiff = Double.MAX_VALUE
    for (i in 0 until latArray.size.toInt()) {
        val diff = abs(latArray.getDouble(i) - targetLat)
        if (diff < minLatDiff) {
            minLatDiff = diff
            closestLatIdx = i
        }
    }

    var closestLonIdx = -1
    var minLonDiff = Double.MAX_VALUE
    // IMPORTANT: Check if longitudes are -180 to 180 or 0 to 360 and handle wrapping if necessary
    // For now, assuming direct difference works for typical model domains.
    for (i in 0 until lonArray.size.toInt()) {
        val diff = abs(lonArray.getDouble(i) - targetLon)
        if (diff < minLonDiff) {
            minLonDiff = diff
            closestLonIdx = i
        }
    }

    return if (closestLatIdx != -1 && closestLonIdx != -1) {
        Log.d(TAG, "Closest grid point indices: Lat=$closestLatIdx (Val=${latArray.getDouble(closestLatIdx)}), Lon=$closestLonIdx (Val=${lonArray.getDouble(closestLonIdx)})")
        Pair(closestLatIdx, closestLonIdx)
    } else {
        Log.e(TAG, "Could not find closest grid point for $targetLat, $targetLon")
        null
    }
}

/**
 * Reads gridded data for a specific variable, time index, and grid point.
 * Assumes a 4D variable like var(time, height_level, lat, lon) or 3D if no height_level.
 */
fun NetcdfFile.readGridDataAtPoint(
    varName: String,
    timeIndex: Int,
    latIndex: Int,
    lonIndex: Int,
    // If a dimension for a single height level exists (e.g., temp_at_hagl=1), its index is 0
    fixedDimensionIndices: Map<String, Int> = emptyMap()
): Double? {
    val variable = this.findVariable(varName) ?: return null

    val rank = variable.rank
    val origin = IntArray(rank)
    val shape = IntArray(rank) { 1 } // Read a single point

    var timeDimIndex = -1
    var latDimIndex = -1
    var lonDimIndex = -1

    val dimensionNames = variable.dimensions.map { it.shortName }
    Log.d(TAG, "Variable $varName dimensions: $dimensionNames")


    for ((idx, dim) in variable.dimensions.withIndex()) {
        when (dim.shortName) {
            "time" -> {
                origin[idx] = timeIndex
                timeDimIndex = idx
            }
            "latitude" -> { // Match your NetCDF variable
                origin[idx] = latIndex
                latDimIndex = idx
            }
            "longitude" -> { // Match your NetCDF variable
                origin[idx] = lonIndex
                lonDimIndex = idx
            }
            else -> {
                // Handle other dimensions, like 'temp_at_hagl' if it's a fixed dimension
                val fixedIndex = fixedDimensionIndices[dim.shortName]
                if (fixedIndex != null) {
                    origin[idx] = fixedIndex
                } else {
                    // This dimension is not one we are iterating or fixing,
                    // if its length is > 1, this read will be problematic
                    // For now, assume if not specified, it's length 1 or should be handled
                    if (dim.length > 1) {
                        Log.w(TAG, "Unhandled dimension ${dim.shortName} with length ${dim.length} in variable $varName. Attempting to use index 0.")
                        origin[idx] = 0 // Default to 0 if not specified and length > 1 (could be risky)
                    } else {
                        origin[idx] = 0 // If length is 1, index 0 is fine
                    }
                }
            }
        }
    }

    // Validate that required dimensions were found
    if (timeDimIndex == -1) {
        Log.e(TAG, "Time dimension not found in variable $varName. Available: ${dimensionNames.joinToString()}")
        return null
    }
    if (latDimIndex == -1 && rank > 2) { // Allow for 1D or 2D time series not on a spatial grid if needed, but temp is spatial
        Log.e(TAG, "Latitude dimension not found in variable $varName. Available: ${dimensionNames.joinToString()}")
        return null
    }
    if (lonDimIndex == -1 && rank > 2) {
        Log.e(TAG, "Longitude dimension not found in variable $varName. Available: ${dimensionNames.joinToString()}")
        return null
    }


    try {
        val dataArray: Array = variable.read(origin, shape)
        if (dataArray.size != 1L) {
            Log.e(TAG, "Read for $varName did not return a single value. Origin: ${origin.joinToString()}, Shape: ${shape.joinToString()}")
            return null
        }

        val value = when (variable.dataType) {
            DataType.FLOAT -> dataArray.getFloat(0).toDouble()
            DataType.DOUBLE -> dataArray.getDouble(0)
            DataType.INT -> dataArray.getInt(0).toDouble()
            DataType.SHORT -> dataArray.getShort(0).toDouble()
            else -> {
                Log.w(TAG, "Unsupported data type ${variable.dataType} for $varName")
                return null
            }
        }

        val fillValueAttr = variable.findAttributeIgnoreCase("_FillValue")
        if (fillValueAttr != null && !fillValueAttr.isString) {
            val fillValue = fillValueAttr.numericValue
            if (value == fillValue) {
                Log.d(TAG, "$varName at point is fill value ($fillValue).")
                return null
            }
        } else if (value.isNaN() && variable.dataType == DataType.FLOAT) { // Check for default NaN float
            Log.d(TAG, "$varName at point is NaN _FillValue.")
            return null
        }


        return value
    } catch (e: Exception) {
        Log.e(TAG, "Error reading $varName at T:$timeIndex, Lat:$latIndex, Lon:$lonIndex: ${e.message}", e)
        return null
    }
}


/**
 * Parses a NetCDF file containing gridded temperature forecast data.
 *
 * @param tempNcFile The NetCDF file for temperature (e.g., uwcw_ha43_nl_2km_air-temperature-hagl_...).
 * @param targetLocation The location for which to extract the forecast.
 * @return List of HourlyWrapper containing temperature data.
 */
fun parseKnmiTemperatureForecast(
    tempNcFile: NetcdfFile,
    targetLocation: Location
): List<HourlyWrapper> {
    Log.i(TAG, "Parsing KNMI Temperature Forecast from file: ${tempNcFile.location}")

    val (latIdx, lonIdx) = findClosestGridPoint(
        tempNcFile,
        targetLocation.latitude,
        targetLocation.longitude
    ) ?: run {
        Log.e(TAG, "Failed to find closest grid point. Aborting temperature parsing.")
        return emptyList()
    }

    val timeVar = tempNcFile.findVariable("time") ?: run {
        Log.e(TAG, "'time' coordinate variable not found.")
        return emptyList()
    }
    val timeData: Array = timeVar.read()
    val numTimePoints = timeData.size.toInt()
    if (numTimePoints == 0) {
        Log.w(TAG, "No time points found in 'time' coordinate variable.")
        return emptyList()
    }

    val timeUnits = timeVar.findAttribute("units")?.stringValue
    if (timeUnits == null) {
        Log.e(TAG, "Time units attribute not found for 'time' coordinate variable.")
        return emptyList()
    }
    val calDateUnit = try {
        CalendarDateUnit.of(null, timeUnits)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create CalendarDateUnit from timeUnits: '$timeUnits'. Error: ${e.message}", e)
        return emptyList()
    }
    Log.d(TAG, "Time units: $timeUnits, Number of time points: $numTimePoints")

    val hourlyWrappers = mutableListOf<HourlyWrapper>()

    // For the air-temperature-hagl variable with dimension temp_at_hagl=1
    // its index will be 0. We pass this information to readGridDataAtPoint.
    val fixedDimsForTemp = mapOf("temp_at_hagl" to 0)

    for (tIdx in 0 until numTimePoints) {
        val timeVal = timeData.getDouble(tIdx)
        val calendarDate: CalendarDate = calDateUnit.makeCalendarDate(timeVal)
        val date: Date = calendarDate.toDate()

        val tempValue = tempNcFile.readGridDataAtPoint(
            varName = "air-temperature-hagl", // From your metadata
            timeIndex = tIdx,
            latIndex = latIdx,
            lonIndex = lonIdx,
            fixedDimensionIndices = fixedDimsForTemp
        )
        Log.d(TAG, "Time $date (Epoch val: $timeVal, Index: $tIdx): Raw Temp = $tempValue")


        val temperature = tempValue?.let { Temperature(temperature = it) }

        // In a real scenario, you'd merge this with data from other parameter files
        // For now, creating HourlyWrapper with only temperature
        hourlyWrappers.add(
            HourlyWrapper(
                date = date,
                temperature = temperature,
                // Initialize other fields to null or default
                isDaylight = null, // Calculate this later based on lat, lon, date
                weatherText = null,
                weatherCode = null, // Will come from a weather code parameter file or logic
                precipitation = null, // Will come from precipitation file
                precipitationProbability = null,
                wind = null, // Will come from wind file(s)
                uV = null,
                relativeHumidity = null, // Will come from humidity file/calculation
                dewPoint = null,
                pressure = null, // Will come from pressure file
                cloudCover = null, // Will come from cloud cover file
                visibility = null, // Will come from visibility file
                sunshineDuration = null
            )
        )
    }
    Log.i(TAG, "Successfully parsed ${hourlyWrappers.size} temperature data points.")
    return hourlyWrappers
}
