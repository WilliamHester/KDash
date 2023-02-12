package me.williamhester.kdash.swing

import java.awt.Color
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

fun row(
  background: Color = Color.BLACK,
  block: LinearContainer.() -> Unit,
) = LinearContainer(background, orientation = LinearContainer.Orientation.HORIZONTAL, block)

fun column(
  background: Color = Color.BLACK,
  block: LinearContainer.() -> Unit,
) = LinearContainer(background, orientation = LinearContainer.Orientation.VERTICAL, block)


fun LinearContainer.row(weight: Double = 1.0, background: Color = Color.BLACK, block: LinearContainer.() -> Unit) =
  section(row(background, block), weight)

fun LinearContainer.column(weight: Double = 1.0, background: Color = Color.BLACK, block: LinearContainer.() -> Unit) =
  section(column(background, block), weight)


class LinearContainer(
  background: Color = Color.BLACK,
  private val orientation: Orientation = Orientation.HORIZONTAL,
  block: LinearContainer.() -> Unit,
) : JPanel() {
  private var section = 0

  init {
    layout = GridBagLayout()
    this.background = background
    block(this)
  }

  fun section(component: Component, weight: Double = 1.0) {
    add(
      component,
      constraints {
        fill = GridBagConstraints.BOTH
        if (orientation == Orientation.HORIZONTAL) {
          gridx = section++
          weightx = weight
          weighty = 1.0
        } else {
          gridy = section++
          weightx = 1.0
          weighty = weight
        }
      }
    )
  }

  enum class Orientation {
    HORIZONTAL,
    VERTICAL,
  }
}
