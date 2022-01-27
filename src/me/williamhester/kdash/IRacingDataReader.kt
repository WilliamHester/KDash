package me.williamhester.kdash

import java.io.RandomAccessFile
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference

class IRacingDataReader(path: String) {
  private val file = RandomAccessFile(path, "r")
  private val headers: Map<String, VarHeader>
  private val fileHeader: FileHeader
  private val varBuffer: AtomicReference<ByteBuffer> = AtomicReference(ByteBuffer.allocate(0))

  init {
    val headerBuffer = ByteBuffer.allocate(144).order(ByteOrder.LITTLE_ENDIAN)
    file.read(headerBuffer.array())
    fileHeader = FileHeader(headerBuffer)

    file.seek(fileHeader.varHeaderOffset)

    val headerByteBuffer = ByteBuffer.allocate(144).order(ByteOrder.LITTLE_ENDIAN)
    val headers = mutableMapOf<String, VarHeader>()
    for (i in 0 until fileHeader.numVars) {
      headerByteBuffer.clear()

      file.read(headerByteBuffer.array())

      val header = VarHeader(headerByteBuffer)
      headers[header.name] = header
    }
    this.headers = headers.toMap()
    headers.keys.forEach { println(it) }
  }

  /** Read an int from the buffer with the key [varName]. */
  fun readInt(varName: String) : Int = withMostRecentBufferGet(varName, ByteBuffer::getInt)

  /** Read a double from the buffer with the key [varName]. */
  fun readDouble(varName: String) : Double = withMostRecentBufferGet(varName, ByteBuffer::getDouble)

  private fun <T> withMostRecentBufferGet(varName: String, getter: ByteBuffer.(Int) -> T) : T {
    if (!headers.containsKey(varName)) throw Exception("$varName not found")
    storeMostRecentBuffer()

    return varBuffer.get().getter(headers[varName]!!.offset)
  }

  private fun storeMostRecentBuffer() {
    file.seek(48) // End of the file header
    val varBufferHeaderBytes = ByteArray(16)
    val varBufferHeaderByteBuffer = ByteBuffer.wrap(varBufferHeaderBytes).order(ByteOrder.LITTLE_ENDIAN)
    val buffers = mutableListOf<VarBufferHeader>()
    for (i in 0 until fileHeader.numBuf) {
      file.read(varBufferHeaderBytes)
      buffers.add(VarBufferHeader(varBufferHeaderByteBuffer))
    }
    buffers.sortByDescending { it.tickCount }

    val mostRecent = buffers[0]
    val newByteArray = ByteArray(fileHeader.bufLen)
    file.seek(mostRecent.offset)
    file.read(newByteArray)
    val newBuffer = ByteBuffer.wrap(newByteArray).order(ByteOrder.LITTLE_ENDIAN)
    varBuffer.set(newBuffer)
  }

  /**
   * struct irsdk_varHeader {
   *   0    int type;                              // irsdk_VarType
   *   16   int offset;                            // offset from start of buffer row
   *   32   int count;                             // number of entrys (array)
   *                                               // so length in bytes would be irsdk_VarTypeBytes[type] * count
   *   48   bool countAsTime;
   *   56   char pad[3];                           // (16 byte align)
   *
   *   64   char name[IRSDK_MAX_STRING];
   *   96   char desc[IRSDK_MAX_DESC];
   *   160  char unit[IRSDK_MAX_STRING];           // something like "kg/m^2"
   *   256 -- total length
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
   *   48  irsdk_varBuf varBuf[IRSDK_MAX_BUFS]; // buffers of data being written to
   *   896 total (irsdk_varBuf = 128)
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

  data class VarBufferHeader(
    val tickCount: Int,
    val offset: Long, // Offset from ..?
  ) {
    constructor(byteBuffer: ByteBuffer) : this(
      byteBuffer.int,
      byteBuffer.int.toLong(),
    )
  }

  companion object {
    private const val IRSDK_MAX_STRING = 32
    private const val IRSDK_MAX_DESC = 64
    private const val IRSDK_MAX_BUFS = 4
  }
}