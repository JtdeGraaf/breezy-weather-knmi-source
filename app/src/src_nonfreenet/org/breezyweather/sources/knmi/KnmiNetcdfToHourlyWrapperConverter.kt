package org.breezyweather.sources.knmi

import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Temperature
import breezyweather.domain.weather.wrappers.HourlyWrapper
import org.breezyweather.sources.knmi.KnmiService.Companion.KNMI_API_KEY
import org.breezyweather.sources.knmi.datasets.KnmiDatasets
import org.breezyweather.sources.knmi.datasets.harmoniecy43meteorologicalaviationforecastparameters.KnmiHarmonieCy43ForecastFiles
import org.breezyweather.sources.knmi.datasets.harmoniecy43meteorologicalaviationforecastparameters.KnmiHarmonieCy43ForecastVariables
import org.breezyweather.sources.knmi.json.KnmiDataset
import ucar.ma2.Array
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import ucar.nc2.Variable
import ucar.nc2.time.CalendarDate
import ucar.nc2.time.CalendarDateUnit
import java.net.URL
import java.util.Date
import kotlin.math.abs

/**
 * Finds the indices of the grid cell closest to the target latitude and longitude.
 *
 * @param netcdfFile The NetCDF file.
 * @param targetLat Target latitude.
 * @param targetLon Target longitude.
 * @return Pair of (latitudeIndex, longitudeIndex) or null if not found.
 */
fun findClosestGridPoint(
    netcdfFile: NetcdfFile,
    targetLat: Double,
    targetLon: Double,
): Pair<Int, Int>? {
    val latitudeVariable = netcdfFile.findVariable(KnmiHarmonieCy43ForecastVariables.LATITUDE.variableName) ?: return null
    val longitudeVariable = netcdfFile.findVariable(KnmiHarmonieCy43ForecastVariables.LONGITUDE.variableName) ?: return null

    val latitudes: Array = latitudeVariable.read()
    val longitudes: Array = longitudeVariable.read()

    //TODO: Could potentially use binary search here, but I am not sure if the data is sorted

    // Find the closest latitude point in the grid
    var closestLatitudeIndex : Int? = null
    var minimumLatitudeDifference = Double.MAX_VALUE
    for (i in 0 until latitudes.size.toInt()) {
        val diff = abs(latitudes.getDouble(i) - targetLat)
        if (diff < minimumLatitudeDifference) {
            minimumLatitudeDifference = diff
            closestLatitudeIndex = i
        }
    }

    // Find the closest longitude point in the grid
    var closestLongitudeIndex : Int? = null
    var minimumLongitudeDifference = Double.MAX_VALUE
    for (i in 0 until longitudes.size.toInt()) {
        val diff = abs(longitudes.getDouble(i) - targetLon)
        if (diff < minimumLongitudeDifference) {
            minimumLongitudeDifference = diff
            closestLongitudeIndex = i
        }
    }

    if (closestLatitudeIndex == null || closestLongitudeIndex == null) {
        return null
    }
    return Pair(closestLatitudeIndex, closestLongitudeIndex)
}


/**
 * Converts a KNMI dataset to a list of HourlyWrappers.
 */
fun convertKnmiDatasetToHourlyWrapper(
    dataset: KnmiDataset,
    targetLocation: Location,
    knmiApi: KnmiApi
): List<HourlyWrapper> {
    val temperatureForecastFile = dataset.files.first { it.filename.contains(KnmiHarmonieCy43ForecastFiles.AIR_TEMPERATURE_HAGL.filename) }

    val temperatureFileDownloadUrl = knmiApi.getTempDownloadUrlForFile(
        KNMI_API_KEY,
        KnmiDatasets.HARMONIE_CY43_METEOROLOGICAL_AVIATION_FORECAST_PARAMETERS.datasetName,
        KnmiDatasets.HARMONIE_CY43_METEOROLOGICAL_AVIATION_FORECAST_PARAMETERS.version ,
        temperatureForecastFile.filename
    ).blockingFirst()
    val fileBytes = URL(temperatureFileDownloadUrl.temporaryDownloadUrl).readBytes()
    val temperatureNetcdfFile = NetcdfFiles.openInMemory(temperatureForecastFile.filename, fileBytes)

    val closestGridPoint: Pair<Int, Int> = findClosestGridPoint(temperatureNetcdfFile, targetLocation.latitude, targetLocation.longitude) ?: return emptyList()
    val timeVariable: Variable = temperatureNetcdfFile.findVariable(KnmiHarmonieCy43ForecastVariables.TIME.variableName) ?: return emptyList()

    val timeVariableData: Array = timeVariable.read()
    val timePointCount = timeVariableData.size.toInt()
    if (timePointCount == 0) {
        return emptyList()
    }
    val timeUnits = timeVariable.findAttribute("units")?.stringValue ?: return emptyList()
    val calendarDateUnit = try {
        CalendarDateUnit.of(null, timeUnits)
    } catch (e: Exception) {
        return emptyList()
    }

    val hourlyWrappers = mutableListOf<HourlyWrapper>()

    for (timeIndex in 0 until timePointCount) {
        val timeValue = timeVariableData.getDouble(timeIndex)
        val measurementCalendarDate: CalendarDate = calendarDateUnit.makeCalendarDate(timeValue)
        val measurementDate: Date = measurementCalendarDate.toDate()

        val temperatureNetCdfValue = temperatureNetcdfFile.readGridDataAtPoint(
            varName = KnmiHarmonieCy43ForecastVariables.AIR_TEMPERATURE_HAGL.variableName,
            timeIndex = timeIndex,
            latIndex = closestGridPoint.first,
            lonIndex = closestGridPoint.second,
        )

        val temperatureData = temperatureNetCdfValue?.let { Temperature(temperature = it) }

        // TODO: implement more features
        hourlyWrappers.add(
            HourlyWrapper(
                date = measurementDate,
                temperature = temperatureData,
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
    return hourlyWrappers
}

fun debugPrintNetcdfVariables(ncFile: NetcdfFile) {
    println("================ VARIABLES BELOW ================")
    println(ncFile.variables)
    println("================ GLOBAL ATTRIBUTES BELOW ================")
    print(ncFile.globalAttributes)
    println("================ END ================")
}


