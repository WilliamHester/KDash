package me.williamhester.kdash.api

import java.nio.ByteBuffer

/**
 * A header for a single variable.
 *
 * struct irsdk_varHeader {
 *   0    int type;                              // irsdk_VarType
 *   4    int offset;                            // offset from start of buffer row
 *   8    int count;                             // number of entrys (array)
 *                                               // so length in bytes would be irsdk_VarTypeBytes[type] * count
 *   12   bool countAsTime;
 *   13   char pad[3];                           // (16 byte align)
 *
 *   16   char name[IRSDK_MAX_STRING];
 *   48   char desc[IRSDK_MAX_DESC];
 *   112  char unit[IRSDK_MAX_STRING];           // something like "kg/m^2"
 *   144 -- total length
 * };
 */
data class VarHeader(
  val type: VarType,
  val offset: Int,
  val count: Int,
  val countAsTime: Boolean,
  val name: String,
  val description: String,
  val unit: String,
) {
  constructor(byteBuffer: ByteBuffer) : this(
    VarType.valueMap[byteBuffer.int]!!,
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int != 0,
    byteBuffer.nextString(IRSDK_MAX_STRING),
    byteBuffer.nextString(IRSDK_MAX_DESC),
    byteBuffer.nextString(IRSDK_MAX_STRING),
  )

  enum class VarType(private val int: Int) {
    CHAR(0),
    BOOLEAN(1),
    INT(2),
    BITFIELD(3),
    FLOAT(4),
    DOUBLE(5),
    ETCOUNT(6),
    ;

    companion object {
      internal val valueMap = values().associateBy { it.int }.toMap()
    }
  }

  companion object {
    const val SIZE = 144

    fun fromByteBufferToHeadersMap(varHeaderBuffer: ByteBuffer, numVars: Int): Map<String, VarHeader> {
      val headers = mutableMapOf<String, VarHeader>()
      for (i in 0 until numVars) {
        val header = VarHeader(varHeaderBuffer)
        headers[header.name] = header
      }
      return headers.toMap()
    }
  }
}