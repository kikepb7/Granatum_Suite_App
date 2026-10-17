package com.granatum.core.demo

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Illustrations of the seeded products, drawn in code and encoded as PNG, so the demo shows images
 * without shipping binary files or depending on the network. Each one is a soft gradient with the
 * product's silhouette and a floor shadow; drawn at twice the size and scaled down for smooth edges.
 */
internal object DemoArtwork {
    enum class Kind { TALL_GLASS_VASE, CERAMIC_VASE, RIBBON_SPOOL, BURLAP_BOW, WICKER_BASKET }

    private const val WIDTH = 480
    private const val HEIGHT = 360

    fun png(kind: Kind): ByteArray {
        val canvas = Canvas(WIDTH * 2, HEIGHT * 2)
        when (kind) {
            Kind.TALL_GLASS_VASE -> canvas.tallGlassVase()
            Kind.CERAMIC_VASE -> canvas.ceramicVase()
            Kind.RIBBON_SPOOL -> canvas.ribbonSpool()
            Kind.BURLAP_BOW -> canvas.burlapBow()
            Kind.WICKER_BASKET -> canvas.wickerBasket()
        }
        return Png.encode(canvas.downsampled(), WIDTH, HEIGHT)
    }

    // Scenes, in a 960 × 720 canvas.

    private fun Canvas.tallGlassVase() {
        gradient(0xE6F2F5, 0xC9E3EA)
        shadow(480.0, 640.0, 150.0)
        // Stems and flowers behind the glass.
        listOf(400.0 to 150.0, 470.0 to 110.0, 545.0 to 160.0).forEachIndexed { i, (x, top) ->
            line(480.0 + (x - 480) * 0.3, 330.0, x, top + 40, 7.0, 0x5E8C5A)
            ellipse(x, top, 46.0, 46.0, listOf(0xE8735A, 0xF2B5A0, 0xF4D27A)[i])
            ellipse(x, top, 16.0, 16.0, 0xFFF4E0)
        }
        roundRect(380.0, 240.0, 580.0, 640.0, 26.0, 0xB7DDE8, 0.75)
        roundRect(380.0, 470.0, 580.0, 640.0, 26.0, 0x9CCBDA, 0.55) // water
        roundRect(400.0, 270.0, 425.0, 610.0, 12.0, 0xFFFFFF, 0.55) // highlight
        roundRect(372.0, 232.0, 588.0, 256.0, 12.0, 0xA6D2DF, 0.9) // rim
    }

    private fun Canvas.ceramicVase() {
        gradient(0xF6EEE6, 0xE9D9C8)
        shadow(480.0, 640.0, 175.0)
        roundRect(430.0, 170.0, 530.0, 330.0, 20.0, 0xF7F5F2) // neck
        ellipse(480.0, 168.0, 70.0, 22.0, 0xE4DED7) // lip
        ellipse(480.0, 168.0, 46.0, 12.0, 0xB9AFA4)
        ellipse(480.0, 470.0, 175.0, 170.0, 0xFAF8F5) // belly
        ellipse(420.0, 420.0, 40.0, 95.0, 0xFFFFFF, 0.8) // highlight
        clip = { x, y -> sq((x - 480) / 175) + sq((y - 470) / 170) <= 1 }
        ellipse(600.0, 520.0, 150.0, 190.0, 0xE9E3DC, 0.7) // shade on the far side
        clip = null
        roundRect(380.0, 610.0, 580.0, 640.0, 14.0, 0xE4DED7) // foot
    }

    private fun Canvas.ribbonSpool() {
        gradient(0xFBE9E7, 0xF3CFCB)
        shadow(470.0, 630.0, 190.0)
        // The loose end, under the roll.
        polygon(listOf(560.0 to 520.0, 860.0 to 560.0, 900.0 to 640.0, 830.0 to 610.0, 560.0 to 600.0), 0xC8323A)
        polygon(listOf(860.0 to 560.0, 900.0 to 640.0, 830.0 to 610.0), 0xA8262E)
        ellipse(470.0, 400.0, 220.0, 220.0, 0xD63B43)
        ring(470.0, 400.0, 200.0, 185.0, 0xE85C63, 0.6)
        ring(470.0, 400.0, 150.0, 140.0, 0xB82E36, 0.5)
        ellipse(470.0, 400.0, 90.0, 90.0, 0xF2E6D8) // cardboard core
        ellipse(470.0, 400.0, 55.0, 55.0, 0xD9C7B2)
        ellipse(390.0, 300.0, 50.0, 30.0, 0xFFFFFF, 0.35)
    }

