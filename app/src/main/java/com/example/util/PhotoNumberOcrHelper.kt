package com.example.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
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

    /**
     * Batch-detects handwriting/printed numbers written ABOVE each photo on a sheet.
     * Uses a fast two-phase approach:
     * 1. Full-image text recognition to find all lines and their exact spatial bounding boxes.
     * 2. For each crop, finds the closest text line located vertically above the head/photo.
     * 3. Falls back to focused upscaled regional scan if no full-image line matched.
     */
    suspend fun detectNumbersForCrops(
        source: Bitmap,
        cropRects: List<CropRect>,
        rotationDegrees: Int = 0,
        isFlipped: Boolean = false
    ): List<String?> = withContext(Dispatchers.Default) {
        if (cropRects.isEmpty()) return@withContext emptyList()

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

        // Phase 1: Full image OCR
        val fullImage = InputImage.fromBitmap(oriented, 0)
        val visionText: Text? = suspendCancellableCoroutine { continuation ->
            recognizer.process(fullImage)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
        }

        val allLines = visionText?.textBlocks?.flatMap { it.lines } ?: emptyList()

        val results = cropRects.mapIndexed { index, cropRect ->
            val pLeft = cropRect.left * srcW
            val pTop = cropRect.top * srcH
            val pRight = cropRect.right * srcW
            val pBottom = cropRect.bottom * srcH
            val pWidth = pRight - pLeft
            val pHeight = pBottom - pTop

            // Candidate lines above this photo:
            // - Bottom edge is near or above photo top
            // - Top edge is within reasonable distance above (up to 70% of photo height or 300px)
            // - Horizontal center falls within or very near the photo column
            val candidateLines = allLines.filter { line ->
                val box = line.boundingBox ?: return@filter false
                val isAbovePhoto = box.bottom <= (pTop + pHeight * 0.12f) &&
                        box.top >= (pTop - (pHeight * 0.70f).coerceAtLeast(srcH * 0.10f))
                val isHorizontallyAligned = box.centerX() >= (pLeft - pWidth * 0.35f) &&
                        box.centerX() <= (pRight + pWidth * 0.35f)
                isAbovePhoto && isHorizontallyAligned
            }

            // Extract number from lines, sorting by vertical proximity to photo top
            var detected: String? = null
            val sortedCandidates = candidateLines.sortedByDescending { it.boundingBox?.bottom ?: 0 }
            for (line in sortedCandidates) {
                val num = extractBestNumber(line.text)
                if (!num.isNullOrBlank()) {
                    detected = num
                    break
                }
            }

            // Phase 2: If full-image didn't find it, do a focused regional scan directly above
            if (detected.isNullOrBlank()) {
                val aboveLeft = (cropRect.left - cropRect.width * 0.25f).coerceAtLeast(0f)
                val aboveRight = (cropRect.right + cropRect.width * 0.25f).coerceAtMost(1f)
                val aboveTop = (cropRect.top - (cropRect.height * 0.45f).coerceAtLeast(0.12f)).coerceAtLeast(0f)
                val aboveBottom = (cropRect.top + 0.05f).coerceIn(0f, 1f)

                detected = scanRegionForNumber(oriented, aboveLeft, aboveTop, aboveRight, aboveBottom, srcW, srcH)
            }

            detected
        }

        if (oriented != source) {
            oriented.recycle()
        }

        results
    }

    /**
     * Detects number written directly ABOVE a single crop rectangle.
     */
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

        // 1. PRIMARY: Check directly ABOVE the photo (handwritten roll number / serial number above head)
        val aboveLeft = (cropRect.left - cropRect.width * 0.25f).coerceAtLeast(0f)
        val aboveRight = (cropRect.right + cropRect.width * 0.25f).coerceAtMost(1f)
        val aboveTop = (cropRect.top - (cropRect.height * 0.45f).coerceAtLeast(0.12f)).coerceAtLeast(0f)
        val aboveBottom = (cropRect.top + 0.05f).coerceIn(0f, 1f)

        var result = scanRegionForNumber(oriented, aboveLeft, aboveTop, aboveRight, aboveBottom, srcW, srcH)

        // 2. Wider check ABOVE the photo if not found
        if (result.isNullOrBlank()) {
            val widerTop = (cropRect.top - (cropRect.height * 0.75f).coerceAtLeast(0.20f)).coerceAtLeast(0f)
            val widerBottom = (cropRect.top + 0.08f).coerceIn(0f, 1f)
            result = scanRegionForNumber(oriented, aboveLeft, widerTop, aboveRight, widerBottom, srcW, srcH)
        }

        // 3. Last fallback: Check below the photo
        if (result.isNullOrBlank()) {
            val belowLeft = (cropRect.left - cropRect.width * 0.15f).coerceAtLeast(0f)
            val belowRight = (cropRect.right + cropRect.width * 0.15f).coerceAtMost(1f)
            val belowTop = (cropRect.bottom - cropRect.height * 0.08f).coerceIn(0f, 1f)
            val belowBottom = (cropRect.bottom + cropRect.height * 0.35f).coerceAtMost(1f)
            result = scanRegionForNumber(oriented, belowLeft, belowTop, belowRight, belowBottom, srcW, srcH)
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

        val cropped = try {
            Bitmap.createBitmap(bitmap, pxLeft, pxTop, pxWidth, pxHeight)
        } catch (_: Exception) {
            return null
        }

        // Upscale region if small so handwriting strokes are easily recognized by ML Kit
        val regionBitmap = if (pxHeight < 140 || pxWidth < 200) {
            val scale = (180f / pxHeight.toFloat().coerceAtLeast(1f)).coerceIn(1.5f, 3.0f)
            val targetW = (pxWidth * scale).toInt().coerceAtLeast(10)
            val targetH = (pxHeight * scale).toInt().coerceAtLeast(10)
            try {
                Bitmap.createScaledBitmap(cropped, targetW, targetH, true).also {
                    if (it != cropped) cropped.recycle()
                }
            } catch (_: Exception) {
                cropped
            }
        } else {
            cropped
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

    /**
     * Extracts clean handwriting / printed roll or serial number from OCR text.
     * Handles:
     * - "101", "102", "01", "5", "1234"
     * - Spaced handwriting: "1 0 1", "0 5"
     * - Prefix labels: "Roll No: 101", "Sl. No 12", "No. 05", "#45", "R-101"
     * - OCR handwriting confusions: 'O'/'o' -> 0, 'I'/'l' -> 1
     */
    fun extractBestNumber(rawText: String): String? {
        if (rawText.isBlank()) return null

        val lines = rawText.lines()
        val candidateNumbers = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            // 1. Remove common form prefixes like "Roll No", "Sl No", "Sr", "No", "#", etc.
            val cleanLine = trimmed
                .replace(Regex("(?i)\\b(roll|sl|sr|s\\.no|sno|no|num|number)\\b[:.\\s-]*"), " ")
                .replace(Regex("[#:]"), " ")
                .trim()

            // 2. Collapse spaced digits from handwriting (e.g. "1 0 1" -> "101", "4 5" -> "45")
            val collapsedDigits = cleanLine.replace(Regex("(?<=\\d)\\s+(?=\\d)"), "")

            // 3. Search for digit sequences (1 to 16 digits)
            val digitMatches = Regex("\\b\\d{1,16}\\b").findAll(collapsedDigits)
            for (match in digitMatches) {
                candidateNumbers.add(match.value)
            }

            // 4. Correct common OCR handwriting confusions:
            // e.g. "O5" -> "05", "IO1" -> "101", "l01" -> "101"
            val correctedLine = cleanLine
                .replace(Regex("(?i)\\b[oO](\\d+)"), "0$1")
                .replace(Regex("(\\d+)[oO]\\b"), "$10")
                .replace(Regex("(?i)\\b[lI](\\d+)"), "1$1")
                .replace(Regex("(\\d+)[lI]\\b"), "$11")
                .replace(Regex("(?<=\\d)\\s+(?=\\d)"), "")

            val correctedMatches = Regex("\\b\\d{1,16}\\b").findAll(correctedLine)
            for (match in correctedMatches) {
                if (match.value !in candidateNumbers) {
                    candidateNumbers.add(match.value)
                }
            }

            // 5. Check alphanumeric roll number like "A101", "101A", "R-12"
            val alphaNumMatches = Regex("\\b[A-Za-z]?\\d{1,10}[A-Za-z]?\\b").findAll(collapsedDigits)
            for (m in alphaNumMatches) {
                if (m.value.any { it.isDigit() } && m.value !in candidateNumbers) {
                    candidateNumbers.add(m.value)
                }
            }
        }

        // Return best candidate: prefer multi-digit numbers first, then single digit
        val multiDigit = candidateNumbers.filter { it.length >= 2 }
        if (multiDigit.isNotEmpty()) {
            return multiDigit.first()
        }
        if (candidateNumbers.isNotEmpty()) {
            return candidateNumbers.first()
        }

        return null
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
