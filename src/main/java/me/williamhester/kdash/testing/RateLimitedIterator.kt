package me.williamhester.kdash.testing

import com.google.common.util.concurrent.RateLimiter

class RateLimitedIterator<T>(private val iterator: Iterator<T>) : Iterator<T> {
  private val rateLimiter = RateLimiter.create(60.0)

  override fun next(): T {
    rateLimiter.acquire()
    return iterator.next()
  }

  override fun hasNext() = iterator.hasNext()
}
