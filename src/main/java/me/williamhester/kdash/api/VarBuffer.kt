package me.williamhester.kdash.api

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A VarBuffer represents a chunk of data from a single point in time.
 *
 * Methods exist to get different types of data for the provided variable name. There's also a generic [get] function
 * that allows for getting any type of data.
 */
class VarBuffer(
  private val headers: Map<String, VarHeader>,
  byteBuffer: ByteBuffer,
) {
  private val byteBuffer = byteBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)

  operator fun get(key: String): Any {
    val header = headers[key] ?: throw Exception("Key '$key' not found")

    return when (header.type) {
      VarHeader.VarType.CHAR -> getByte(key)
      VarHeader.VarType.BOOLEAN -> getBoolean(key)
      VarHeader.VarType.INT -> getInt(key)
      VarHeader.VarType.BITFIELD -> getInt(key) // TODO: Return something better?
      VarHeader.VarType.FLOAT -> getFloat(key)
      VarHeader.VarType.DOUBLE -> getDouble(key)
      VarHeader.VarType.ETCOUNT -> TODO()
    }
  }

  fun getByte(key: String, default: Byte? = null) = getInternal(key, default, ByteBuffer::get)
  fun getInt(key: String, default: Int? = null) = getInternal(key, default, ByteBuffer::getInt)
  fun getBoolean(key: String, default: Boolean? = null): Boolean = getInternal(key, default) {
    // TODO figure out why this won't work with get()
    getByte(key).toInt() != 0
  }
  fun getDouble(key: String, default: Double? = null) = getInternal(key, default, ByteBuffer::getDouble)
  fun getFloat(key: String, default: Float? = null) = getInternal(key, default, ByteBuffer::getFloat)

  private fun <T> getInternal(varName: String, default: T?, getter: ByteBuffer.(Int) -> T): T {
    val header = headers[varName] ?: return default ?: throw Exception("$varName not found")
    return byteBuffer.getter(header.offset)
  }
}