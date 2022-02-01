package me.williamhester.kdash

import java.nio.ByteBuffer

/**
 * struct irsdk_header
 * {
 *   0    int ver;               // this api header version, see IRSDK_VER
 *   4    int status;            // bitfield using irsdk_StatusField
 *   8    int tickRate;          // ticks per second (60 or 360 etc)
 *
 *        // session information, updated periodicaly
 *   12   int sessionInfoUpdate; // Incremented when session info changes
 *   16   int sessionInfoLen;    // Length in bytes of session info string
 *   20   int sessionInfoOffset; // Session info, encoded in YAML format
 *
 *        // State data, output at tickRate
 *   24   int numVars;           // length of arra pointed to by varHeaderOffset
 *   28   int varHeaderOffset;   // offset to irsdk_varHeader[numVars] array, Describes the variables received in varBuf
 *
 *   32   int numBuf;            // <= IRSDK_MAX_BUFS (3 for now)
 *   36   int bufLen;            // length in bytes for one line
 *   40   int pad1[2];           // (16 byte align)
 *   48   irsdk_varBuf varBuf[IRSDK_MAX_BUFS]; // buffers of data being written to
 *   104  total (irsdk_varBuf = 16)
 * };
 */
data class FileHeader(
  val version: Int,
  val status: Int,
  val tickRate: Int,
  val sessionInfoUpdate: Int,
  val sessionInfoLen: Int,
  val sessionInfoOffset: Int,
  val numVars: Int,
  val varHeaderOffset: Long,
  val numBuf: Int,
  val bufLen: Int,
) {
  constructor(byteBuffer: ByteBuffer) : this(
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int,
    byteBuffer.int.toLong(),
    byteBuffer.int,
    byteBuffer.int,
  )
}