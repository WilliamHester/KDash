package me.williamhester.kdash

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Rectangle
import javax.swing.JFrame
import javax.swing.JTextField


fun main() {
    val frame = JFrame("Transparent Window")
    frame.isUndecorated = true
    frame.background = Color(0, 0, 0, 255)
    frame.isAlwaysOnTop = true
    frame.bounds = Rectangle(500, 500)
    // Without this, the window is draggable from any non transparent
    // point, including points  inside textboxes.
    frame.rootPane.putClientProperty("apple.awt.draggableWindowBackground", true)
    frame.contentPane.layout = BorderLayout()
    frame.contentPane.bounds = Rectangle(500, 500)
    frame.contentPane.add(JTextField("text field north"), BorderLayout.NORTH)
    frame.contentPane.add(JTextField("text field south"), BorderLayout.SOUTH)
    frame.isVisible = true
//    frame.pack()
}
