package me.williamhester.kdash.api

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path

/** [ByteBufferProvider] that serves ByteBuffers from a local file. */
internal class FileIRacingByteBufferProvider(path: Path) : ByteBufferProvider {
  private val fileChannel = FileChannel.open(path)

  override fun get(offset: Int, length: Int): ByteBuffer {
    val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
    fileChannel.position(offset.toLong())
    fileChannel.read(buffer)
    return buffer.flip()
  }

  override fun getByteArray(offset: Int, length: Int): ByteArray {
    return get(offset, length).array()
  }

  override fun close() {
    fileChannel.close()
  }
}