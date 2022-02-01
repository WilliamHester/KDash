package me.williamhester.kdash

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FileIRacingByteBufferProvider(path: String) : ByteBufferProvider {
  private val file = RandomAccessFile(path, "r")
  private val fileHeader: FileHeader
  override val headers: Map<String, VarHeader>

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
  }

  override fun getLatest(): ByteBuffer {
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
    return ByteBuffer.wrap(newByteArray).order(ByteOrder.LITTLE_ENDIAN)
  }

  override fun close() {
    file.close()
  }
}