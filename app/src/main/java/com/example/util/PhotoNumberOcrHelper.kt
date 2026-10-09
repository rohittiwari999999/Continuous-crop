package com.example.util

import android.graphics.Bitmap
import android.graphics.Matrix
import com.example.data.model.CropRect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object PhotoNumberOcrHelper {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    data class DetectedPhotoItem(
        val cropRect: CropRect,
        val detectedNumber: String,
        val isAbove: Boolean = false
    )

    suspend fun detectNumberAroundCrop(
        source: Bitmap,
        cropRect: CropRect,
        rotationDegrees: Int = 0,
        isFlipped: Boolean = false
    ): String? = withContext(Dispatchers.Default) {
        val oriented = if (rotationDegrees != 0 || isFlipped) {
            val matrix = Matrix()
            if (isFlipped) matrix.postScale(-1f, 1f)
            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val srcW = oriented.width
        val srcH = oriented.height

        // 1. First check BELOW the photo (common layout: photo, then label below)
        val belowLeft = (cropRect.left - cropRect.width * 0.15f).coerceAtLeast(0f)
        val belowRight = (cropRect.right + cropRect.width * 0.15f).coerceAtMost(1f)
        val belowTop = (cropRect.bottom - cropRect.height * 0.08f).coerceIn(0f, 1f)
        val belowBottom = (cropRect.bottom + cropRect.height * 0.35f).coerceAtMost(1f)

        var result = scanRegionForNumber(oriented, belowLeft, belowTop, belowRight, belowBottom, srcW, srcH)

        // 2. If not found below, check ABOVE the photo (title/roll number above head)
        if (result.isNullOrBlank()) {
            val aboveLeft = (cropRect.left - cropRect.width * 0.15f).coerceAtLeast(0f)
            val aboveRight = (cropRect.right + cropRect.width * 0.15f).coerceAtMost(1f)
            val aboveTop = (cropRect.top - cropRect.height * 0.35f).coerceAtLeast(0f)
            val aboveBottom = (cropRect.top + cropRect.height * 0.08f).coerceIn(0f, 1f)

            result = scanRegionForNumber(oriented, aboveLeft, aboveTop, aboveRight, aboveBottom, srcW, srcH)
        }

        if (oriented != source) {
            oriented.recycle()
        }

        result
    }

    private suspend fun scanRegionForNumber(
        bitmap: Bitmap,
        normLeft: Float,
        normTop: Float,
        normRight: Float,
        normBottom: Float,
        srcW: Int,
        srcH: Int
    ): String? {
        val pxLeft = (normLeft * srcW).toInt().coerceIn(0, srcW - 1)
        val pxTop = (normTop * srcH).toInt().coerceIn(0, srcH - 1)
        val pxWidth = ((normRight - normLeft) * srcW).toInt().coerceIn(1, srcW - pxLeft)
        val pxHeight = ((normBottom - normTop) * srcH).toInt().coerceIn(1, srcH - pxTop)

        if (pxWidth <= 5 || pxHeight <= 5) return null

        val regionBitmap = try {
            Bitmap.createBitmap(bitmap, pxLeft, pxTop, pxWidth, pxHeight)
        } catch (_: Exception) {
            return null
        }

        val image = InputImage.fromBitmap(regionBitmap, 0)
        val recognizedText: String = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText: Text ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        }

        if (regionBitmap != bitmap) {
            regionBitmap.recycle()
        }

        return extractBestNumber(recognizedText)
    }

    fun extractBestNumber(rawText: String): String? {
        if (rawText.isBlank()) return null

        val lines = rawText.lines()
        val candidateNumbers = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            // Look for digits like 101, 102, 001, etc.
            val matches = Regex("\\b\\d{2,16}\\b").findAll(trimmed)
            for (match in matches) {
                candidateNumbers.add(match.value)
            }

            // Also check OCR common replacements (O -> 0)
            val correctedLine = trimmed
                .replace(Regex("(?i)\\b[oO](\\d{2,})"), "0$1")
                .replace(Regex("(\\d{2,})[oO]\\b"), "$10")
            val correctedMatches = Regex("\\b\\d{2,16}\\b").findAll(correctedLine)
            for (match in correctedMatches) {
                if (match.value !in candidateNumbers) {
                    candidateNumbers.add(match.value)
                }
            }

            // Also check student names if no digits found (e.g. "ROLL NO: 101", extract after colon)
            if (trimmed.contains("ROLL", ignoreCase = true) || trimmed.contains("NO", ignoreCase = true)) {
                val afterColon = trimmed.substringAfter(":").trim()
                val idMatch = Regex("[A-Za-z0-9_-]{2,15}").find(afterColon)
                if (idMatch != null) {
                    candidateNumbers.add(idMatch.value)
                }
            }
        }

        if (candidateNumbers.isNotEmpty()) {
            // Return first clean candidate
            return candidateNumbers.first()
        }

        // Fallback: If line contains alphanumeric name/word
        val nameCandidate = lines.map { it.trim() }
            .firstOrNull { it.length in 3..25 && !it.contains("STUDENT", ignoreCase = true) && !it.contains("PASSPORT", ignoreCase = true) }

        return nameCandidate
    }

    suspend fun detectAllPhotosOnSheet(
        source: Bitmap
    ): List<DetectedPhotoItem> = withContext(Dispatchers.Default) {
        val srcW = source.width
        val srcH = source.height
        val image = InputImage.fromBitmap(source, 0)

        val visionText: Text? = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text -> continuation.resume(text) }
                .addOnFailureListener {
                    it.printStackTrace()
                    continuation.resume(null)
                }
        }

        val items = mutableListOf<DetectedPhotoItem>()
        val lines = visionText?.textBlocks?.flatMap { it.lines } ?: emptyList()

        for (line in lines) {
            val num = extractBestNumber(line.text) ?: continue
            val b = line.boundingBox ?: continue

            val labelW = b.width().toFloat()
            val labelH = b.height().toFloat()
            val cardW = (labelW * 1.35f).coerceIn(srcW * 0.12f, srcW * 0.40f)
            val cardH = cardW / 0.7778f
            val cx = b.centerX().toFloat()
            val cardBottom = (b.bottom.toFloat() + 15f).coerceAtMost(srcH.toFloat())
            val cardTop = (cardBottom - cardH).coerceAtLeast(0f)
            val cardLeft = (cx - cardW / 2f).coerceIn(0f, (srcW - cardW).coerceAtLeast(0f))
            val cardRight = (cardLeft + cardW).coerceAtMost(srcW.toFloat())

            val normL = (cardLeft / srcW).coerceIn(0f, 1f)
            val normT = (cardTop / srcH).coerceIn(0f, 1f)
            val normR = (cardRight / srcW).coerceIn(normL + 0.05f, 1f)
            val normB = (cardBottom / srcH).coerceIn(normT + 0.05f, 1f)

            items.add(DetectedPhotoItem(CropRect(normL, normT, normR, normB), num))
        }

        items.sortedWith { i1, i2 ->
            val rowThreshold = 0.08f
            if (kotlin.math.abs(i1.cropRect.centerY - i2.cropRect.centerY) < rowThreshold) {
                i1.cropRect.left.compareTo(i2.cropRect.left)
            } else {
                i1.cropRect.top.compareTo(i2.cropRect.top)
            }
        }
    }
}
