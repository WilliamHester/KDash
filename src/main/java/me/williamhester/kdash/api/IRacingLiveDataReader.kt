package me.williamhester.kdash.api

import com.google.common.util.concurrent.RateLimiter
import java.nio.ByteOrder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** An [IRacingDataReader] that reads from the live data. */
class IRacingLiveDataReader(
  liveIRacingByteBufferProvider: LiveIRacingByteBufferProvider = LiveIRacingByteBufferProvider()
) : IRacingDataReader(liveIRacingByteBufferProvider) {
  private val rateLimiter = RateLimiter.create(60.0)

  override val metadata: SessionMetadata by RateLimitCachedProperty(this::parseMetadata)
  private val latestHeader: VarBufferHeader
    get() = fileHeader.varBufHeaders.maxByOrNull {
      val tickCount = it.tickCount
      tickCount
    }!!

  private var previousTick = 0

  override fun next(): VarBuffer {
    rateLimiter.acquire()
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

  private class RateLimitCachedProperty<T : Any>(private val getter: () -> T) : ReadOnlyProperty<Any?, T> {
    private lateinit var currentValue: T
    private val rateLimiter = RateLimiter.create(1.0)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
      return if (rateLimiter.tryAcquire()) {
        val newValue = getter()
        currentValue = newValue
        newValue
      } else {
        currentValue
      }
    }
  }
}
