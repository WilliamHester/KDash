package me.williamhester.kdash

import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.testing.RateLimitedIterator
import java.nio.file.Paths

val reader = IRacingLoggedDataReader(Paths.get("main/java/me/williamhester/kdash/sampledata/live-data.ibt"))
val iterator = RateLimitedIterator(reader)

fun main() {
  reader.headers.keys.forEach { println("$it: ${reader.headers[it]!!.description} in ${reader.headers[it]!!.unit}") }

//  iterator.forEach {
//    val speed = 2.2369362920544025 * it.getFloat("Speed")
//    print("\r$speed")
//  }

  var foundOneBefore = false
  for (buffer in reader) {
    if (buffer.getBoolean("DriverMarker")) {
      if (foundOneBefore) continue
      foundOneBefore = true
      println("Found a marker at time ${buffer["SessionTime"]}, tick ${buffer["SessionTick"]}")
    } else {
      foundOneBefore = false
    }
  }
}
