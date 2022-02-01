package me.williamhester.kdash

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.win32.W32APIOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LiveIRacingByteBufferProvider : ByteBufferProvider {
  private val handle = kernel32.OpenFileMapping(WinBase.FILE_MAP_READ, false, "Local\\IRSDKMemMapFileName")
  private val pointer = kernel32.MapViewOfFile(handle, WinBase.FILE_MAP_READ, 0, 0, 0)
  private val fileHeader: FileHeader
  override val headers: Map<String, VarHeader>

  init {
    val headerBytes = pointer.getByteBuffer(0, 144).order(ByteOrder.LITTLE_ENDIAN)
    fileHeader = FileHeader(headerBytes)

    val headers = mutableMapOf<String, VarHeader>()
    for (i in 0 until fileHeader.numVars) {
      // 144 (file header) + i * 144 (var header size)
      val varHeaderBuffer = pointer.getByteBuffer(144 * (i + 1L), 144).order(ByteOrder.LITTLE_ENDIAN)
      val header = VarHeader(varHeaderBuffer)
      headers[header.name] = header
    }
    this.headers = headers.toMap()
  }

  override fun getLatest(): ByteBuffer {
    val varBufferHeaderByteBuffer = pointer.getByteBuffer(48, 16L * fileHeader.numBuf).order(ByteOrder.LITTLE_ENDIAN)

    val buffers = mutableListOf<VarBufferHeader>()
    for (i in 0 until fileHeader.numBuf) {
      buffers.add(VarBufferHeader(varBufferHeaderByteBuffer))
    }
    buffers.sortByDescending { it.tickCount }

    val mostRecent = buffers[0]

    return pointer.getByteBuffer(mostRecent.offset, fileHeader.bufLen.toLong())
  }

  override fun close() {
    kernel32.CloseHandle(handle)
  }

  companion object {
    private val kernel32 =
      Native.loadLibrary("kernel32", Kernel32::class.java, W32APIOptions.DEFAULT_OPTIONS) as Kernel32
  }
}