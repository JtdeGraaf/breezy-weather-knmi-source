package org.breezyweather.sources.knmi

import io.reactivex.rxjava3.core.Observable
import org.breezyweather.sources.knmi.json.KnmiDataset
import org.breezyweather.sources.knmi.json.KnmiTempFileDownloadUrl

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDateTime
import java.time.ZonedDateTime

// https://tyk-cdn.dataplatform.knmi.nl/open-data/index.html
interface KnmiApi {

    @GET("/open-data/v1/datasets/Actuele10mindataKNMIstations/versions/2/files")
    fun getTenMinuteIntervalDatasets(
        @Header("Authorization") apiKey: String,
        @Query("maxKeys") maxKeys: Long?, // Maximum of 1000
        @Query("sorting") sorting: String?, // 'asc', 'desc'
        @Query("orderBy") orderBy: String?, // 'filename', 'lastModified', 'created'
        @Query("begin") begin: ZonedDateTime?,
        @Query("end") end: ZonedDateTime?,
    ): Observable<KnmiDataset>

    @GET("/open-data/v1/datasets/Actuele10mindataKNMIstations/versions/2/files/{filename}/url")
    fun getTempDownloadUrlForFile(
        @Header("Authorization") apiKey: String,
        @Path("filename") filename: String,
    ): Observable<KnmiTempFileDownloadUrl>
}
