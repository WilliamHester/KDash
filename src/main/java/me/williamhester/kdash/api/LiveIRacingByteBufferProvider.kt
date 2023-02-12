package me.williamhester.kdash.api

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.win32.W32APIOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A provider of data from Windows's "memory-mapped file" API. */
class LiveIRacingByteBufferProvider : ByteBufferProvider {
  private val handle = kernel32.OpenFileMapping(WinBase.FILE_MAP_READ, false, "Local\\IRSDKMemMapFileName")
  private val pointer = kernel32.MapViewOfFile(handle, WinBase.FILE_MAP_READ, 0, 0, 0)

  override fun get(offset: Int, length: Int): ByteBuffer {
    return pointer.getByteBuffer(offset.toLong(), length.toLong()).order(ByteOrder.LITTLE_ENDIAN)
  }

  override fun close() {
    kernel32.CloseHandle(handle)
  }

  companion object {
    private val kernel32 = Native.load("kernel32", Kernel32::class.java, W32APIOptions.DEFAULT_OPTIONS)
  }
}