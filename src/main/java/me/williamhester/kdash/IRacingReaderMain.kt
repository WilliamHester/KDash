package me.williamhester.kdash

import me.williamhester.kdash.api.IRacingDataReader
import java.nio.file.Paths

fun main() {
  val reader = IRacingDataReader.fromFile(Paths.get("main/java/me/williamhester/kdash/sampledata/logged-data.ibt"))

  var foundOneBefore = false
  for (i in 0 until reader.fileHeader.sessionRecordCount) {
    val buffer = reader.nthBuffer(i)
    if (buffer.getBoolean("DriverMarker")) {
      if (foundOneBefore) continue
      foundOneBefore = true
      println("Found a marker at time ${buffer["SessionTime"]}")
    } else {
      foundOneBefore = false
    }
  }
}
