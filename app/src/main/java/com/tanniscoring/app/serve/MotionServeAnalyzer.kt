package com.tanniscoring.app.serve

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight on-device serve / return candidate detector.
 *
 * MVP: frame-to-frame luminance energy in the **upper/far** band = serve candidate;
 * energy in the **mid/lower** band after a serve = return-like motion.
 * Optional yellow-green chroma boost when YUV suggests tennis-ball hues (no TFLite).
 *
 * Not radar-accurate — feeds [ServeSessionEngine] with pixels/sec peaks only.
 */
class MotionServeAnalyzer(
    private val onServePeak: (pixelsPerSecond: Float) -> Unit,
    private val onReturnLike: () -> Unit,
) : ImageAnalysis.Analyzer {

    private var prevLuma: ByteArray? = null
    private var prevWidth = 0
    private var prevHeight = 0
    private var lastTsNs = 0L
    private var cooldownUntilMs = 0L
    private var returnArmedUntilMs = 0L
    private val enabled = AtomicBoolean(true)

    /** After a serve is accepted by the session, arm return detection for a few seconds. */
    fun armReturnWindow(durationMs: Long = 4_500L) {
        returnArmedUntilMs = System.currentTimeMillis() + durationMs
    }

    fun clearReturnWindow() {
        returnArmedUntilMs = 0L
    }

    fun setEnabled(value: Boolean) {
        enabled.set(value)
        if (!value) {
            prevLuma = null
            clearReturnWindow()
        }
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (!enabled.get()) return
            if (image.format != ImageFormat.YUV_420_888 && image.planes.isEmpty()) return
            val yPlane = image.planes[0]
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = image.width
            val height = image.height
            val nowNs = image.imageInfo.timestamp.takeIf { it > 0 } ?: System.nanoTime()
            val luma = extractDownsampledLuma(buffer, rowStride, width, height, SAMPLE_STEP)
            val sw = width / SAMPLE_STEP
            val sh = height / SAMPLE_STEP
            val prev = prevLuma
            if (prev != null && prevWidth == sw && prevHeight == sh && lastTsNs > 0L) {
                val dtSec = ((nowNs - lastTsNs).coerceAtLeast(1L)) / 1_000_000_000.0
                if (dtSec in 0.01..0.5) {
                    analyzeDiff(luma, prev, sw, sh, dtSec)
                }
            }
            prevLuma = luma
            prevWidth = sw
            prevHeight = sh
            lastTsNs = nowNs
        } finally {
            image.close()
        }
    }

    private fun analyzeDiff(
        curr: ByteArray,
        prev: ByteArray,
        w: Int,
        h: Int,
        dtSec: Double,
    ) {
        val nowMs = System.currentTimeMillis()
        // Upper 40% of frame = far court / serve toss & strike region
        val serveBandEnd = (h * 0.42f).toInt().coerceAtLeast(2)
        // Mid-lower = return / rally region
        val returnBandStart = (h * 0.35f).toInt()
        var serveEnergy = 0.0
        var serveMotionPx = 0
        var returnEnergy = 0.0
        var returnMotionPx = 0
        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val d = abs((curr[idx].toInt() and 0xFF) - (prev[idx].toInt() and 0xFF))
                if (d >= DIFF_THRESHOLD) {
                    if (y < serveBandEnd) {
                        serveEnergy += d
                        serveMotionPx++
                    }
                    if (y >= returnBandStart) {
                        returnEnergy += d
                        returnMotionPx++
                    }
                }
                idx++
            }
        }
        val serveDensity = serveMotionPx.toDouble() / (w * serveBandEnd).coerceAtLeast(1)
        val returnDensity = returnMotionPx.toDouble() /
            (w * (h - returnBandStart).coerceAtLeast(1))

        if (nowMs < cooldownUntilMs) {
            // Still allow return detection during cooldown after serve peak
            maybeEmitReturn(returnDensity, returnEnergy, nowMs)
            return
        }

        // Serve candidate: strong upper-band motion peak
        if (serveDensity >= SERVE_DENSITY_MIN && serveEnergy >= SERVE_ENERGY_MIN) {
            // Approximate peak travel: sqrt(motionPixels) * step / dt as px/sec proxy
            val pxPerSec = (sqrt(serveMotionPx.toDouble()) * SAMPLE_STEP / dtSec).toFloat()
            if (pxPerSec >= MIN_PX_PER_SEC) {
                cooldownUntilMs = nowMs + SERVE_COOLDOWN_MS
                armReturnWindow()
                onServePeak(pxPerSec)
                return
            }
        }
        maybeEmitReturn(returnDensity, returnEnergy, nowMs)
    }

    private fun maybeEmitReturn(returnDensity: Double, returnEnergy: Double, nowMs: Long) {
        if (nowMs > returnArmedUntilMs || returnArmedUntilMs == 0L) return
        if (returnDensity >= RETURN_DENSITY_MIN && returnEnergy >= RETURN_ENERGY_MIN) {
            returnArmedUntilMs = 0L
            onReturnLike()
        }
    }

    companion object {
        private const val SAMPLE_STEP = 4
        private const val DIFF_THRESHOLD = 28
        private const val SERVE_DENSITY_MIN = 0.035
        private const val SERVE_ENERGY_MIN = 2_500.0
        private const val RETURN_DENSITY_MIN = 0.04
        private const val RETURN_ENERGY_MIN = 2_000.0
        private const val MIN_PX_PER_SEC = 400f
        private const val SERVE_COOLDOWN_MS = 1_200L

        private fun extractDownsampledLuma(
            buffer: ByteBuffer,
            rowStride: Int,
            width: Int,
            height: Int,
            step: Int,
        ): ByteArray {
            val sw = width / step
            val sh = height / step
            val out = ByteArray(sw * sh)
            val dup = buffer.duplicate()
            var o = 0
            var y = 0
            while (y + step <= height) {
                val rowStart = y * rowStride
                var x = 0
                while (x + step <= width) {
                    val pos = rowStart + x
                    if (pos < dup.limit()) {
                        out[o++] = dup.get(pos)
                    } else {
                        out[o++] = 0
                    }
                    x += step
                }
                y += step
            }
            return out
        }
    }
}
