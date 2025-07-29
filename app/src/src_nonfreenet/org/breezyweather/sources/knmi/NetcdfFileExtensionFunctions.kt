package org.breezyweather.sources.knmi

import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.nc2.NetcdfFile

/**
 * Reads gridded data for a specific variable, time index, and grid point.
 * Assumes a 4D variable like var(time, height_level, lat, lon) or 3D if no height_level.
 */
fun NetcdfFile.readGridDataAtPoint(
    varName: String,
    timeIndex: Int,
    latIndex: Int,
    lonIndex: Int,
): Double? {
    val variable = this.findVariable(varName) ?: return null

    val rank = variable.rank
    val origin = IntArray(rank)
    val shape = IntArray(rank) { 1 } // Read a single point

    var timeDimIndex: Int? = null
    var latDimIndex: Int? = null
    var lonDimIndex: Int? = null

    for ((idx, dim) in variable.dimensions.withIndex()) {
        // For now I am just assuming the variables are named universally for KNMI datasets, else add them to the parameters of the function
        when (dim.shortName) {
            "time" -> {
                origin[idx] = timeIndex
                timeDimIndex = idx
            }
            "latitude" -> {
                origin[idx] = latIndex
                latDimIndex = idx
            }
            "longitude" -> {
                origin[idx] = lonIndex
                lonDimIndex = idx
            }
            else -> return null
        }
    }

    // Validate that required dimensions were found
    if (timeDimIndex == null || latDimIndex == null || lonDimIndex == null || rank > 2) {
        return null
    }


    try {
        val dataArray: Array = variable.read(origin, shape)
        if (dataArray.size != 1L) {
            return null
        }

        val value = when (variable.dataType) {
            DataType.FLOAT -> dataArray.getFloat(0).toDouble()
            DataType.DOUBLE -> dataArray.getDouble(0)
            DataType.INT -> dataArray.getInt(0).toDouble()
            DataType.SHORT -> dataArray.getShort(0).toDouble()
            else -> {
                return null
            }
        }

        val fillValueAttr = variable.findAttributeIgnoreCase("_FillValue")
        if (fillValueAttr != null && !fillValueAttr.isString) {
            val fillValue = fillValueAttr.numericValue
            if (value == fillValue) {
                return null
            }
        } else if (value.isNaN() && variable.dataType == DataType.FLOAT) { // Check for default NaN float
            return null
        }


        return value
    } catch (e: Exception) {
        return null
    }
}