    private fun Canvas.burlapBow() {
        gradient(0xF3EFE6, 0xE2D8C3)
        shadow(480.0, 620.0, 210.0)
        polygon(listOf(470.0 to 400.0, 230.0 to 250.0, 200.0 to 560.0), 0xC9A979) // left wing
        polygon(listOf(490.0 to 400.0, 730.0 to 250.0, 760.0 to 560.0), 0xC9A979) // right wing
        polygon(listOf(460.0 to 420.0, 380.0 to 640.0, 440.0 to 620.0, 480.0 to 450.0), 0xB8965F) // tails
        polygon(listOf(500.0 to 420.0, 580.0 to 640.0, 520.0 to 620.0, 480.0 to 450.0), 0xB8965F)
        // Weave: darker threads, only over the wings.
        val left = listOf(470.0 to 400.0, 230.0 to 250.0, 200.0 to 560.0)
        val right = listOf(490.0 to 400.0, 730.0 to 250.0, 760.0 to 560.0)
        clip = { x, y -> inside(left, x, y) || inside(right, x, y) }
        var y = 262.0
        while (y < 560) {
            line(190.0, y, 770.0, y, 3.0, 0xA8885A, 0.35)
            y += 26
        }
        clip = null
        roundRect(430.0, 350.0, 530.0, 455.0, 30.0, 0xA8885A) // knot
        roundRect(445.0, 365.0, 470.0, 440.0, 12.0, 0xC9A979, 0.6)
    }

    private fun Canvas.wickerBasket() {
        gradient(0xF1EFE4, 0xDCD7BE)
        shadow(480.0, 640.0, 230.0)
        ring(480.0, 360.0, 200.0, 175.0, 0x9C6B3C, 1.0, onlyTop = true) // handle
        polygon(listOf(250.0 to 360.0, 710.0 to 360.0, 650.0 to 630.0, 310.0 to 630.0), 0xB98451)
        var y = 385.0
        var row = 0
        while (y < 625) {
            val inset = (y - 360) / 270 * 60
            line(250.0 + inset, y, 710.0 - inset, y, 9.0, if (row % 2 == 0) 0x9C6B3C else 0xC99763, 0.8)
            y += 30
            row++
        }
        var x = 290.0
        while (x < 690) {
            line(x, 365.0, 330.0 + (x - 290) * 0.85, 625.0, 4.0, 0x8A5C30, 0.45)
            x += 40
        }
        roundRect(235.0, 340.0, 725.0, 378.0, 18.0, 0x8A5C30) // rim
    }

    private fun Canvas.shadow(cx: Double, cy: Double, rx: Double) = ellipse(cx, cy, rx, rx * 0.16, 0x000000, 0.10)

    /** An RGB raster with alpha blending; shapes are tested per pixel, which is plenty at this size. */
    private class Canvas(val width: Int, val height: Int) {
        val pixels = IntArray(width * height)

        /** When set, shapes only paint where it holds. */
        var clip: ((Double, Double) -> Boolean)? = null

        fun gradient(top: Int, bottom: Int) {
            for (y in 0 until height) {
                val color = mix(top, bottom, y.toDouble() / (height - 1))
                for (x in 0 until width) pixels[y * width + x] = color
            }
        }

        fun ellipse(cx: Double, cy: Double, rx: Double, ry: Double, color: Int, alpha: Double = 1.0) =
            fill(cx - rx, cy - ry, cx + rx, cy + ry, color, alpha) { x, y -> sq((x - cx) / rx) + sq((y - cy) / ry) <= 1 }

        fun ring(cx: Double, cy: Double, outer: Double, inner: Double, color: Int, alpha: Double = 1.0, onlyTop: Boolean = false) =
            fill(cx - outer, cy - outer, cx + outer, cy + outer, color, alpha) { x, y ->
                val d = sqrt(sq(x - cx) + sq(y - cy))
                d in inner..outer && (!onlyTop || y <= cy)
            }

        fun roundRect(left: Double, top: Double, right: Double, bottom: Double, radius: Double, color: Int, alpha: Double = 1.0) =
            fill(left, top, right, bottom, color, alpha) { x, y ->
                val dx = maxOf(left + radius - x, 0.0, x - (right - radius))
                val dy = maxOf(top + radius - y, 0.0, y - (bottom - radius))
                sq(dx) + sq(dy) <= sq(radius)
            }

        fun polygon(points: List<Pair<Double, Double>>, color: Int, alpha: Double = 1.0) =
            fill(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second }, color, alpha) { x, y ->
                inside(points, x, y)
            }

