package me.williamhester.kdash.api

import java.nio.ByteBuffer

/**
 * A header describing one of the live [VarBuffer]s.
 *
 * Data is retrieved _dynamically_ from the provided ByteBuffer to keep up with the live data.
 */
internal class VarBufferHeader(
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
