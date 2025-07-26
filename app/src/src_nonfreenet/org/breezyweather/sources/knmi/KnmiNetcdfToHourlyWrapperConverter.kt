package org.breezyweather.sources.knmi

import android.util.Log
import breezyweather.domain.location.model.Location
import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import ucar.nc2.time.CalendarDateUnit

import breezyweather.domain.weather.model.Temperature
import breezyweather.domain.weather.model.Precipitation
import breezyweather.domain.weather.model.Wind
import breezyweather.domain.weather.wrappers.HourlyWrapper


// Helper function to safely read a double value from a variable at a specific station and time
// This helper needs to know the dimension order or be flexible
internal fun NetcdfFile.readStationTimeData(
    varName: String,
    stationIndex: Int,
    timeIndex: Int,
    stationDimIndex: Int, // 0 if (station, time), 1 if (time, station)
    timeDimIndex: Int,    // 1 if (station, time), 0 if (time, station)
    fillValueOverride: Double? = null,
): Double? {
    val variable = this.findVariable(varName) ?: return null
    if (variable.rank != 2) {
        Log.w("NcReadHelper", "Variable $varName is not rank 2, skipping.")
        return null
    }

    val origin = IntArray(2)
    origin[stationDimIndex] = stationIndex
    origin[timeDimIndex] = timeIndex
    val shape = intArrayOf(1, 1) // To read a single point

    try {
        val dataArray: Array = variable.read(origin, shape)
        if (dataArray.size != 1L) return null

        val value = when (variable.dataType) {
            DataType.FLOAT -> dataArray.getFloat(0).toDouble()
            DataType.DOUBLE -> dataArray.getDouble(0)
            DataType.INT -> dataArray.getInt(0).toDouble()
            DataType.SHORT -> dataArray.getShort(0).toDouble()
            else -> {
                Log.w("NcReadHelper", "Unsupported data type ${variable.dataType} for $varName")
                return null
            }
        }
        val fillValue = fillValueOverride
            ?: variable.findAttribute("_FillValue")?.numericValue?.toDouble()
            ?: -9999.0 // Default fallback

        if (value == fillValue) {
            Log.d("NcReadHelper", "$varName at [st:$stationIndex, t:$timeIndex] is fill value ($fillValue).")
            return null
        }
        return value
    } catch (e: Exception) {
        Log.e("NcReadHelper", "Error reading $varName at [st:$stationIndex, t:$timeIndex]: ${e.message}", e)
        return null
    }
}


