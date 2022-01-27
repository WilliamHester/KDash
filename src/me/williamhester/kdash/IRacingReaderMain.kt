package me.williamhester.kdash

fun main() {
  val reader = IRacingDataReader("/Users/williamhester/Downloads/iracing-data.ibt")
//  val fileHeader = reader.fileHeader
//  println(fileHeader)
  println(reader.readDouble("SessionTimeRemain"))
//  println(reader.readInt("IsOnTrack"))
}
