package me.williamhester.kdash.swing

import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.api.VarBuffer
import me.williamhester.kdash.testing.RateLimitedIterator
import java.awt.Color
import java.nio.file.Paths
import javax.swing.JFrame

fun main(args: Array<String>) {
  val updaterList = mutableListOf<(VarBuffer) -> Unit>()

  fun newDataBox(
    title: String,
    default: String,
    scale: Double = 1.0,
    getter: VarBuffer.() -> Any): DataBox {
    val box = DataBox(title, default, scale)
    updaterList.add { box.valueLabel.text = getter.invoke(it).toString() }
    return box
  }

  JFrame().apply {
    title = "KDash"

    add(
      column {
        row(weight = 2.0) {
          column {  }
          column {
            section(newDataBox("Speed", "0") { (getFloat("Speed", 0.0f) * 2.23694f).toInt() }, 0.2)
            section(newDataBox("Gear", "N") { getInt("Gear", 0).toGearString() })
          }
          section(newDataBox("RPM", "0") { getFloat("RPM", 0.0f).toInt() })
        }

        row(weight = 1.0) {
          column {
            row {
              section(newDataBox("LF", "1.0", 0.32) { getFloat("LFpressure", 0f).toInt() })
              section(newDataBox("RF", "1.0", 0.32) { getFloat("RFpressure", 0f).toInt() })
            }
            row {
              section(newDataBox("LR", "1.0", 0.32) { getFloat("LRpressure", 0f).toInt() })
              section(newDataBox("RR", "1.0", 0.32) { getFloat("RRpressure", 0f).toInt() })
            }
          }
          column {
            section(newDataBox("TC1", "1", 0.32) { getFloat("dcTractionControl", 0f).toInt() })
            section(newDataBox("TC2", "1", 0.32) { getFloat("dcTractionControl2", 0f).toInt() })
          }
          column {
            section(newDataBox("Current Lap", "0:00.000", 0.32) { getFloat("LapCurrentLapTime", 0f).toLapTimeString() })
            section(newDataBox("Best Lap", "0:00.000", 0.32) { getFloat("LapLastLap", 0f).toLapTimeString() })
          }
          section(newDataBox("BBAL", "50", 0.32) { getFloat("dcBrakeBias", 0f) })
        }
      }
    )

    setSize(1440, 720)
//    isUndecorated = true
//    extendedState = JFrame.MAXIMIZED_BOTH

    contentPane.background = Color.BLACK
//    isAlwaysOnTop = true
    isVisible = true
  }

  val unlimitedReader = IRacingLoggedDataReader(Paths.get(args[0]))
  for (i in 1..25000) {
    unlimitedReader.next()
  }
  val reader = RateLimitedIterator(unlimitedReader)
  for (buffer in reader) {
    for (updater in updaterList) {
      updater.invoke(buffer)
    }
  }
}