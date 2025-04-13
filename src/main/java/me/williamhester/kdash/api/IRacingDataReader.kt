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

    val byteArray = byteBufferProvider.getByteArray(fileHeader.sessionInfoOffset, fileHeader.sessionInfoLen)
    Scanner(ByteArrayInputStream(byteArray)).use { reader ->
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
          currentMetadata._map[previousKey] = newMetadata
          currentMetadata = newMetadata
        }
        if (key.startsWith("- ")) {
          // Peek the stack to get the map from 1 level up
          // I think this might have an issue with lists in lists, but that's probably not necessary to handle.
          val upperMap = stack.peek().sessionMetadata

          key = key.substringAfter("- ")
          val list = upperMap[stack.peek().key]._list
          currentMetadata = SessionMetadata("")
          currentIndent = newIndent
          list.add(currentMetadata)
        }

        currentMetadata._map[key] = SessionMetadata(value)
        previousKey = key
      }
    }
    return metadata
  }

  class SessionMetadata(val value: String) : Iterable<SessionMetadata> {
    internal val _map = mutableMapOf<String, SessionMetadata>()
    val map: Map<String, SessionMetadata>
      get() = _map
    internal val _list = mutableListOf<SessionMetadata>()
    val list: List<SessionMetadata>
      get() = _list

    operator fun get(key: String): SessionMetadata {
      return _map[key] ?: SessionMetadata("")
    }

    operator fun get(pos: Int): SessionMetadata {
      if (pos >= _list.size) return SessionMetadata("")
      return _list[pos]
    }

    override fun iterator(): Iterator<SessionMetadata> = _list.iterator()
  }

  private class StackEntry(val indent: Int, val sessionMetadata: SessionMetadata, val key: String)
}
