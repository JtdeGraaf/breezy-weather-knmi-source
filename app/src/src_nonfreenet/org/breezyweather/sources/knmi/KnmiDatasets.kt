package org.breezyweather.sources.knmi

enum class KnmiDatasets(val datasetName: String, val version: String, val fileExtension: String) {
    CURRENT_WEATHER_AT_KNMI_STATIONS("Actuele10mindataKNMIstations", "2", ".nc"),
    GRIDDED_DAILY_MEAN_TEMPERATURE("Tg1", "5", ".nc"), //https://dataplatform.knmi.nl/dataset/tg1-5
    GRIDDED_DAILY_MAXIMUM_TEMPERATURE("Tx1", "2", ".nc"), //https://dataplatform.knmi.nl/dataset/tx1-2
    GRIDDED_DAILY_MINIMUM_TEMPERATURE("Tn1", "2", ".nc"), //https://dataplatform.knmi.nl/dataset/tn1-2
    GRIDDED_PRECIPATION_FIVE_MINUTE_RADAR_UP_TO_TWO_HOURS_AHED("radar_forecast", "2.0", ".h5"),
    HARMONIE_CY43_METEOROLOGICAL_AVIATION_FORECAST_PARAMETERS("uwcw_extra_lv_ha43_nl_2km", "1.0", ".nc")




    // https://dataplatform.knmi.nl/dataset/outlook-weather-forecast-1-0

}
//https://dataplatform.knmi.nl/dataset/uwcw-extra-lv-ha43-nl-2km-1-0
//https://dataplatform.knmi.nl/dataset/preview/uwcw-extra-lv-ha43-nl-2km-1-0
