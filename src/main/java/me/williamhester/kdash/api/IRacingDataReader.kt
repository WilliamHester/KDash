package me.williamhester.kdash.api

import java.nio.ByteOrder
import java.nio.file.Path

/**
 * IRacingDataReader provides methods for accessing iRacing's telemetry data, both live and logged.
 *
 * This class serves two purposes:
 * 1. Serving the file header and variable metadata
 * 2. Serving the [VarBuffer]s.
 *
 * For live data, use [mostRecentBuffer] to access the most recent buffer. For logged data, [nthBuffer] will provide the
 * nth buffer in order.
 *
 * To access this class, use one of the factory methods, either [fromFile] for logged data or [fromLiveData] for live
 * data.
 */
class IRacingDataReader
internal constructor(
  private val byteBufferProvider: ByteBufferProvider,
) {
  val fileHeader = FileHeader(byteBufferProvider.get(0, 144))
  private val headers: Map<String, VarHeader> =
    VarHeader.fromByteBufferToHeadersMap(
      byteBufferProvider.get(fileHeader.varHeaderOffset, VarHeader.SIZE * fileHeader.numVars),
      fileHeader.numVars
    )
  val mostRecentBuffer: VarBuffer
    get() = findMostRecentBuffer()

  fun nthBuffer(nthBuffer: Int): VarBuffer {
    val offset = fileHeader.sessionInfoOffset + fileHeader.sessionInfoLen + nthBuffer * fileHeader.bufLen
    return VarBuffer(headers, byteBufferProvider.get(offset, fileHeader.bufLen).order(ByteOrder.LITTLE_ENDIAN))
  }

  private fun findMostRecentBuffer(): VarBuffer {
    val latestHeader = fileHeader.varBufHeaders.maxByOrNull { it.tickCount }!!

    return VarBuffer(
      headers,
      byteBufferProvider.get(latestHeader.offset, fileHeader.bufLen).duplicate().order(ByteOrder.LITTLE_ENDIAN),
    )
  }

  companion object {
    fun fromFile(path: Path): IRacingDataReader {
      return IRacingDataReader(FileIRacingByteBufferProvider(path))
    }

    fun fromLiveData(): IRacingDataReader {
      return IRacingDataReader(LiveIRacingByteBufferProvider())
    }
  }
}
