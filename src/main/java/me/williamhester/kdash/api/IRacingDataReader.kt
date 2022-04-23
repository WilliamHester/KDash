package me.williamhester.kdash.api

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
  protected val headers: Map<String, VarHeader> =
    VarHeader.fromByteBufferToHeadersMap(
      byteBufferProvider.get(fileHeader.varHeaderOffset, VarHeader.SIZE * fileHeader.numVars),
      fileHeader.numVars
    )
}
