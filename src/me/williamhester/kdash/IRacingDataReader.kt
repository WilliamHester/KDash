package me.williamhester.kdash

import java.lang.Exception
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

class IRacingDataReader(
  private val byteBufferProvider: ByteBufferProvider,
) {
  private val varBuffer: AtomicReference<ByteBuffer> = AtomicReference(ByteBuffer.allocate(0))

  /** Read an int from the buffer with the key [varName]. */
  fun readInt(varName: String) : Int = withMostRecentBufferGet(varName, ByteBuffer::getInt)

  /** Read a double from the buffer with the key [varName]. */
  fun readDouble(varName: String) : Double = withMostRecentBufferGet(varName, ByteBuffer::getDouble)

  private fun <T> withMostRecentBufferGet(varName: String, getter: ByteBuffer.(Int) -> T) : T {
    if (!byteBufferProvider.headers.containsKey(varName)) throw Exception("$varName not found")
    storeMostRecentBuffer()

    return varBuffer.get().getter(byteBufferProvider.headers[varName]!!.offset)
  }

  private fun storeMostRecentBuffer() {
    // TODO: Cache this instead of getting the buffer each time.
    varBuffer.set(byteBufferProvider.getLatest())
  }
}