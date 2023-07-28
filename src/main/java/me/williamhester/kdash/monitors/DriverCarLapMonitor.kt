package me.williamhester.kdash.monitors

import com.google.common.base.Joiner
import me.williamhester.kdash.api.IRacingDataReader
import me.williamhester.kdash.api.VarBuffer
import kotlin.math.min

class DriverCarLapMonitor(
  private val reader: IRacingDataReader,
  private val relativeMonitor: RelativeMonitor,
) {

  private val _logEntries = mutableListOf<LogEntry>()
  val logEntries: List<LogEntry> = _logEntries

  private var lapNum = -1
  private var driverName = "unknown"
  private var position = -1
  private var lapStartTime = 0.0
  private var lapTime = -1.0
  private var fuelRemaining = 0.0F
  private var trackTemp = 0.0F
  private var driverIncidents = 0
  private var teamIncidents = 0
  private var optionalRepairsRemaining = 0.0F
  private var repairsRemaining = 0.0F
  private var pitIn = false
  private var pitOut = false
  private var pitTime = 0.0
  private var pitStartTime = 0.0

  private var previousInPits = false
  private var wasInPitBox = false
  private var didAddFuel = false
  private var fuelUsedBeforeRefuel = 0.0F
  private var minFuelRemaining = 1000.0F

  fun process(buffer: VarBuffer) {
    val currentLap = buffer.getInt("Lap")
    val fuelRemaining = buffer.getFloat("FuelLevel")
    // Check that currentLap > lapNum in case we tow. Tows actually go back to lap 0 temporarily.
    if (currentLap != lapNum && currentLap > lapNum) {
      val sessionTime = buffer.getDouble("SessionTime")
      // Values that are accurate at the end of the previous lap
      position = buffer.getInt("PlayerCarPosition")
      lapTime = buffer.getDouble("SessionTime") - lapStartTime
      trackTemp = buffer.getFloat("TrackTempCrew")
      driverName = reader.metadata["DriverInfo"]["Drivers"][0]["UserName"].value
      driverIncidents = buffer.getInt("PlayerCarDriverIncidentCount")
      teamIncidents = buffer.getInt("PlayerCarTeamIncidentCount")
      val fuelUsed = (this.fuelRemaining - fuelRemaining) + fuelUsedBeforeRefuel
      this.fuelRemaining = fuelRemaining

      val gapToLeader = relativeMonitor.getGaps()[0].gap

      // Log the previous lap
      val newEntry =
        LogEntry(
          lapNum,
          driverName,
          position,
          lapTime,
          gapToLeader,
          fuelRemaining,
          fuelUsed,
          trackTemp,
          driverIncidents,
          teamIncidents,
          optionalRepairsRemaining,
          repairsRemaining,
          pitIn,
          pitOut,
          pitTime,
        )
//      println(newEntry)
      _logEntries.add(newEntry)

      // Values that are only accurate at the start of the new lap
      lapNum = currentLap
      lapStartTime = sessionTime

      pitTime = 0.0
      pitIn = false
      pitOut = false

      fuelUsedBeforeRefuel = 0.0F
      minFuelRemaining = fuelRemaining
      didAddFuel = false
    }

    minFuelRemaining = min(fuelRemaining, minFuelRemaining)
    if (fuelRemaining > minFuelRemaining) {
      if (!didAddFuel) {
        fuelUsedBeforeRefuel = this.fuelRemaining - minFuelRemaining
        didAddFuel = true
      }
      this.fuelRemaining = fuelRemaining
    }

    val inPits = buffer.getBoolean("OnPitRoad")

    pitIn = pitIn || (!previousInPits && inPits)
    pitOut = pitOut || (previousInPits && !inPits)

    val trackLocFlags = buffer.getArrayInt("CarIdxTrackSurface", 0)
    val isInPitBox = trackLocFlags == 1

    if (!wasInPitBox && isInPitBox) {
      pitStartTime = buffer.getDouble("SessionTime")
    } else if (wasInPitBox && !isInPitBox) {
      pitTime = buffer.getDouble("SessionTime") - pitStartTime
    }
    if (buffer.getBoolean("PitstopActive")) {
      optionalRepairsRemaining = buffer.getFloat("PitOptRepairLeft")
      repairsRemaining = buffer.getFloat("PitRepairLeft")
    }

    wasInPitBox = isInPitBox
    previousInPits = inPits
  }

  data class LogEntry(
    val lapNum: Int,
    val driverName: String,
    val position: Int,
    val lapTime: Double,
    val gapToLeader: Double,
    val fuelRemaining: Float,
    val fuelUsed: Float,
    val trackTemp: Float,
    val driverIncidents: Int,
    val teamIncidents: Int,
    val optionalRepairsRemaining: Float,
    val repairsRemaining: Float,
    val pitIn: Boolean,
    val pitOut: Boolean,
    val pitTime: Double,
  ) {
    override fun toString(): String {
      val paddedObjects = listOf(
        lapNum,
        driverName,
        position,
        lapTime,
        gapToLeader,
        fuelRemaining,
        fuelUsed,
        trackTemp,
        driverIncidents,
        teamIncidents,
        optionalRepairsRemaining,
        repairsRemaining,
        pitIn,
        pitOut,
        pitTime,
      ).map {
        when (it) {
          is Double,
          is Float -> String.format("%.3f", it)
          else -> {
            val string = it.toString()
            string.substring(0, min(string.length, 10))
          }
        }.padStart(10)
      }
      return Joiner.on(" | ").join(paddedObjects)
    }

    companion object {
      val HEADER = Joiner.on(" | ").join(
        listOf(
          "Lap",
          "DriverName",
          "Position",
          "LapTime",
          "Gap Leader",
          "Fuel",
          "Fuel Used",
          "TrackTemp",
          "Incidents",
          "TIncidents",
          "Opt Rep",
          "Repairs",
          "PitIn",
          "PitOut",
          "PitTime",
        ).map { it.padStart(10) }
      )
    }
  }
}