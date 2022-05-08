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
    val differ = BufferDiffer()
    for (currentBuffer in reader) {
      differ.updateBuffer(currentBuffer)
      // When we get a new tick, send updated values to subscribers
      differ.checkBooleanChangedToTrue("DriverMarker") { onMark() }
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

  private inner class BufferDiffer {
    private var lastBuffer: VarBuffer? = null
    private var currentBuffer: VarBuffer? = null

    fun updateBuffer(newBuffer: VarBuffer) {
      lastBuffer = currentBuffer
      currentBuffer = newBuffer
    }

    fun checkBooleanChangedToTrue(key: String, block: Callbacks.() -> Unit) {
      if ((lastBuffer?.getBoolean(key) == false) && currentBuffer!!.getBoolean(key)) {
        callBack {
          block()
        }
      }
    }

    private fun callBack(block: Callbacks.() -> Unit) {
      for (callback in callbacksList) {
        callback.block()
      }
    }
  }
}