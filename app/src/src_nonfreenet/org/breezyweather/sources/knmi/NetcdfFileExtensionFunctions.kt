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
                    continue
                }
                // This dimension is not one we are iterating or fixing,
                // if its length is > 1, this read will be problematic
                // For now, assume if not specified, it's length 1 or should be handled
                if (dim.length > 1) {
                    origin[idx] = 0 // Default to 0 if not specified and length > 1 (could be risky)
                } else {
                    origin[idx] = 0 // If length is 1, index 0 is fine
                }

            }
        }
    }

    // Validate that required dimensions were found
    if (timeDimIndex == -1) {
        return null
    }
    if (latDimIndex == -1 && rank > 2) { // Allow for 1D or 2D time series not on a spatial grid if needed, but temp is spatial
        return null
    }
    if (lonDimIndex == -1 && rank > 2) {
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