        fun inside(points: List<Pair<Double, Double>>, x: Double, y: Double): Boolean {
            var inside = false
            var j = points.lastIndex
            for (i in points.indices) {
                val (xi, yi) = points[i]
                val (xj, yj) = points[j]
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
            return inside
        }

        fun line(x1: Double, y1: Double, x2: Double, y2: Double, thickness: Double, color: Int, alpha: Double = 1.0) {
            val length = sqrt(sq(x2 - x1) + sq(y2 - y1))
            val nx = -(y2 - y1) / length * thickness / 2
            val ny = (x2 - x1) / length * thickness / 2
            polygon(listOf(x1 + nx to y1 + ny, x2 + nx to y2 + ny, x2 - nx to y2 - ny, x1 - nx to y1 - ny), color, alpha)
        }

        private inline fun fill(
            left: Double,
            top: Double,
            right: Double,
            bottom: Double,
            color: Int,
            alpha: Double,
            contains: (Double, Double) -> Boolean,
        ) {
            for (y in maxOf(0, top.toInt())..minOf(height - 1, bottom.toInt() + 1)) {
                for (x in maxOf(0, left.toInt())..minOf(width - 1, right.toInt() + 1)) {
                    val px = x + 0.5
                    val py = y + 0.5
                    if (contains(px, py) && clip?.invoke(px, py) != false) {
                        val index = y * width + x
                        pixels[index] = if (alpha >= 1.0) color else mix(pixels[index], color, alpha)
                    }
                }
            }
        }

        /** Averages each 2 × 2 block: the antialiasing. */
        fun downsampled(): IntArray {
            val w = width / 2
            val h = height / 2
            return IntArray(w * h) { i ->
                val x = (i % w) * 2
                val y = (i / w) * 2
                val block = intArrayOf(pixels[y * width + x], pixels[y * width + x + 1], pixels[(y + 1) * width + x], pixels[(y + 1) * width + x + 1])
                fun channel(shift: Int) = block.sumOf { (it shr shift) and 0xFF } / 4
                (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
            }
        }
    }

    private fun sq(v: Double) = v * v

    private fun mix(a: Int, b: Int, t: Double): Int {
        fun channel(shift: Int): Int {
            val ca = (a shr shift) and 0xFF
            val cb = (b shr shift) and 0xFF
            return (ca + (cb - ca) * t).roundToInt().coerceIn(0, 255)
        }
        return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** A minimal PNG writer: 8-bit RGB, deflate "stored" blocks (no compression library needed). */
    private object Png {
        private val crcTable = IntArray(256) { n ->
            var c = n
            repeat(8) { c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else (c ushr 1) }
            c
        }

        fun encode(rgb: IntArray, width: Int, height: Int): ByteArray {
            val raw = ByteArray(height * (1 + width * 3))
            var p = 0
            for (y in 0 until height) {
                raw[p++] = 0 // filter: none
                for (x in 0 until width) {
                    val c = rgb[y * width + x]
                    raw[p++] = (c shr 16).toByte()
                    raw[p++] = (c shr 8).toByte()
                    raw[p++] = c.toByte()
                }
            }
            val out = Bytes()
            out.raw(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
            out.chunk("IHDR", Bytes().apply { int(width); int(height); raw(byteArrayOf(8, 2, 0, 0, 0)) }.toByteArray())
            out.chunk("IDAT", zlibStored(raw))
            out.chunk("IEND", ByteArray(0))
            return out.toByteArray()
        }

        private fun zlibStored(data: ByteArray): ByteArray {
            val out = Bytes()
            out.raw(byteArrayOf(0x78, 0x01))
            var offset = 0
            do {
                val size = minOf(65_535, data.size - offset)
                val last = offset + size == data.size
                out.raw(byteArrayOf(if (last) 1 else 0, size.toByte(), (size shr 8).toByte(), size.inv().toByte(), (size.inv() shr 8).toByte()))
                out.raw(data.copyOfRange(offset, offset + size))
                offset += size
            } while (!last)
            var a = 1
            var b = 0
            for (byte in data) {
                a = (a + (byte.toInt() and 0xFF)) % 65_521
                b = (b + a) % 65_521
            }
            out.int((b shl 16) or a)
            return out.toByteArray()
        }

        private fun Bytes.chunk(type: String, data: ByteArray) {
            int(data.size)
            val typed = type.encodeToByteArray() + data
            raw(typed)
            var crc = -1
            for (byte in typed) crc = crcTable[(crc xor byte.toInt()) and 0xFF] xor (crc ushr 8)
            int(crc.inv())
        }
    }

    private class Bytes {
        private var buffer = ByteArray(1024)
        private var size = 0

        fun raw(bytes: ByteArray) {
            if (size + bytes.size > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + bytes.size))
            bytes.copyInto(buffer, size)
            size += bytes.size
        }

        fun int(value: Int) = raw(byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte()))

        fun toByteArray() = buffer.copyOf(size)
    }
}
