package me.williamhester.kdash.api

import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.Scanner
import java.util.Stack
import kotlin.math.max

/** An [IRacingDataReader] that reads from a .ibt file. */
class IRacingLoggedDataReader(path: Path) : IRacingDataReader(FileIRacingByteBufferProvider(path)) {
  private var i = 0
  private val recordCount = estimateSessionRecordCount(path)
  val sessionMetadata = parseMetadata(path)

  override fun next(): VarBuffer {
    val offset = max(
      fileHeader.sessionInfoOffset + fileHeader.sessionInfoLen,
      fileHeader.varHeaderOffset + 144 * fileHeader.numVars,
    ) + i * fileHeader.bufLen
    i++
    return VarBuffer(headers, byteBufferProvider.get(offset, fileHeader.bufLen).order(ByteOrder.LITTLE_ENDIAN))
  }

  override fun hasNext(): Boolean {
    return i < recordCount
  }

  private fun estimateSessionRecordCount(path: Path): Long {
    if (fileHeader.sessionRecordCount > 0) return fileHeader.sessionRecordCount.toLong()
    val start = fileHeader.sessionInfoOffset + fileHeader.sessionInfoLen
    val end = Files.size(path)
    return (end - start) / fileHeader.bufLen
  }

  private fun parseMetadata(path: Path): SessionMetadata {
    val metadata = SessionMetadata("")
    Files.newInputStream(path).use {
      // Skip to the start of the YAML section
      it.skip(fileHeader.sessionInfoOffset.toLong())

      val reader = Scanner(it)

      reader.nextLine() // Throw away the three dashes from the beginning of the YAML

      val stack = Stack<StackEntry>()
      var currentIndent = 0
      var currentMetadata = metadata
      var previousKey = ""
      while (true) {
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