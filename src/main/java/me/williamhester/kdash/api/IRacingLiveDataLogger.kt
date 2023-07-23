package me.williamhester.kdash.api

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Scanner
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.io.path.absolute

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
    try {
      while (isRunning.get() && liveReader.hasNext()) {
        if (numRead % 600 == 0) println("Read $numRead buffers")
        val array = ByteArray(liveReader.fileHeader.bufLen)
        val nextBuffer = liveReader.next()
        nextBuffer.byteBuffer.get(array)
        buffers.add(array)
        numRead++

        println(nextBuffer.getFloat("Speed"))
      }

      val readOnlyHeaderBuffer = liveReader.fileHeader.buffer
      val headerBuffer = ByteBuffer.allocate(144).order(ByteOrder.LITTLE_ENDIAN)
      readOnlyHeaderBuffer.get(headerBuffer.array())
      headerBuffer.putInt(20, 144) // Override the sessionInfoOffset to look like a logged data header
      headerBuffer.putInt(28, 144 + liveReader.fileHeader.sessionInfoLen)
      headerBuffer.putInt(140, buffers.size) // Write the number of records to the "disk subheader"

      Files.newOutputStream(outputPath, StandardOpenOption.CREATE).use { outputStream ->
        val array = ByteArray(144) // 144 is FileHeader's length
        headerBuffer.get(array)
        outputStream.write(array)

        val sessionInfoBytes = ByteArray(liveReader.fileHeader.sessionInfoLen)
        val sessionInfoBuffer = liveDataBufferProvider.get(
          liveReader.fileHeader.sessionInfoOffset,
          liveReader.fileHeader.sessionInfoLen,
        )
        sessionInfoBuffer.get(sessionInfoBytes)
        outputStream.write(sessionInfoBytes)

        val varHeaderBytes = ByteArray(liveReader.fileHeader.numVars * 144) // Each var header is 144 bytes
        val varHeaderBuffer = liveDataBufferProvider.get(
          liveReader.fileHeader.varHeaderOffset,
          varHeaderBytes.size,
        )
        varHeaderBuffer.get(varHeaderBytes)
        outputStream.write(varHeaderBytes)

        for (buffer in buffers) {
          outputStream.write(buffer)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun stop() {
    isRunning.set(false)
  }
}

fun main() {
  val currentUser = System.getProperty("user.name")
  val outputPath = Paths.get("C:\\Users\\$currentUser\\Downloads\\livedata.ibt")
  val logger = IRacingLiveDataLogger(outputPath)
  val thread = thread {
    logger.collectAndLog()
  }

  println("Reading from live data...")
  val scanner = Scanner(System.`in`)
  scanner.nextLine()
  logger.stop()

  println("Wrote to ${outputPath.absolute()}")

  thread.join()
}