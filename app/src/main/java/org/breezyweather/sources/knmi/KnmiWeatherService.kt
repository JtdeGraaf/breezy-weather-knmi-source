package org.breezyweather.sources.knmi

import android.content.Context
import breezyweather.domain.location.model.Location
import breezyweather.domain.source.SourceContinent
import breezyweather.domain.source.SourceFeature
import breezyweather.domain.weather.wrappers.WeatherWrapper
import dagger.hilt.android.qualifiers.ApplicationContext

import io.reactivex.rxjava3.core.Observable

import org.breezyweather.common.source.HttpSource
import org.breezyweather.common.source.WeatherSource
import org.breezyweather.sources.brightsky.BrightSkyApi
import retrofit2.Retrofit
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Named

class KnmiWeatherService @Inject constructor(
    @ApplicationContext context: Context,
    @Named("JsonClient") client: Retrofit.Builder,
) : HttpSource(), WeatherSource{

    private val mApi by lazy {
        client
            .baseUrl(KNMI_BASE_URL)
            .build()
            .create(KnmiApi::class.java)
    }

    // Source overrides
    override val id = "knmi"
    override val name = "KNMI"

    // HttpSource overrides
    override val continent = SourceContinent.EUROPE
    override val privacyPolicyUrl = "REPLACE" // TODO

    // WeatherSource overrides
    override val supportedFeatures = mapOf(
        SourceFeature.FORECAST to name
        // TODO: Figure out what to support
    )

    // TODO: Add Dutch locations
    override val testingLocations: List<Location> = emptyList()

    override fun requestWeather(
        context: Context,
        location: Location,
        requestedFeatures: List<SourceFeature>,
    ): Observable<WeatherWrapper> {
        /** Step by step plan:
         * 1. Retrieve latest dataset file name
         * 2. Retrieve file download url
         * 3. Download file and store in memory
         * 4. Parse file with ucar library
         * 5. Profit?
         */
        val ncFile: NetcdfFile = NetcdfFiles.open("")

        //mApi.getLocations(KNMI_API_KEY, LocalDateTime.now()) // TODO replace with valid datetime for android 5


        TODO("Not yet implemented")
    }

    companion object {
        private const val KNMI_BASE_URL = "https://api.dataplatform.knmi.nl"
        // Api key is publicly distributed by KNMI, so it is not secret. Expires on the first of July 2026
        private const val KNMI_API_KEY = "eyJvcmciOiI1ZTU1NGUxOTI3NGE5NjAwMDEyYTNlYjEiLCJpZCI6ImVlNDFjMWI0MjlkODQ2MThiNWI4ZDViZDAyMTM2YTM3IiwiaCI6Im11cm11cjEyOCJ9"
    }

}
