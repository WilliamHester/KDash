package me.williamhester.kdash

import java.nio.ByteBuffer

/**
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
  val type: Int,
  val offset: Int,
  val count: Int,
  val countAsTime: Boolean,
  val name: String,
  val description: String,
  val unit: String,
) {
  constructor(byteBuffer: ByteBuffer) : this(
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int != 0,
    byteBuffer.nextString(IRSDK_MAX_STRING),
    byteBuffer.nextString(IRSDK_MAX_DESC),
    byteBuffer.nextString(IRSDK_MAX_STRING),
  )
}