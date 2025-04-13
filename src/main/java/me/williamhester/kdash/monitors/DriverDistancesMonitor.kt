package me.williamhester.kdash.monitors

import me.williamhester.kdash.api.IRacingDataReader
import me.williamhester.kdash.api.VarBuffer

class DriverDistancesMonitor(
  private val iRacingDataReader: IRacingDataReader,
) {
  private val _distances = mutableListOf<DriverDistances>()
  val distances: List<DriverDistances>
    get() = _distances
  private var lastSessionTime = 0.0

  fun process(buffer: VarBuffer) {
    val sessionTime = buffer.getDouble("SessionTime")
    if (sessionTime < lastSessionTime + 1) return

    lastSessionTime = sessionTime
    val numDrivers = iRacingDataReader.headers["CarIdxLapCompleted"]!!.count

    val tickDistances = mutableListOf<DriverDistance>()
    for (i in 0 until numDrivers) {
      val distance = buffer.getArrayInt("CarIdxLapCompleted", i).toFloat() + buffer.getArrayFloat("CarIdxLapDistPct", i)
      tickDistances.add(DriverDistance(i, distance))
    }
    _distances.add(DriverDistances(sessionTime, tickDistances))
  }

  data class DriverDistances(
    val sessionTime: Double,
    val distances: List<DriverDistance>,
  )

  data class DriverDistance(
    val carId: Int,
    val distance: Float,
  )
}