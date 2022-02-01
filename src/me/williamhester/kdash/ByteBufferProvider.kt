package me.williamhester.kdash

import java.nio.ByteBuffer

interface ByteBufferProvider : AutoCloseable {
  val headers: Map<String, VarHeader>

  fun getLatest(): ByteBuffer
}