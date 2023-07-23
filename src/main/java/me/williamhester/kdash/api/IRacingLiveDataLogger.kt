package me.williamhester.kdash.api

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Scanner
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A live data reader that logs to a file.
 *
 * iRacing logs data to a telemetry file, but that doesn't contain all variables. This is intended for creating .ibt
 * files for local development.
 *
 * .ibt file format is
 * - File header (bytes)
 * - YAML session info
 * - repeated var buffers
 */
class IRacingLiveDataLogger(
  private val outputPath: Path,
) {
  private val liveDataBufferProvider = LiveIRacingByteBufferProvider()
  private val liveReader = IRacingLiveDataReader()
  private val isRunning = AtomicBoolean(true)

  fun collectAndLog() {
    val buffers = mutableListOf<ByteArray>()
    var numRead = 0
    while (isRunning.get() && liveReader.hasNext()) {
      if (numRead % 600 == 0) println("Read $numRead buffers")
      buffers.add(liveReader.next().byteBuffer.array())
      numRead++
    }

    val headerBuffer = liveReader.fileHeader.buffer
    headerBuffer.putInt(140, buffers.size) // Write the number of records to the "disk subheader"

    val file = Files.createFile(outputPath)
    Files.newOutputStream(file).use { outputStream ->
      outputStream.write(headerBuffer.array())

      outputStream.write(
        liveDataBufferProvider.get(
          liveReader.fileHeader.sessionInfoOffset,
          liveReader.fileHeader.sessionInfoLen,
        ).array()
      )

      for (buffer in buffers) {
        outputStream.write(buffer)
      }
    }
  }

  fun stop() {
    isRunning.set(true)
  }
}

fun main() {
  val outputPath = Paths.get("livedata.ibt")
  val logger = IRacingLiveDataLogger(outputPath)
  logger.collectAndLog()

  println("Reading from live data...")
  val scanner = Scanner(System.`in`)
  scanner.nextLine()
  logger.stop()

  println("Wrote to livedata.ibt")
}