package me.williamhester.kdash.swing.laphero

import me.williamhester.kdash.swing.laphero.TdfHeader.DataHeader
import me.williamhester.kdash.swing.laphero.TdfHeader.EntryType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path

class TdfParser(private val tdfPath: Path) {
  fun parseTdf(): List<DataPoint> {
    Files.newInputStream(tdfPath).use {
      it.skip(16) // Skip the header

      val dataHeader = DataHeader.parseDelimitedFrom(it)
//      println(dataHeader)

      val dataMap = mutableMapOf<EntryType, List<Any>>()

      for (section in dataHeader.sectionsList) {
        val bufSize = section.len / section.numPoints
        val converter = section.type.toEntryTypeConverter(bufSize)
        val byteBuffer = ByteBuffer.allocate(converter.length)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val values = mutableListOf<Any>()
        for (point in 0 until section.numPoints) {
          it.read(byteBuffer.array())
          values += converter.convert(byteBuffer)
        }
        dataMap[section.type] = values.toList()
      }

      val result = mutableListOf<DataPoint>()
      for (i in 0 until dataHeader.dataPoints) {
        result += DataPoint(
          lapPercentage = dataMap[EntryType.LAP_PCT]!![i] as Float,
          brakePercentage = dataMap[EntryType.BRAKE_PCT]!![i] as Float,
          gasPercentage = dataMap[EntryType.GAS_PCT]!![i] as Float,
          speed = dataMap[EntryType.SPEED]!![i] as Float,
        )
      }
      return result
    }
  }
}

sealed interface EntryTypeConverter {
  val length: Int

  fun convert(buffer: ByteBuffer): Any
}

class FloatConverter : EntryTypeConverter {
  override val length = 4

  override fun convert(buffer: ByteBuffer): Float = buffer.getFloat(0)
}

class IntConverter : EntryTypeConverter {
  override val length = 4

  override fun convert(buffer: ByteBuffer): Int = buffer.getInt(0)
}

class DoubleConverter : EntryTypeConverter {
  override val length = 8

  override fun convert(buffer: ByteBuffer): Double = buffer.getDouble(0)
}

class UnknownConverter(len: Int) : EntryTypeConverter {
  override val length = len

  override fun convert(buffer: ByteBuffer): String {
    val stringBuilder = StringBuilder()
    for (i in 0 until length) {
      stringBuilder.append(buffer.get(i).toString(16))
    }
    return stringBuilder.toString()
  }
}

fun EntryType.toEntryTypeConverter(len: Int): EntryTypeConverter {
  return when (this) {
    EntryType.BRAKE_PCT,
    EntryType.CLUTCH_PCT,
    EntryType.GAS_PCT,
    EntryType.LAP_PCT,
    EntryType.STEERING,
    EntryType.RPM,
    EntryType.SPEED ->  FloatConverter()
    EntryType.GEAR -> IntConverter()
    EntryType.GPS_LON,
    EntryType.GPS_LAT -> DoubleConverter()
    else -> if (len == 4) FloatConverter() else UnknownConverter(len)
  }
}