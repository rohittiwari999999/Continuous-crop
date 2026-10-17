package com.example.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.example.data.model.CropRect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
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
        isFlipped: Boolean = false
    ): DetectionResult = withContext(Dispatchers.Default) {
        val orientedBitmap = if (rotationDegrees != 0 || isFlipped) {
            val matrix = Matrix()
            if (isFlipped) matrix.postScale(-1f, 1f)
            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val allFaces = detectAllPassportFaces(orientedBitmap)

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
        isFlipped: Boolean = false
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

        val mlFaces: List<Face> = suspendCancellableCoroutine { continuation ->
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    continuation.resume(faces)
                }
                .addOnFailureListener { error ->
                    error.printStackTrace()
                    continuation.resume(emptyList())
                }
        }

        if (oriented != source) {
            oriented.recycle()
        }

        if (mlFaces.isEmpty()) {
            // If ML Kit doesn't detect any face, create ONE real centered passport box (3.5x4.5 ratio)
            // NEVER generate fake 9-grid boxes
            val imageAspect = srcW.toFloat() / srcH.toFloat()
            var normW = 0.65f.coerceAtMost(0.92f)
            var normH = (normW * imageAspect) / PASSPORT_ASPECT_RATIO
            if (normH > 0.88f) {
                normH = 0.88f
                normW = (normH * PASSPORT_ASPECT_RATIO) / imageAspect
            }
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
            val faceCenterX = box.centerX().toFloat()

            // Standard passport photo framing:
            // Full head (crown of hair to chin) should be ~70-75% of the photo height.
            // ML Kit bounding box covers forehead to chin.
            // Total photo height should be ~1.75x of face box height to capture hair, crown, and collar/shoulders.
            var photoHeightPx = faceH * 1.75f
            var photoWidthPx = photoHeightPx * PASSPORT_ASPECT_RATIO

            // Headroom above face box (hair & top margin): ~20% of photo height
            var photoTopPx = box.top.toFloat() - (photoHeightPx * 0.20f)
            var photoBottomPx = photoTopPx + photoHeightPx

            // Ensure vertical bounds are within bitmap
            if (photoTopPx < 0f) {
                val shift = -photoTopPx
                photoTopPx = 0f
                photoBottomPx += shift
            }
            if (photoBottomPx > srcH) {
                val overflow = photoBottomPx - srcH
                photoTopPx = (photoTopPx - overflow).coerceAtLeast(0f)
                photoBottomPx = srcH.toFloat()
                // Recompute height and width to maintain strict passport ratio
                photoHeightPx = photoBottomPx - photoTopPx
                photoWidthPx = photoHeightPx * PASSPORT_ASPECT_RATIO
            }

            var photoLeftPx = faceCenterX - (photoWidthPx / 2f)
            var photoRightPx = photoLeftPx + photoWidthPx

            // Ensure horizontal bounds are within bitmap
            if (photoLeftPx < 0f) {
                val shift = -photoLeftPx
                photoLeftPx = 0f
                photoRightPx += shift
            }
            if (photoRightPx > srcW) {
                val overflow = photoRightPx - srcW
                photoLeftPx = (photoLeftPx - overflow).coerceAtLeast(0f)
                photoRightPx = srcW.toFloat()
                // Recompute height and width to maintain strict passport ratio
                photoWidthPx = photoRightPx - photoLeftPx
                photoHeightPx = photoWidthPx / PASSPORT_ASPECT_RATIO
                photoBottomPx = (photoTopPx + photoHeightPx).coerceAtMost(srcH.toFloat())
            }

            val normLeft = (photoLeftPx / srcW).coerceIn(0f, 0.95f)
            val normTop = (photoTopPx / srcH).coerceIn(0f, 0.95f)
            val normRight = (photoRightPx / srcW).coerceIn(normLeft + 0.05f, 1f)
            val normBottom = (photoBottomPx / srcH).coerceIn(normTop + 0.05f, 1f)

            PassportFaceItem(
                cropRect = CropRect(normLeft, normTop, normRight, normBottom),
                faceBoundingBox = box
            )
        }
    }
}
