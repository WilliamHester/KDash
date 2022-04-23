package me.williamhester.kdash.api

import java.nio.ByteOrder
import java.nio.file.Path

/** An [IRacingDataReader] that reads from a .ibt file. */
class IRacingLoggedDataReader(path: Path) : IRacingDataReader(FileIRacingByteBufferProvider(path)) {
  private var i = 0

  override fun next(): VarBuffer {
    val offset = fileHeader.sessionInfoOffset + fileHeader.sessionInfoLen + i * fileHeader.bufLen
    i++
    return VarBuffer(headers, byteBufferProvider.get(offset, fileHeader.bufLen).order(ByteOrder.LITTLE_ENDIAN))
  }

  override fun hasNext(): Boolean {
    return i < fileHeader.sessionRecordCount
  }
}