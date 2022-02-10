package me.williamhester.kdash.api

import java.nio.ByteBuffer

/**
 * A provider of [ByteBuffer]s from an underlying data source.
 *
 * The ByteBuffers returned from this *must* be in [ByteBuffer.LITTLE_ENDIAN] order, as iRacing's data is provided in
 * that order.
 */
internal interface ByteBufferProvider : AutoCloseable {
  /** Get a ByteBuffer starting at [offset] with the given [length]. */
  fun get(offset: Int, length: Int): ByteBuffer
}
