package me.williamhester.kdash

import me.williamhester.kdash.api.IRacingDataMonitor
import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.testing.RateLimitedIterator
import java.nio.file.Paths

fun main() {
  val reader = //RateLimitedIterator(
    IRacingLoggedDataReader(Paths.get("main/java/me/williamhester/kdash/sampledata/logged-data.ibt"))
  //)
  val monitor = IRacingDataMonitor(reader)

  monitor.registerCallbacks(object : IRacingDataMonitor.Callbacks() {
    override fun onMark() {
      println("onMark!")
    }
  })

  var foundOneBefore = false
  for (buffer in reader) {
    if (buffer.getBoolean("DriverMarker")) {
      if (foundOneBefore) continue
      foundOneBefore = true
      println("Found a marker at time ${buffer["SessionTime"]}")
    } else {
      foundOneBefore = false
    }
  }
}
