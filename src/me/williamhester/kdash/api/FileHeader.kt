package me.williamhester.kdash.api

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.stream.IntStream

/**
 * The file header consists of two parts: the main header and the "disk subheader."
 *
 * The subheader is simply appended immediately after the main header, so we can simply consider them together to be the
 * header. For live data, I expect the disk subheader to simply be zeroed out.
 *
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
 *   24   int numVars;           // length of array pointed to by varHeaderOffset
 *   28   int varHeaderOffset;   // offset to irsdk_varHeader[numVars] array, Describes the variables received in varBuf
 *
 *   32   int numBuf;            // <= IRSDK_MAX_BUFS (3 for now)
 *   36   int bufLen;            // length in bytes for one line
 *   40   int pad1[2];           // (16 byte align)
 *   48   irsdk_varBuf varBuf[IRSDK_MAX_BUFS]; // buffers of data being written to
 *   112  total (irsdk_varBuf = 16)
 * }
 *
 * struct irsdk_diskSubHeader {
 *   112  time_t sessionStartDate;
 *   120  double sessionStartTime;
 *   128  double sessionEndTime;
 *   136  int sessionLapCount;
 *   140  int sessionRecordCount;
 *   144
 * };
 */
class FileHeader(
  private val buffer: ByteBuffer,
) {
  val version = buffer.getInt(0)
  val status = buffer.getInt(4)
  val tickRate = buffer.getInt(8)
  val sessionInfoUpdate: Int
    get() = buffer.getInt(12)
  val sessionInfoLen: Int
    get() = buffer.getInt(16)
  val sessionInfoOffset: Int
    get() = buffer.getInt(20)
  val numVars = buffer.getInt(24)
  val varHeaderOffset = buffer.getInt(28)
  val numBuf = buffer.getInt(32)
  val bufLen = buffer.getInt(36)

  val sessionLapCount = buffer.getInt(136)
  val sessionRecordCount = buffer.getInt(140)

  internal val varBufHeaders: List<VarBufferHeader> =
    IntStream.range(0, numBuf).mapToObj {
      VarBufferHeader(
        buffer.slice(48 + VarBufferHeader.SIZE * it, VarBufferHeader.SIZE).order(ByteOrder.LITTLE_ENDIAN)
      )
    }.toList()
}