package me.williamhester.kdash

import java.nio.ByteOrder

class IRacingDataReader(
  private val byteBufferProvider: ByteBufferProvider,
) {
  val fileHeader = FileHeader(byteBufferProvider.get(0, 144))
  private val headers: Map<String, VarHeader> =
    VarHeader.fromByteBufferToHeadersMap(
      byteBufferProvider.get(fileHeader.varHeaderOffset, VarHeader.SIZE * fileHeader.numVars),
      fileHeader.numVars
    )
  val headerNames: Collection<String>
    get() = headers.keys
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
}
