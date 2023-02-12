package me.williamhester.kdash.swing


fun Int.toGearString(): String {
  return when {
    this == -1 -> "R"
    this == 0 -> "N"
    else -> this.toString()
  }
}

fun Float.toLapTimeString(): String {
  val int = this.toInt()
  val minutes = int / 60
  val seconds = int % 60
  val milliseconds = ((this - this.toInt()) * 1000).toInt()
  return String.format("%d:%02d.%03d", minutes, seconds, milliseconds)
}