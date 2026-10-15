package com.denic.geoscan.data.parser

import com.denic.geoscan.data.model.ScanPoint

/**
 * Robust stream parser for magnetometer telemetry packets formatted as:
 * `$X,Y,Z#\n` (e.g. `$1,2,1045#\n`, `$0,5,-42.8#\n`).
 *
 * Handles chunking, stream reassembly, packet concatenation, leading/trailing noise,
 * and safely drops malformed packets without interrupting the stream.
 */
class ScanDataParser {

    private val streamBuffer = StringBuilder()

    companion object {
        private const val PACKET_START = '$'
        private const val PACKET_END = '#'
        private const val MAX_BUFFER_CAPACITY = 1024
    }

    /**
     * Appends an incoming chunk from the Bluetooth SPP input stream,
     * extracts all complete and valid packets, and returns a list of parsed [ScanPoint] objects.
     *
     * @param chunk Raw string fragment received from Bluetooth socket.
     * @return List of successfully validated and parsed [ScanPoint] items.
     */
    @Synchronized
    fun parseChunk(chunk: String): List<ScanPoint> {
        streamBuffer.append(chunk)
        val extractedPoints = mutableListOf<ScanPoint>()

        while (true) {
            val startIndex = streamBuffer.indexOf(PACKET_START)
            if (startIndex == -1) {
                // No start marker in buffer; discard garbage to prevent buffer growth
                streamBuffer.clear()
                break
            }

            // Discard any noisy bytes preceding the start marker '$'
            if (startIndex > 0) {
                streamBuffer.delete(0, startIndex)
            }

            val endIndex = streamBuffer.indexOf(PACKET_END)
            if (endIndex == -1) {
                // Packet incomplete, awaiting subsequent chunks
                if (streamBuffer.length > MAX_BUFFER_CAPACITY) {
                    // Prevent memory buildup if packet was truncated indefinitely
                    streamBuffer.delete(0, 1)
                }
                break
            }

            // Extract the payload between '$' (index 0) and '#' (index endIndex)
            val packetPayload = streamBuffer.substring(1, endIndex).trim()

            // Calculate trailing line endings (\r, \n) to remove
            var removeEnd = endIndex + 1
            while (removeEnd < streamBuffer.length && (streamBuffer[removeEnd] == '\r' || streamBuffer[removeEnd] == '\n')) {
                removeEnd++
            }
            streamBuffer.delete(0, removeEnd)

            parsePacket(packetPayload)?.let { point ->
                extractedPoints.add(point)
            }
        }

        return extractedPoints
    }

    /**
     * Parses the inner string content of a single packet: "X,Y,Z"
     *
     * @param payload String with comma-delimited tokens.
     * @return [ScanPoint] if valid; null if malformed or containing invalid numbers.
     */
    fun parsePacket(payload: String): ScanPoint? {
        val tokens = payload.split(",")
        if (tokens.size != 3) {
            return null
        }

        return try {
            val x = tokens[0].trim().toInt()
            val y = tokens[1].trim().toInt()
            val z = tokens[2].trim().toFloat()
            ScanPoint(x = x, y = y, zValue = z)
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Clears internal accumulator buffer.
     */
    @Synchronized
    fun reset() {
        streamBuffer.clear()
    }
}
