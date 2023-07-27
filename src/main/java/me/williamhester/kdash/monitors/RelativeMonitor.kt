package me.williamhester.kdash.monitors

import me.williamhester.kdash.api.VarBuffer
import me.williamhester.kdash.api.VarHeader
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * RelativeMonitor keeps track of the duration that a car was at the driver's car.
 */
class RelativeMonitor(
  private val headers: Map<String, VarHeader>,
) : Runnable {
  private val driverDistancesByTick = ConcurrentHashMap<Int, MutableList<Float>>()
  private val tickSeconds = mutableListOf<Double>()
  @Volatile private var latestSeconds = 0.0
  private val lock = Any()
  private val workQueue = LinkedBlockingQueue<Optional<VarBuffer>>()

  fun process(buffer: VarBuffer?) {
    workQueue.add(Optional.ofNullable(buffer))
  }

  override fun run() {
    while (true) {
      val currentBufferOptional = workQueue.take()
      if (!currentBufferOptional.isPresent) break

      val currentBuffer = currentBufferOptional.get()

      val distPctHeader = headers["CarIdxLapDistPct"]!!
      val numDrivers = distPctHeader.count

      synchronized(lock) {
        val latestSeconds = currentBuffer.getDouble("SessionTime")
        tickSeconds.add(latestSeconds)
        this.latestSeconds = latestSeconds

        for (i in 0 until numDrivers) {
          val totalCompletedLaps =
            currentBuffer.getArrayInt("CarIdxLapCompleted", i) + currentBuffer.getArrayFloat("CarIdxLapDistPct", i)
          driverDistancesByTick.computeIfAbsent(i) { mutableListOf() }.add(totalCompletedLaps)
        }
      }
    }
  }

  fun getGaps(): List<GapToCarId> = synchronized(lock) {
    val sessionSeconds = latestSeconds
    val car0Distances = driverDistancesByTick[0]!!
    val car0CurrentDistance = car0Distances.last()

    val entries = mutableListOf<GapToCarId>()
    for (car in 0 until headers["CarIdxLapCompleted"]!!.count) {
      val carDistances = driverDistancesByTick[car]!!
      val car1CurrentDistance = carDistances.last()
      val gap = when {
        car1CurrentDistance > car0CurrentDistance -> {
          val otherCarSeconds = findSecondsForDistance(car0CurrentDistance, carDistances)
          if (otherCarSeconds == null) null else sessionSeconds - otherCarSeconds
        }
        car0CurrentDistance > car1CurrentDistance -> {
          val otherCarSeconds = findSecondsForDistance(car1CurrentDistance, car0Distances)
          if (otherCarSeconds == null) null else otherCarSeconds - sessionSeconds
        }
        else -> 0.0
      }
      gap ?: continue
      entries.add(GapToCarId(gap, car))
    }
    entries.sortByDescending { it.gap }
    return entries
  }

  data class GapToCarId(val gap: Double, val carId: Int)

  private fun findSecondsForDistance(distance: Float, distances: List<Float>): Double? {
    if (distances.size < 2) return null
    var high = distances.size - 1
    var low = 0

    while (true) {
      val middle = (high + low) / 2
      val middleValue = distances[middle]
      when {
        distances[low] > distance -> return null
        (high - low) == 1 -> return interpolate(
          distance, distances[high], distances[low], tickSeconds[high], tickSeconds[low]
        )
        distance == middleValue -> return tickSeconds[middle]
        distance < middleValue -> high = middle
        else -> low = middle
      }
    }
  }

  private fun interpolate(
    targetDist: Float,
    highDist: Float,
    lowDist: Float,
    highSeconds: Double,
    lowSeconds: Double
  ): Double {
    val percentBetweenHighAndLow = (targetDist - lowDist) / (highDist - lowDist)
    val valueBetweenHighAndLow = percentBetweenHighAndLow * (highSeconds - lowSeconds)
    return valueBetweenHighAndLow + lowSeconds
  }
}
