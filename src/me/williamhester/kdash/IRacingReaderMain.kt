package me.williamhester.kdash

fun main() {
  val reader = IRacingDataReader(FileIRacingByteBufferProvider("/Users/williamhester/Downloads/iracing-data.ibt"))

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
