package me.williamhester.kdash.api

import com.google.common.util.concurrent.RateLimiter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
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
 * files for local development. This also generates its own file format (.irh, iRacing Header), which is a series of
 * session info strings with the null bytes at the end removed.
 *
 * .ibt file format is
 * - File header (bytes)
 * - YAML session info
 * - repeated var buffers
 * .irh file format is (repeated)
 * - SessionTime (double)
 * - session info length (without padded zeroes)
 * - YAML session info
 */
class IRacingLiveDataLogger(
  private val outputPath: Path,
  private val headerOutputPath: Path,
) {
  private val isRunning = AtomicBoolean(true)

  fun collectAndLog() {
    val liveDataBufferProvider = connectToLiveTelemetry()
    val liveReader = IRacingLiveDataReader(liveDataBufferProvider)
    val metadataBytesPerSecondWriteRateLimiter = RateLimiter.create(400_000.0)

    val ibtFileChannel = FileChannel.open(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    val headerFileChannel = FileChannel.open(headerOutputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

    // Skip over the file header contents so we can append to the bottom of the file first (just the repeated
    // var buffers). We'll write the header at the end.
    val bytesToSkip =
      144 + // 144 bytes for the header
        liveReader.fileHeader.sessionInfoLen + // bytes for the session info (metadata)
        liveReader.fileHeader.numVars * 144 // Each var header is 144 bytes
    ibtFileChannel.position(bytesToSkip.toLong())

    val buffers = mutableListOf<ByteArray>()
    var numRead = 0
    var previousMetadataVersion = -1
    val sessionInfoHeaderByteBuffer = ByteBuffer.allocate(12)
    try {
      while (isRunning.get() && liveReader.hasNext()) {
        if (numRead % (60 * 30) == 0) println("Read $numRead buffers")
        val nextBuffer = liveReader.next()
        ibtFileChannel.write(nextBuffer.byteBuffer)
        numRead++

        val metadataVersion = liveReader.fileHeader.sessionInfoUpdate
        if (metadataVersion != previousMetadataVersion) {
          val header = liveDataBufferProvider.get(
            liveReader.fileHeader.sessionInfoOffset,
            liveReader.fileHeader.sessionInfoLen,
          )
          val limit = header.setLimitToLastNonZeroByte()

          if (metadataBytesPerSecondWriteRateLimiter.tryAcquire(limit)) {
            sessionInfoHeaderByteBuffer.apply {
              clear()
              putDouble(nextBuffer.getDouble("SessionTime"))
              putInt(header.limit())
              flip()
            }
            headerFileChannel.write(sessionInfoHeaderByteBuffer)
            headerFileChannel.write(header)
            previousMetadataVersion = metadataVersion
            println("Wrote a new session info string (version $metadataVersion), ${limit / 1024}KB")
          }
        }
      }

      // Move back to the start of the file. Time to write the header.
      ibtFileChannel.position(0)

      val readOnlyHeaderBuffer = liveReader.fileHeader.buffer
      val mutableHeaderBuffer = ByteBuffer.allocate(144).order(ByteOrder.LITTLE_ENDIAN)
      readOnlyHeaderBuffer.get(mutableHeaderBuffer.array())
      mutableHeaderBuffer.putInt(20, 144) // Override the sessionInfoOffset to look like a logged data header
      mutableHeaderBuffer.putInt(28, 144 + liveReader.fileHeader.sessionInfoLen)
      mutableHeaderBuffer.putInt(140, buffers.size) // Write the number of records to the "disk subheader"
      ibtFileChannel.write(mutableHeaderBuffer)

      val sessionInfoBuffer = liveDataBufferProvider.get(
        liveReader.fileHeader.sessionInfoOffset,
        liveReader.fileHeader.sessionInfoLen,
      )
      ibtFileChannel.write(sessionInfoBuffer)

      val varHeaderBuffer = liveDataBufferProvider.get(
        liveReader.fileHeader.varHeaderOffset,
        liveReader.fileHeader.numVars * 144, // Each var header is 144 bytes
      )
      ibtFileChannel.write(varHeaderBuffer)

      ibtFileChannel.close()
      headerFileChannel.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun connectToLiveTelemetry(): LiveIRacingByteBufferProvider {
    do {
      try {
        return LiveIRacingByteBufferProvider()
      } catch (e: Exception) {
        println("Failed to connect to iRacing live telemetry. Trying again in 5 seconds...")
        e.printStackTrace()
        Thread.sleep(5_000)
      }
    } while (true)
  }

  fun ByteBuffer.setLimitToLastNonZeroByte(): Int {
    var newLimit = limit()
    for (i in limit() - 1 downTo 0) {
      if (get(i).toInt() != 0) {
        newLimit = i + 1
        break
      }
    }
    limit(newLimit)
    return newLimit
  }

  fun stop() {
    isRunning.set(false)
  }
}

fun main() {
  val currentUser = System.getProperty("user.name")
  val downloadsDir = Paths.get("C:\\Users\\$currentUser\\Downloads")
  val outputPath = Files.createTempFile(downloadsDir, "livedata", ".ibt")
  val sessionInfoOutputPath = Files.createTempFile(downloadsDir, "livesessioninfo", ".irh")
  val logger = IRacingLiveDataLogger(outputPath, sessionInfoOutputPath)
  val thread = thread {
    logger.collectAndLog()
  }

  println("Reading from live data. Press enter to stop...")
  val scanner = Scanner(System.`in`)
  scanner.nextLine()
  logger.stop()

  println("""
    Wrote live data to ${outputPath.absolute()}
    Wrote session info to ${sessionInfoOutputPath.absolute()}
    """.trimIndent()
  )

  thread.join()

  println("Press enter to exit.")
  scanner.nextLine()
}