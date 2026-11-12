package com.mindlink.journal.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

/**
 * Room TypeConverters for complex types like lists of strings and vector FloatArrays.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String {
        if (array == null || array.isEmpty()) return ""
        val byteBuffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in array) {
            byteBuffer.putFloat(f)
        }
        return android.util.Base64.encodeToString(byteBuffer.array(), android.util.Base64.NO_WRAP)
    }

    @TypeConverter
    fun toFloatArray(encoded: String?): FloatArray {
        if (encoded.isNullOrBlank()) return FloatArray(0)
        return try {
            val bytes = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val floats = FloatArray(bytes.size / 4)
            for (i in floats.indices) {
                floats[i] = buffer.getFloat()
            }
            floats
        } catch (e: Exception) {
            FloatArray(0)
        }
    }
}
