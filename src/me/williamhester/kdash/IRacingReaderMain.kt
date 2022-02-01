package me.williamhester.kdash

fun main() {
  val reader = IRacingDataReader(FileIRacingByteBufferProvider("/Users/williamhester/Downloads/iracing-live.ibt"))

  println(reader.readDouble("SessionTimeRemain"))
}
