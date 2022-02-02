package me.williamhester.kdash

import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference

class IRacingDataReader(
  private val byteBufferProvider: ByteBufferProvider,
) {
  private val fileHeader = FileHeader(byteBufferProvider.get(0, 144))
  private val headers: Map<String, VarHeader> =
    VarHeader.fromByteBufferToHeadersMap(
      byteBufferProvider.get(fileHeader.varHeaderOffset, VarHeader.SIZE * fileHeader.numVars),
      fileHeader.numVars
    )
  private val varBuffer: AtomicReference<ByteBuffer> = AtomicReference(ByteBuffer.allocate(0))
  private var latestTick: Int = -1

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
    val latestHeader = fileHeader.varBufHeaders.maxByOrNull { it.tickCount }!!
    if (latestHeader.tickCount == latestTick) return

    latestTick = latestHeader.tickCount
    varBuffer.set(
      byteBufferProvider.get(latestHeader.offset, fileHeader.bufLen).duplicate().order(ByteOrder.LITTLE_ENDIAN)
    )
  }
}
