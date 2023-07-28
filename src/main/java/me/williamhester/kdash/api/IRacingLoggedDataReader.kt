package me.williamhester.kdash.api

import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.max

/** An [IRacingDataReader] that reads from a .ibt file. */
class IRacingLoggedDataReader(path: Path) : IRacingDataReader(FileIRacingByteBufferProvider(path)) {
  private var i = 0
  private val recordCount = estimateSessionRecordCount(path)
  override val metadata = parseMetadata()

  override fun next(): VarBuffer {
    val offset = max(
      fileHeader.sessionInfoOffset + fileHeader.sessionInfoLen,
      fileHeader.varHeaderOffset + 144 * fileHeader.numVars,
    ) + i * fileHeader.bufLen
    i++
    return VarBuffer(headers, byteBufferProvider.get(offset, fileHeader.bufLen).order(ByteOrder.LITTLE_ENDIAN))
  }

  override fun hasNext(): Boolean {
    return i < recordCount
  }

  private fun estimateSessionRecordCount(path: Path): Long {
    if (fileHeader.sessionRecordCount > 0) return fileHeader.sessionRecordCount.toLong()
    val start = fileHeader.sessionInfoOffset + fileHeader.sessionInfoLen
    val end = Files.size(path)
    return (end - start) / fileHeader.bufLen
  }
}