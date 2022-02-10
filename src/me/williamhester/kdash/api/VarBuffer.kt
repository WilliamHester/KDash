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

  fun getByte(key: String) = getInternal(key, ByteBuffer::get)

  fun getInt(key: String) = getInternal(key, ByteBuffer::getInt)

  fun getBoolean(key: String) = getInternal(key, ByteBuffer::get).toInt() != 0

  fun getDouble(key: String) = getInternal(key, ByteBuffer::getDouble)

  fun getFloat(key: String) = getInternal(key, ByteBuffer::getFloat)

  private fun <T> getInternal(varName: String, getter: ByteBuffer.(Int) -> T): T {
    val header = headers[varName] ?: throw Exception("$varName not found")
    return byteBuffer.getter(header.offset)
  }
}