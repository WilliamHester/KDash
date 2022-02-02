package me.williamhester.kdash

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path

class FileIRacingByteBufferProvider(path: String) : ByteBufferProvider {
  private val fileChannel = FileChannel.open(Path.of(path))

  override fun get(offset: Int, len: Int): ByteBuffer {
    val buffer = ByteBuffer.allocate(len).order(ByteOrder.LITTLE_ENDIAN)
    fileChannel.position(offset.toLong())
    fileChannel.read(buffer)
    return buffer.flip()
  }

  override fun close() {
    fileChannel.close()
  }
}