package me.williamhester.kdash.api

import java.nio.ByteOrder

/** An [IRacingDataReader] that reads from the live data. */
class IRacingLiveDataReader : IRacingDataReader(LiveIRacingByteBufferProvider()) {
  private val latestHeader: VarBufferHeader
    get() = fileHeader.varBufHeaders.maxByOrNull { it.tickCount }!!

  private var previousTick = 0

  override fun next(): VarBuffer {
    while (previousTick == latestHeader.tickCount) continue
    previousTick = latestHeader.tickCount

    return VarBuffer(
      headers,
      byteBufferProvider.get(latestHeader.offset, fileHeader.bufLen).duplicate().order(ByteOrder.LITTLE_ENDIAN),
    )
  }

  override fun hasNext(): Boolean {
    return true
  }
}