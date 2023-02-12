package me.williamhester.kdash.swing

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel


class DataBox(
  title: String,
  value: String,
  scale: Double = 1.0,
): JPanel() {
  val valueLabel: JLabel

  init {
    layout = GridBagLayout()
    background = null

    add(
      label(title, textSize = 20).apply {
        background = Color.BLACK
        isOpaque = true
      }
    )

    valueLabel = label(value, textSize = (84 * scale).toInt())
    add(
      valueLabel,
      constraints {
        fill = GridBagConstraints.BOTH
        weighty = 1.0
        gridy = 1
      }
    )
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    g.create().apply {
      this as Graphics2D

      color = Color.BLUE
      stroke = BasicStroke(1.3f)

      drawRoundRect(10, 10, size.width - 20, size.height - 20, 20, 20)
    }
  }

  override fun getPreferredSize(): Dimension = Dimension()
}