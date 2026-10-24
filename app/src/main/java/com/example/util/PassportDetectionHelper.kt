package com.example.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.example.data.model.CropRect
import com.example.data.model.PassportCropMode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object PassportDetectionHelper {

    const val PASSPORT_ASPECT_RATIO = 35f / 45f // 0.7778 (3.5cm x 4.5cm standard passport ratio)

    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(0.015f) // Support small faces on multi-photo sheets
            .build()
        FaceDetection.getClient(options)
    }

    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    data class PassportFaceItem(
        val cropRect: CropRect,
        val faceBoundingBox: Rect
    )

    data class DetectionResult(
        val cropRect: CropRect,
        val isFaceDetected: Boolean,
        val confidenceNote: String
    )

    suspend fun detectPassportCrop(
        source: Bitmap,
        rotationDegrees: Int = 0,
        isFlipped: Boolean = false,
        cropMode: PassportCropMode = PassportCropMode.PHOTO_ONLY
    ): DetectionResult = withContext(Dispatchers.Default) {
        val orientedBitmap = if (rotationDegrees != 0 || isFlipped) {
            val matrix = Matrix()
            if (isFlipped) matrix.postScale(-1f, 1f)
            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val allFaces = detectAllPassportFaces(orientedBitmap, cropMode = cropMode)

        if (orientedBitmap != source) {
            orientedBitmap.recycle()
        }

        if (allFaces.isNotEmpty()) {
            val primaryFace = allFaces.first()
            val note = if (allFaces.size > 1) {
                "Detected ${allFaces.size} passport photos on sheet"
            } else {
                "Face detected with standard passport framing (3.5 × 4.5 cm)"
            }
            DetectionResult(
                cropRect = primaryFace.cropRect,
                isFaceDetected = true,
                confidenceNote = note
            )
        } else {
            // Default center passport box
            val defaultW = 0.65f
            val imageAspect = source.width.toFloat() / source.height.toFloat()
            var defaultH = (defaultW * imageAspect) / PASSPORT_ASPECT_RATIO
            if (defaultH > 0.92f) {
                defaultH = 0.92f
                val adjustedW = (defaultH * PASSPORT_ASPECT_RATIO) / imageAspect
                defaultH = 0.92f
            }
            val left = ((1f - defaultW) / 2f).coerceIn(0f, 0.9f)
            val top = 0.05f.coerceIn(0f, (1f - defaultH).coerceAtLeast(0f))
            val right = (left + defaultW).coerceIn(0.1f, 1f)
            val bottom = (top + defaultH).coerceIn(0.1f, 1f)

            DetectionResult(
                cropRect = CropRect(left, top, right, bottom),
                isFaceDetected = false,
                confidenceNote = "Standard passport framing applied (3.5 × 4.5 cm)"
            )
        }
    }

    suspend fun detectAllPassportFaces(
        source: Bitmap,
        rotationDegrees: Int = 0,
        isFlipped: Boolean = false,
        cropMode: PassportCropMode = PassportCropMode.PHOTO_ONLY
    ): List<PassportFaceItem> = withContext(Dispatchers.Default) {
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
        val image = InputImage.fromBitmap(oriented, 0)

        // Run face detection and text recognition concurrently on the sheet image
        val (mlFaces, visionText) = coroutineScope {
            val facesDeferred = async {
                suspendCancellableCoroutine<List<Face>> { cont ->
                    faceDetector.process(image)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { error ->
                            error.printStackTrace()
                            cont.resume(emptyList())
                        }
                }
            }

            val textDeferred = async {
                suspendCancellableCoroutine<Text?> { cont ->
                    textRecognizer.process(image)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener {
                            cont.resume(null)
                        }
                }
            }

            facesDeferred.await() to textDeferred.await()
        }

        if (oriented != source) {
            oriented.recycle()
        }

        val allLines = visionText?.textBlocks?.flatMap { it.lines } ?: emptyList()

        if (mlFaces.isEmpty()) {
            val imageAspect = srcW.toFloat() / srcH.toFloat()
            val normW = if (cropMode == PassportCropMode.WITH_SIGNATURE) 0.65f else 0.60f
            val normH = if (cropMode == PassportCropMode.WITH_SIGNATURE) 0.85f else (normW * imageAspect / PASSPORT_ASPECT_RATIO).coerceAtMost(0.85f)
            val left = ((1f - normW) / 2f).coerceIn(0f, 0.95f)
            val top = 0.06f.coerceIn(0f, (1f - normH).coerceAtLeast(0f))
            val fallbackRect = CropRect(left, top, (left + normW).coerceAtMost(1f), (top + normH).coerceAtMost(1f))
            return@withContext listOf(
                PassportFaceItem(
                    cropRect = fallbackRect,
                    faceBoundingBox = Rect((left * srcW).toInt(), (top * srcH).toInt(), ((left + normW) * srcW).toInt(), ((top + normH) * srcH).toInt())
                )
            )
        }

        // Sort faces in natural reading order: top-to-bottom, left-to-right
        val sortedFaces = mlFaces.sortedWith { f1, f2 ->
            val b1 = f1.boundingBox
            val b2 = f2.boundingBox
            val rowThreshold = (b1.height() + b2.height()) / 4
            if (kotlin.math.abs(b1.centerY() - b2.centerY()) < rowThreshold) {
                b1.left.compareTo(b2.left)
            } else {
                b1.top.compareTo(b2.top)
            }
        }

        sortedFaces.map { face ->
            val box = face.boundingBox
            val faceH = box.height().toFloat().coerceAtLeast(10f)
            val faceW = box.width().toFloat().coerceAtLeast(10f)
            val faceCenterX = box.centerX().toFloat()

            // Find if there is another face directly below in the same column
            val faceBelow = mlFaces.filter { other ->
                other != face &&
                other.boundingBox.top > box.bottom &&
                kotlin.math.abs(other.boundingBox.centerX() - faceCenterX) < faceW * 1.6f
            }.minByOrNull { it.boundingBox.top }

            // Find if there is another face directly above in the same column
            val faceAbove = mlFaces.filter { other ->
                other != face &&
                other.boundingBox.bottom < box.top &&
                kotlin.math.abs(other.boundingBox.centerX() - faceCenterX) < faceW * 1.6f
            }.maxByOrNull { it.boundingBox.bottom }

            // Find handwritten roll number line directly above this head
            val numberLineAbove = allLines.filter { line ->
                val b = line.boundingBox ?: return@filter false
                b.bottom <= box.top + (faceH * 0.08f) &&
                b.top >= box.top - (faceH * 0.95f) &&
                kotlin.math.abs(b.centerX() - faceCenterX) < faceW * 1.6f
            }.maxByOrNull { it.boundingBox?.bottom ?: 0 }

            // Find signature lines directly below this chin
            val signatureLines = allLines.filter { line ->
                val b = line.boundingBox ?: return@filter false
                val belowChin = b.top >= box.bottom - (faceH * 0.10f)
                val withinSigReach = b.top <= box.bottom + (faceH * 1.65f)
                val alignedWithCol = kotlin.math.abs(b.centerX() - faceCenterX) < faceW * 2.5f
                val beforeFaceBelow = if (faceBelow != null) b.bottom <= faceBelow.boundingBox.top - (faceH * 0.25f) else true
                belowChin && withinSigReach && alignedWithCol && beforeFaceBelow
            }

            // Find if the student below has a roll number above their head
            val numberLineBelowStudent = if (faceBelow != null) {
                allLines.filter { line ->
                    val b = line.boundingBox ?: return@filter false
                    b.bottom <= faceBelow.boundingBox.top + (faceH * 0.08f) &&
                    b.top >= faceBelow.boundingBox.top - (faceH * 0.95f) &&
                    kotlin.math.abs(b.centerX() - faceBelow.boundingBox.centerX()) < faceW * 1.6f
                }.minByOrNull { it.boundingBox?.top ?: Int.MAX_VALUE }
            } else null

            val photoTopPx: Float
            val photoBottomPx: Float
            var photoLeftPx: Float
            var photoRightPx: Float

            if (cropMode == PassportCropMode.WITH_SIGNATURE) {
                // PHOTO + SIGNATURE BELOW:
                // 1. TOP: Ensure head & hair are 100% covered without cutting hair,
                // while strictly stopping BELOW the roll number written above the photo.
                var topPx = (box.top.toFloat() - (faceH * 0.40f)).coerceAtLeast(0f)
                if (numberLineAbove != null) {
                    val numberBottom = numberLineAbove.boundingBox!!.bottom.toFloat()
                    topPx = maxOf(topPx, numberBottom + 4f)
                }
                if (faceAbove != null) {
                    val floorAfterPrev = faceAbove.boundingBox.bottom.toFloat() + (faceH * 1.55f)
                    if (floorAfterPrev < box.top) {
                        topPx = maxOf(topPx, floorAfterPrev)
                    }
                }
                photoTopPx = topPx

                // 2. BOTTOM: Captures torso + full signature.
                // Must strictly stop BEFORE the next student's roll number or photo below.
                var bottomPx = box.bottom.toFloat() + (faceH * 1.48f)
                if (signatureLines.isNotEmpty()) {
                    val sigBottom = signatureLines.maxOf { it.boundingBox!!.bottom }.toFloat()
                    bottomPx = maxOf(bottomPx, sigBottom + 12f)
                }
                // Stop strictly before next student's roll number
                if (numberLineBelowStudent != null) {
                    val ceilingBeforeNextNum = numberLineBelowStudent.boundingBox!!.top.toFloat() - 4f
                    if (ceilingBeforeNextNum > box.bottom + (faceH * 1.05f)) {
                        bottomPx = minOf(bottomPx, ceilingBeforeNextNum)
                    }
                }
                if (faceBelow != null) {
                    val ceilingBeforeFaceBelow = faceBelow.boundingBox.top.toFloat() - (faceH * 0.65f)
                    if (ceilingBeforeFaceBelow > box.bottom + (faceH * 1.05f)) {
                        bottomPx = minOf(bottomPx, ceilingBeforeFaceBelow)
                    }
                }
                photoBottomPx = bottomPx.coerceAtMost(srcH.toFloat())

                // 3. HORIZONTAL (LEFT & RIGHT):
                // Prioritize covering the ENTIRE signature from left to right, even if shifted
                // or wide, along with full shoulders and photo width.
                val baseWidth = maxOf(faceW * 2.45f, faceH * 1.65f)
                var leftPx = faceCenterX - (baseWidth / 2f)
                var rightPx = faceCenterX + (baseWidth / 2f)

                if (signatureLines.isNotEmpty()) {
                    val sigLeft = signatureLines.minOf { it.boundingBox!!.left }.toFloat()
                    val sigRight = signatureLines.maxOf { it.boundingBox!!.right }.toFloat()
                    val sigPadding = 16f
                    // Expand horizontally to cover complete signature start and end
                    leftPx = minOf(leftPx, sigLeft - sigPadding)
                    rightPx = maxOf(rightPx, sigRight + sigPadding)
                }

                // Protect against overlapping neighboring student faces in adjacent columns
                val faceToLeft = mlFaces.filter {
                    it != face &&
                    it.boundingBox.right < box.left &&
                    kotlin.math.abs(it.boundingBox.centerY() - box.centerY()) < faceH * 0.85f
                }.maxByOrNull { it.boundingBox.right }

                val faceToRight = mlFaces.filter {
                    it != face &&
                    it.boundingBox.left > box.right &&
                    kotlin.math.abs(it.boundingBox.centerY() - box.centerY()) < faceH * 0.85f
                }.minByOrNull { it.boundingBox.left }

                if (faceToLeft != null) {
                    val leftLimit = faceToLeft.boundingBox.right.toFloat() + (faceW * 0.30f)
                    leftPx = maxOf(leftPx, leftLimit)
                }
                if (faceToRight != null) {
                    val rightLimit = faceToRight.boundingBox.left.toFloat() - (faceW * 0.30f)
                    rightPx = minOf(rightPx, rightLimit)
                }

                photoLeftPx = leftPx
                photoRightPx = rightPx
            } else {
                // STANDARD PASSPORT (PHOTO ONLY):
                // 1. TOP: Full hair coverage without top number
                var topPx = (box.top.toFloat() - (faceH * 0.40f)).coerceAtLeast(0f)
                if (numberLineAbove != null) {
                    val numberBottom = numberLineAbove.boundingBox!!.bottom.toFloat()
                    topPx = maxOf(topPx, numberBottom + 4f)
                }
                if (faceAbove != null) {
                    val floorAfterPrev = faceAbove.boundingBox.bottom.toFloat() + (faceH * 1.25f)
                    if (floorAfterPrev < box.top) {
                        topPx = maxOf(topPx, floorAfterPrev)
                    }
                }
                photoTopPx = topPx

                // 2. BOTTOM: Clean passport framing (head, neck, collar/chest).
                // MUST NOT capture signature below! Shorten bottom and stop strictly above any signature.
                var bottomPx = box.bottom.toFloat() + (faceH * 0.74f)
                if (signatureLines.isNotEmpty()) {
                    val sigTop = signatureLines.minOf { it.boundingBox!!.top }.toFloat()
                    // Stop cleanly at or above signature with safe margin
                    bottomPx = minOf(bottomPx, sigTop - 6f)
                }
                if (faceBelow != null) {
                    val ceilingBeforeNext = faceBelow.boundingBox.top.toFloat() - (faceH * 0.50f)
                    if (ceilingBeforeNext > box.bottom + (faceH * 0.60f)) {
                        bottomPx = minOf(bottomPx, ceilingBeforeNext)
                    }
                }
                val minHeight = faceH * 1.25f
                if (bottomPx < photoTopPx + minHeight) {
                    bottomPx = (photoTopPx + minHeight).coerceAtMost(srcH.toFloat())
                }
                photoBottomPx = bottomPx.coerceAtMost(srcH.toFloat())

                // 3. WIDTH: Maintain standard passport aspect ratio (3.5 × 4.5 cm)
                val h = photoBottomPx - photoTopPx
                val photoWidthPx = (h * PASSPORT_ASPECT_RATIO).coerceAtMost(srcW.toFloat())
                photoLeftPx = faceCenterX - (photoWidthPx / 2f)
                photoRightPx = photoLeftPx + photoWidthPx
            }

            // Ensure horizontal bounds are strictly within bitmap
            if (photoLeftPx < 0f) {
                val shift = -photoLeftPx
                photoLeftPx = 0f
                photoRightPx = (photoRightPx + shift).coerceAtMost(srcW.toFloat())
            }
            if (photoRightPx > srcW.toFloat()) {
                val shift = photoRightPx - srcW.toFloat()
                photoRightPx = srcW.toFloat()
                photoLeftPx = (photoLeftPx - shift).coerceAtLeast(0f)
            }

            val normLeft = (photoLeftPx / srcW).coerceIn(0f, 0.98f)
            val normTop = (photoTopPx / srcH).coerceIn(0f, 0.98f)
            val normRight = (photoRightPx / srcW).coerceIn(normLeft + 0.02f, 1f)
            val normBottom = (photoBottomPx / srcH).coerceIn(normTop + 0.02f, 1f)

            PassportFaceItem(
                cropRect = CropRect(normLeft, normTop, normRight, normBottom),
                faceBoundingBox = box
            )
        }
    }
}
