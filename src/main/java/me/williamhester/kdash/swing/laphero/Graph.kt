package me.williamhester.kdash.swing.laphero

import me.williamhester.kdash.api.IRacingLiveDataReader
import me.williamhester.kdash.api.IRacingLoggedDataReader
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.nio.file.Paths
import javax.swing.JFrame
import javax.swing.JPanel
import me.williamhester.kdash.testing.RateLimitedIterator
import java.util.LinkedList

class GraphDrawer(
  var dataPoints: List<DataPoint>,
  comparisonValues: List<DataPoint>,
  private val dataPointsToShow: Int = 180,
) : JPanel() {
  // Hackish: For the comparison values, map it to the values and the same thing for the next lap, since we'll filter
  // down to the next 180 values for each iteration.
  private val comparisonValues =
    comparisonValues +
        comparisonValues.map { DataPoint(1.0f + it.lapPercentage, it.brakePercentage, it.gasPercentage, it.speed) }

  @Volatile
  private var minX = 0f
  private var maxX = 1f

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2d = g.create() as Graphics2D

    g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
    g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

    val dataPoints = dataPoints
    if (dataPoints.isEmpty()) {
      return
    }
    minX = dataPoints[0].lapPercentage
    maxX = dataPoints.last().lapPercentage

    // Draw horizontal lines in blue
    g2d.color = Color(0x383838)
    for (i in ORIGIN_Y..height step (height - ORIGIN_Y) / 10) {
      g2d.drawLine(ORIGIN_X, i, width, i)
    }

    // We draw the axis here instead of before because otherwise they would become blue colored.
    g2d.color = Color.BLACK
    g2d.drawLine(ORIGIN_X, ORIGIN_Y, ORIGIN_X, height)

    // TODO: This is going to break at the end of the lap. Fix this.
    val futureDataPoints = comparisonValues.filter { it.lapPercentage >= maxX }.subList(0, dataPointsToShow)

    val interpolatedPastData = interpolateData(dataPoints, comparisonValues.filter { it.lapPercentage in minX..maxX })

    val midpoint = (width * dataPointsToShow) / (dataPointsToShow + PAST_DATA_POINTS)
    val leftOfMidpoint = width - midpoint

    drawLineForProperty2(g2d, Color(0x005E00), interpolatedPastData, leftOfMidpoint, 0) { it.gasPercentage }
    drawLineForProperty2(g2d, Color(0x6C0000), interpolatedPastData, leftOfMidpoint, 0) { it.brakePercentage }
    drawLineForProperty(g2d, Color(0x005E00), futureDataPoints, midpoint, leftOfMidpoint) { it.gasPercentage }
    drawLineForProperty(g2d, Color(0x6C0000), futureDataPoints, midpoint, leftOfMidpoint) { it.brakePercentage }
    drawLineForProperty(g2d, Color(0x00AA00), dataPoints, leftOfMidpoint, 0) { it.gasPercentage }
    drawLineForProperty(g2d, Color(0x880000), dataPoints, leftOfMidpoint, 0) { it.brakePercentage }

    g2d.dispose()
  }

  private fun interpolateData(
    dataPoints: List<DataPoint>,
    pastTdfDataPoints: List<DataPoint>,
  ): List<DataPointWrapper> {
    if (pastTdfDataPoints.isEmpty()) return emptyList()
    val dataPointsIterator = dataPoints.iterator()
    val pastTdfDataPointsIterator = pastTdfDataPoints.iterator()
    var prevDataPoint = dataPointsIterator.next()
    var tdfDataPoint = pastTdfDataPointsIterator.next()

    while (tdfDataPoint.lapPercentage < prevDataPoint.lapPercentage && pastTdfDataPointsIterator.hasNext()) {
      tdfDataPoint = pastTdfDataPointsIterator.next()
    }

    val ret = mutableListOf<DataPointWrapper>()
    for ((i, dataPoint) in dataPointsIterator.withIndex()) {
      while (tdfDataPoint.lapPercentage < dataPoint.lapPercentage) {
        val percentBetweenPoints =
          (tdfDataPoint.lapPercentage - prevDataPoint.lapPercentage) /
              (dataPoint.lapPercentage - prevDataPoint.lapPercentage)
        ret.add(
          DataPointWrapper(
            ((i + 1 + percentBetweenPoints) / dataPoints.size.toFloat()),
            tdfDataPoint,
          )
        )
        if (!pastTdfDataPointsIterator.hasNext()) break
        tdfDataPoint = pastTdfDataPointsIterator.next()
      }
      prevDataPoint = dataPoint
    }
    return ret
  }

  // Need a new percentage of distance, aligned by the distance travelled in the telemetry data points.
  private class DataPointWrapper(val percent: Float, val dataPoint: DataPoint)

  private fun drawLineForProperty(
    g2d: Graphics2D,
    color: Color,
    dataPoints: List<DataPoint>,
    width: Int,
    xOffset: Int,
    property: (DataPoint) -> Float,
  ) {
    g2d.color = color
    g2d.stroke = BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, null, 0.0f,)
    val height = height - PADDING * 2

    val dataPointsIterator = dataPoints.iterator()
    val firstPoint = dataPointsIterator.next()
    var prevX = getX(0, dataPoints.size, width) + xOffset
    var prevY = getY(height, property, firstPoint)

    for ((i, point) in dataPointsIterator.withIndex()) {
      val newX = getX(i + 1, dataPoints.size, width) + xOffset
      val newY = getY(height, property, point)
//      if (newX < prevX) continue
      g2d.draw(Line2D.Float(prevX, prevY, newX, newY))
      prevX = newX
      prevY = newY
    }
  }

  private fun drawLineForProperty2(
    g2d: Graphics2D,
    color: Color,
    dataPoints: List<DataPointWrapper>,
    width: Int,
    xOffset: Int,
    property: (DataPoint) -> Float,
  ) {
    if (dataPoints.isEmpty()) return
    g2d.color = color
    g2d.stroke = BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, null, 0.0f,)
    val height = height - PADDING * 2

    val dataPointsIterator = dataPoints.iterator()
    val firstPoint = dataPointsIterator.next()
    var prevX = getX2(firstPoint, width) + xOffset
    var prevY = getY(height, property, firstPoint.dataPoint)

    for (point in dataPointsIterator) {
      val newX = getX2(point, width) + xOffset
      val newY = getY(height, property, point.dataPoint)
//      if (newX < prevX) continue
      g2d.draw(Line2D.Float(prevX, prevY, newX, newY))
      prevX = newX
      prevY = newY
    }
  }

  private fun getY(
    height: Int,
    property: (DataPoint) -> Float,
    firstPoint: DataPoint,
  ) = (height - property(firstPoint) * height) + PADDING

  private fun getX(pos: Int, size: Int, width: Int): Float {
    return pos.toFloat() / size.toFloat() * width + PADDING
  }

  private fun getX2(dataPoint: DataPointWrapper, width: Int): Float {
    return dataPoint.percent * width + PADDING
  }

  override fun getPreferredSize() = Dimension(WIDTH + PADDING * 2, HEIGHT + PADDING * 2)

  companion object {
    private const val PADDING = 8
    private const val ORIGIN_X = PADDING
    private const val ORIGIN_Y = PADDING
    private const val WIDTH = 750
    private const val HEIGHT = 200
  }
}

