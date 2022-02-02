package me.williamhester.kdash

import java.nio.ByteBuffer

class VarBufferHeader(
  private val innerBuffer: ByteBuffer,
) {
  val tickCount: Int
    get() = innerBuffer.getInt(0)
  val offset: Int
    get() = innerBuffer.getInt(4)

  companion object {
    const val SIZE = 16
  }
}
