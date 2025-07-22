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
         * 1. Retrieve all available locations
         * 2. Get the location closest to the coordinates
         * 3. Get the 10 minute interval forecast for that location
         */

        mApi.getLocations(KNMI_API_KEY, LocalDateTime.now()) // TODO replace with valid datetime for android 5


        TODO("Not yet implemented")
    }

    companion object {
        private const val KNMI_BASE_URL = "https://api.dataplatform.knmi.nl"
        private const val KNMI_API_KEY = ""
    }

}
