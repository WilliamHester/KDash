package me.williamhester.kdash.api

import java.io.ByteArrayInputStream
import java.util.Scanner
import java.util.Stack

/**
 * IRacingDataReader provides methods for accessing iRacing's telemetry data, both live and logged.
 *
 * This class serves two purposes:
 * 1. Serving the file header and variable metadata
 * 2. Serving the [VarBuffer]s.
 */
abstract class IRacingDataReader
internal constructor(
  internal val byteBufferProvider: ByteBufferProvider,
) : Iterator<VarBuffer> {
  val fileHeader = FileHeader(byteBufferProvider.get(0, 144))
  val headers: Map<String, VarHeader> =
    VarHeader.fromByteBufferToHeadersMap(
      byteBufferProvider.get(fileHeader.varHeaderOffset, VarHeader.SIZE * fileHeader.numVars),
      fileHeader.numVars
    )
  abstract val metadata: SessionMetadata

  internal fun parseMetadata(): SessionMetadata {
    val metadata = SessionMetadata("")

    val byteBuffer = byteBufferProvider.get(fileHeader.sessionInfoOffset, fileHeader.sessionInfoLen)
    Scanner(ByteArrayInputStream(byteBuffer.array())).use { reader ->
      reader.nextLine() // Throw away the three dashes from the beginning of the YAML

      val stack = Stack<StackEntry>()
      var currentIndent = 0
      var currentMetadata = metadata
      var previousKey = ""
      while (reader.hasNextLine()) {
        val line = reader.nextLine().trimEnd()
        if (line.isEmpty()) continue
        if (line == "...") break
        val trimmed = line.trimStart()
        val newIndent = (line.length - trimmed.length) + if (trimmed.startsWith("- ")) 2 else 0

        while (newIndent < currentIndent) {
          val stackEntry = stack.pop()
          currentIndent = stackEntry.indent
          currentMetadata = stackEntry.sessionMetadata
        }

        var key = trimmed.substringBefore(":")
        val value = trimmed.substringAfter(":").trim()

        if (newIndent > currentIndent) {
          stack.push(StackEntry(currentIndent, currentMetadata, previousKey))
          currentIndent = newIndent
          val newMetadata = SessionMetadata("")
          currentMetadata.map[previousKey] = newMetadata
          currentMetadata = newMetadata
        }
        if (key.startsWith("- ")) {
          // Peek the stack to get the map from 1 level up
          // I think this might have an issue with lists in lists, but that's probably not necessary to handle.
          val upperMap = stack.peek().sessionMetadata

          key = key.substringAfter("- ")
          val list = upperMap[stack.peek().key].list
          currentMetadata = SessionMetadata("")
          currentIndent = newIndent
          list.add(currentMetadata)
        }

        currentMetadata.map[key] = SessionMetadata(value)
        previousKey = key
      }
    }
    return metadata
  }

  class SessionMetadata(val value: String) : Iterable<SessionMetadata> {
    internal val map = mutableMapOf<String, SessionMetadata>()
    internal val list = mutableListOf<SessionMetadata>()

    operator fun get(key: String): SessionMetadata {
      return map[key] ?: SessionMetadata("")
    }

    operator fun get(pos: Int): SessionMetadata {
      if (pos >= list.size) return SessionMetadata("")
      return list[pos]
    }

    override fun iterator(): Iterator<SessionMetadata> = list.iterator()
  }

  private class StackEntry(val indent: Int, val sessionMetadata: SessionMetadata, val key: String)
}
