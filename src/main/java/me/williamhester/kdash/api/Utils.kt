package me.williamhester.kdash.api

import java.nio.ByteBuffer

/**
 * Read the next string from the byte buffer of length [length], then strip everything once a null byte is found.
 */
internal fun ByteBuffer.nextString(length: Int) : String {
  val buffer = ByteArray(length)
  get(buffer, 0, length)
  var last0 = 0
  for (i in 0 until length) {
    if (buffer[i].toInt() == 0) break
    last0++
  }
  return String(buffer, 0, last0)
}
