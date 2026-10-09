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
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.05f) // Support small faces on sheets/grids
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
        source: Bitmap
    ): List<PassportFaceItem> = withContext(Dispatchers.Default) {
        val srcW = source.width
        val srcH = source.height
        val image = InputImage.fromBitmap(source, 0)

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

        if (mlFaces.isEmpty()) {
            return@withContext emptyList()
        }

        // Sort faces reading-order: top-to-bottom, left-to-right (grid row tolerance)
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

            // Head height estimation:
            // Standard passport photo rules: Head (crown to chin) should be 70% - 80% of total height.
            // Face box from ML Kit covers roughly eyebrows/forehead to chin.
            val faceH = box.height().toFloat()
            val faceW = box.width().toFloat()
            val faceCenterX = box.centerX().toFloat()
            val faceCenterY = box.centerY().toFloat()

            // Photo height should be approx 1.8x - 2.0x of the detected face box height
            val photoHeightPx = faceH * 1.95f
            val photoWidthPx = photoHeightPx * PASSPORT_ASPECT_RATIO

            // Headroom above face: ~15% of photo height above top of face box
            val photoTopPx = (box.top.toFloat() - photoHeightPx * 0.18f).coerceAtLeast(0f)
            val photoBottomPx = (photoTopPx + photoHeightPx).coerceAtMost(srcH.toFloat())
            val actualHeightPx = photoBottomPx - photoTopPx
            val actualWidthPx = actualHeightPx * PASSPORT_ASPECT_RATIO

            val photoLeftPx = (faceCenterX - actualWidthPx / 2f).coerceIn(0f, (srcW - actualWidthPx).coerceAtLeast(0f))
            val photoRightPx = (photoLeftPx + actualWidthPx).coerceIn(photoLeftPx + 10f, srcW.toFloat())

            val normLeft = (photoLeftPx / srcW).coerceIn(0f, 1f)
            val normTop = (photoTopPx / srcH).coerceIn(0f, 1f)
            val normRight = (photoRightPx / srcW).coerceIn(normLeft + 0.05f, 1f)
            val normBottom = (photoBottomPx / srcH).coerceIn(normTop + 0.05f, 1f)

            PassportFaceItem(
                cropRect = CropRect(normLeft, normTop, normRight, normBottom),
                faceBoundingBox = box
            )
        }
    }
}
