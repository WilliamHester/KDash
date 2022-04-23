package me.williamhester.kdash.testing

import com.google.common.util.concurrent.RateLimiter
import me.williamhester.kdash.api.VarBuffer

class RateLimitedIterator(private val iterator: Iterator<VarBuffer>) : Iterator<VarBuffer> {
  private val rateLimiter = RateLimiter.create(60.0)

  override fun next(): VarBuffer {
    rateLimiter.acquire()
    return iterator.next()
  }

  override fun hasNext() = iterator.hasNext()
}