fun parseKnmiDataToHourlyWrappers(
    // Renamed to plural
    ncFile: NetcdfFile,
    targetLocation: Location,
): List<HourlyWrapper> {

    val latVar: Variable? = ncFile.findVariable("lat")
    val lonVar: Variable? = ncFile.findVariable("lon")
    val timeCoordVar: Variable? = ncFile.findVariable("time") // Time Coordinate Variable

    if (latVar == null || lonVar == null || timeCoordVar == null) {
        Log.e("KnmiParser", "Core coordinate variables (lat, lon, time) not found.")
        return emptyList()
    }

    // --- 1. Find the closest station index ---
    val latData: Array = latVar.read()
    val lonData: Array = lonVar.read()
    val numStations = latData.size.toInt()
    if (numStations == 0) return emptyList()

    var closestStationIndex = -1
    var minDistanceSquared = Double.MAX_VALUE
    for (i in 0 until numStations) {
        val stationLat = latData.getDouble(i)
        val stationLon = lonData.getDouble(i)
        val latDiff = stationLat - targetLocation.latitude
        val lonDiff = stationLon - targetLocation.longitude
        val distanceSquared = (latDiff * latDiff) + (lonDiff * lonDiff)
        if (distanceSquared < minDistanceSquared) {
            minDistanceSquared = distanceSquared
            closestStationIndex = i
        }
    }
    if (closestStationIndex == -1) {
        Log.e("KnmiParser", "Could not find closest station.")
        return emptyList()
    }
    Log.i("KnmiParser", "Using data for station index: $closestStationIndex")

    // --- 2. Get the time values and units ---
    val timeUnits = timeCoordVar.findAttribute("units")?.stringValue
    if (timeUnits == null) {
        Log.e("KnmiParser", "Time units attribute not found for 'time' coordinate variable.")
        return emptyList()
    }
    val calDateUnit = CalendarDateUnit.of(null, timeUnits)
    val timeValuesArray: Array = timeCoordVar.read() // Read all time values
    val numTimePoints = timeValuesArray.size.toInt()

    if (numTimePoints == 0) {
        Log.w("KnmiParser", "No time points found in 'time' coordinate variable.")
        return emptyList()
    }

    val hourlyWrappers = mutableListOf<HourlyWrapper>()

    // Determine dimension order for data variables (e.g., 'ta')
    // This is crucial. You might need to inspect a sample file or KNMI docs.
    // Let's assume (station, time) for now for most variables like 'ta', 'ff', 'dd' etc.
    // If it's (time, station), these indices need to be swapped.
    // It's safer to check the actual dimension names/order from the variable object.
    val tempVarExample = ncFile.findVariable("ta") // Check one variable
    var stationDimIdx = 0
    var timeDimIdx = 1
    if (tempVarExample != null && tempVarExample.rank == 2) {
        val dims = tempVarExample.dimensions
        // Crude check, assumes 'time' dim name contains 'time' and 'station' dim name contains 'station'
        if (dims[0].shortName.contains("time", ignoreCase = true) && dims[1].shortName.contains(
                "station",
                ignoreCase = true
            )
        ) {
            timeDimIdx = 0
            stationDimIdx = 1
            Log.i("KnmiParser", "Detected dimension order: (time, station)")
        } else if (dims[0].shortName.contains("station", ignoreCase = true) && dims[1].shortName.contains(
                "time",
                ignoreCase = true
            )
        ) {
            stationDimIdx = 0
            timeDimIdx = 1
            Log.i("KnmiParser", "Detected dimension order: (station, time)")
        } else {
            Log.w(
                "KnmiParser",
                "Could not reliably determine dimension order for 'ta'. Assuming (station, time). Dims: ${dims.map { it.shortName }}"
            )
            // Default to (station, time) if unsure, but this should be verified
        }
    } else {
        Log.w("KnmiParser", "'ta' variable not found or not rank 2. Assuming (station, time) dimension order.")
    }


    // --- 3. Loop through each time point ---
    for (timeIndex in 0 until numTimePoints) {
        val timeValue = timeValuesArray.getDouble(timeIndex)
        val actualDate = calDateUnit.makeCalendarDate(timeValue).toDate()

        // Read data for the closest station AT THIS SPECIFIC timeIndex
        val tempValue = ncFile.readStationTimeData("ta", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val precipitationR1H =
            ncFile.readStationTimeData("R1H", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val windSpeed = ncFile.readStationTimeData("ff", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val windDirection =
            ncFile.readStationTimeData("dd", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)?.toInt()
        val windGust = ncFile.readStationTimeData("fx", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val humidity = ncFile.readStationTimeData("rh", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val dewPointTemp = ncFile.readStationTimeData("td", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val seaLevelPressure =
            ncFile.readStationTimeData("pp", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val totalCloudCoverOcta =
            ncFile.readStationTimeData("n", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val sunshineDurationMin =
            ncFile.readStationTimeData("ss", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)
        val presentWeatherCodeVal =
            ncFile.readStationTimeData("pwc", closestStationIndex, timeIndex, stationDimIdx, timeDimIdx)?.toInt()

        val temperature = tempValue?.let { Temperature(temperature = it) }
        val precipitation = precipitationR1H?.let { Precipitation(total = it) }
        val wind = if (windSpeed != null && windDirection != null) {
            Wind(speed = windSpeed, degree = windDirection.toDouble(), gusts = windGust)
        } else null
        val cloudCoverPercent = totalCloudCoverOcta?.let { (it * 12.5).toInt().coerceIn(0, 100) }
        val sunshineDurationHours = sunshineDurationMin?.let { it / 60.0 }
//        val weatherCode = when (presentWeatherCodeVal) {
//            // Add actual KNMI PWC code mappings here
//            null -> WeatherCode.UNKNOWN
//            else -> {
//                Log.w("KnmiParser", "Unknown PWC code from KNMI: $presentWeatherCodeVal for time $actualDate")
//                WeatherCode.UNKNOWN
//            }
//        }

        val hourly = HourlyWrapper(
            date = actualDate,
            isDaylight = null,
            weatherText = null,
            //weatherCode = weatherCode,
            temperature = temperature,
            precipitation = precipitation,
            precipitationProbability = null,
            wind = wind,
            uV = null,
            relativeHumidity = humidity,
            dewPoint = dewPointTemp,
            pressure = seaLevelPressure,
            cloudCover = cloudCoverPercent,
            visibility = null,
            sunshineDuration = sunshineDurationHours
        )
        hourlyWrappers.add(hourly)
    }

    Log.i("KnmiParser", "Parsed ${hourlyWrappers.size} hourly data points for station $closestStationIndex.")
    return hourlyWrappers
}
