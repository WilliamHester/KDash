package me.williamhester.kdash.api

class IRacingDataMonitor(
  private val reader: Iterator<VarBuffer>,
) : AutoCloseable {
  private val thread = Thread(this::run)
  private val callbacksList = mutableListOf<Callbacks>()
  private var previousBuffer: VarBuffer? = null

  init {
    thread.start()
  }

  private fun run() {
    for (currentBuffer in reader) {
      // When we get a new tick, send updated values to subscribers
      if (previousBuffer?.getBoolean("DriverMarker") != currentBuffer.getBoolean("DriverMarker")) {
        for (callback in callbacksList) {
          callback.onMark()
        }
      }
      previousBuffer = currentBuffer
    }
  }

  fun registerCallbacks(callbacks: Callbacks) {
    callbacksList.add(callbacks)
  }

  override fun close() {
    thread.interrupt()
  }

  abstract class Callbacks {
    open fun onMark() {}
  }
}