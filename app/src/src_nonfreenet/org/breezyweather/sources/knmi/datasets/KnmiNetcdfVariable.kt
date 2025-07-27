package org.breezyweather.sources.knmi.datasets

import ucar.ma2.DataType

interface KnmiNetcdfVariable {
    val variableName: String
    val description: String
    val unitDescription: String
    val dataType: DataType
}
