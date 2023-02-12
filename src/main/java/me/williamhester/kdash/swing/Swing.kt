package me.williamhester.kdash.swing

import java.awt.Color
import java.awt.Font
import java.awt.GridBagConstraints
import javax.swing.JLabel
import javax.swing.border.EmptyBorder

fun constraints(block: GridBagConstraints.() -> Unit): GridBagConstraints = GridBagConstraints().apply(block)
fun label(value: String, textSize: Int = 12): JLabel = JLabel(value).apply {
  font = Font("Monospaced", Font.BOLD, textSize)
  horizontalAlignment = JLabel.CENTER
  verticalAlignment = JLabel.CENTER
  foreground = Color.WHITE
  border = EmptyBorder(0, 8, 0, 8)
}