private const val LOOKAHEAD_DATA_POINTS = 180
private const val PAST_DATA_POINTS = 600

fun main() {
  // 1. Parse tdf
//  val tdf = Paths.get("/Users/williamhester/Programming/tdf-parsing/tdf-a")
//  val tdfParser = TdfParser(tdf)
//  val tdfValues = tdfParser.parseTdf()
//  val rateLimitedIterator = RateLimitedIterator(tdfValues.iterator())


  val currentUser = System.getProperty("user.name")
  val tdfB = Paths.get("C:\\Users\\$currentUser\\Downloads\\tdf")
  val tdfBParser = TdfParser(tdfB)
  val comparisonValues = tdfBParser.parseTdf()
  // 2. Read data from iRacing
  val dataReader = IRacingLiveDataReader()
//    IRacingLoggedDataReader(Paths.get("/Users/williamhester/Downloads/session70D82C53.ibt"))
//  val rateLimitedIterator = RateLimitedIterator(dataReader)

  val liveValues = LinkedList<DataPoint>()

  val drawer = GraphDrawer(liveValues, comparisonValues, LOOKAHEAD_DATA_POINTS).apply {
    background = Color(0x27292F)
  }

  JFrame("Lap Hero").apply {
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    isAlwaysOnTop = true
//    isUndecorated = true
    isVisible = true

    add(drawer)
    pack()
  }
  for (item in dataReader) {
    liveValues.add(
//      item
      DataPoint(
        lapPercentage = item.getFloat("LapDistPct"),
        brakePercentage = item.getFloat("Brake"),
        gasPercentage = item.getFloat("Throttle"),
        speed = item.getFloat("Speed"),
      )
    )
    while (liveValues.size > PAST_DATA_POINTS) {
      liveValues.removeFirst()
    }
    drawer.dataPoints = liveValues.toList()
    drawer.repaint()
  }
}