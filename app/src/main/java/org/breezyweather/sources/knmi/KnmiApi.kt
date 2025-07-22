package org.breezyweather.sources.knmi

import io.reactivex.rxjava3.core.Observable
import org.breezyweather.sources.knmi.json.KnmiLocation
import org.breezyweather.sources.pirateweather.json.PirateWeatherForecastResult
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDateTime

interface KnmiApi {

    @GET("/edr/v1/collections/10-minute-in-situ-meteorological-observations/locations")
    fun getLocations(
        @Header("Authorization") apiKey: String,
        @Query("datetime") datetime: LocalDateTime,
    ): Observable<List<KnmiLocation>>
}
