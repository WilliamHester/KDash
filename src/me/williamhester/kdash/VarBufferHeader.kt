package me.williamhester.kdash

import java.nio.ByteBuffer

data class VarBufferHeader(
  val tickCount: Int,
  val offset: Long, // Offset from ..?
  val unused: Long,
) {
  constructor(byteBuffer: ByteBuffer) : this(
    byteBuffer.int,
    byteBuffer.int.toLong(),
    byteBuffer.long, // Unused, but 8 bytes exist after the second int.
  )
}