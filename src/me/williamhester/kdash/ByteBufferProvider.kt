package me.williamhester.kdash

import java.nio.ByteBuffer


interface ByteBufferProvider : AutoCloseable {
  fun get(offset: Int, len: Int): ByteBuffer
}
