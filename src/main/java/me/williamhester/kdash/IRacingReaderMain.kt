package me.williamhester.kdash

import me.williamhester.kdash.api.IRacingLoggedDataReader
import java.nio.file.Paths
import kotlin.system.exitProcess


fun main(args: Array<String>) {
  val reader = IRacingLoggedDataReader(Paths.get(args[0]))

  // Print each variable and its unit

  reader.headers.keys.forEach { println("$it\t${reader.headers[it]!!.description}\t${reader.headers[it]!!.unit}") }
  if (System.currentTimeMillis() > 0) exitProcess(0)

//  val weekendInfo = reader.sessionMetadata["WeekendInfo"]
//  println("SessionID: ${weekendInfo["SessionID"].value}")
//  println("SubSessionID: ${weekendInfo["SubSessionID"].value}")

//  var foundOneBefore = false
  var lap = 0
  var lastLapTime = 0.0f
  var buffersIntoNewLap = 0
  var prevLapN = 0
  var prevLapDistPct = 0.0f
  var lapEstStartTime = 0.0
  var prevSessionTime = 0.0

  for (buffer in reader) {
//    println("UniqueSessionID: ${buffer.getInt("SessionUniqueID")}")
//    if (System.currentTimeMillis() > 1) break

    val newLap = buffer.getInt("Lap")
    val newTime = buffer.getFloat("LapLastLapTime")
    val newLapN = buffer.getInt("LapLasNLapSeq")
    buffersIntoNewLap++

    if (newLapN != prevLapN) {
      println("New 'LapLasNLapSeq' different ($newLapN - $prevLapN), buffer: $buffersIntoNewLap")
    }
    prevLapN = newLapN

    when {
      lap == newLap && lastLapTime == newTime -> {}
      lap == newLap && lastLapTime != newTime -> println("Lap $lap, old time: $lastLapTime, new time: $newTime, buffers: $buffersIntoNewLap")
      lap != newLap && lastLapTime == newTime -> {}// println("Lap changed ($lap -> $newLap), but the lastLapTime didn't")
      else -> println("New lap and new time")
    }

    val currentSessionTime = buffer.getDouble("SessionTime")
    val currentLapDistPct = buffer.getFloat("LapDistPct")
    if (lap != newLap) {
      lap = newLap
      buffersIntoNewLap = 0

      val percentCompletedBetweenEvents = (1 + currentLapDistPct) - prevLapDistPct
      val percentTo100 = 1 - prevLapDistPct
      val weight = percentTo100 / percentCompletedBetweenEvents
      val sessionTimeDelta = prevSessionTime - currentSessionTime
      val newLapStartTime = weight * sessionTimeDelta + currentSessionTime
      println("Computed lap time: ${newLapStartTime - lapEstStartTime}")
      lapEstStartTime = newLapStartTime
    }
    prevSessionTime = currentSessionTime
    prevLapDistPct = currentLapDistPct

    lastLapTime = newTime

//    if (buffer.getBoolean("DriverMarker")) {
//      if (foundOneBefore) continue
//      foundOneBefore = true
//      println("Found a marker at time ${buffer["SessionTime"]}, tick ${buffer["SessionTick"]}")
//    } else {
//      foundOneBefore = false
//    }
//    print("\r                                 ")
//    print("${buffer.getDouble("Lat")}")
//    Thread.sleep(1000 / 60)
  }
}
