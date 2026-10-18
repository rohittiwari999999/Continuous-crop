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
        isFlipped: Boolean = false,
        cropMode: com.example.data.model.PassportCropMode = com.example.data.model.PassportCropMode.PHOTO_ONLY
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
            val imageAspect = srcW.toFloat() / srcH.toFloat()
            val normW = if (cropMode == com.example.data.model.PassportCropMode.WITH_SIGNATURE) 0.65f else 0.60f
            val normH = if (cropMode == com.example.data.model.PassportCropMode.WITH_SIGNATURE) 0.85f else (normW * imageAspect / PASSPORT_ASPECT_RATIO).coerceAtMost(0.85f)
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

            val photoTopPx: Float
            val photoBottomPx: Float
            val photoWidthPx: Float

            if (cropMode == com.example.data.model.PassportCropMode.WITH_SIGNATURE) {
                // PHOTO + SIGNATURE BELOW:
                // Generous coverage satisfying user's exact requirements:
                // 1. Head pura cover ho: generous headroom for hair and crown
                // 2. Solder pura cover ho + thoda sa daye-baye margin: wide enough to cover both shoulders, torso, and wide signature
                // 3. Whole signature cover ho: extends comfortably down past chest and signature box
                photoTopPx = (box.top.toFloat() - (faceH * 0.40f)).coerceAtLeast(0f)
                photoBottomPx = (box.bottom.toFloat() + (faceH * 2.25f)).coerceAtMost(srcH.toFloat())
                val targetW = maxOf(faceW * 2.50f, faceH * 1.85f)
                photoWidthPx = targetW.coerceAtMost(srcW.toFloat())
            } else {
                // STANDARD PASSPORT (PHOTO ONLY):
                // Generous passport framing with full hair, chest, shoulders & surrounding scene
                photoTopPx = (box.top.toFloat() - (faceH * 0.42f)).coerceAtLeast(0f)
                photoBottomPx = (box.bottom.toFloat() + (faceH * 1.18f)).coerceAtMost(srcH.toFloat())
                val h = photoBottomPx - photoTopPx
                photoWidthPx = (h * PASSPORT_ASPECT_RATIO).coerceAtMost(srcW.toFloat())
            }

            var photoLeftPx = faceCenterX - (photoWidthPx / 2f)
            var photoRightPx = photoLeftPx + photoWidthPx

            // Ensure horizontal bounds are strictly within bitmap
            if (photoLeftPx < 0f) {
                photoLeftPx = 0f
                photoRightPx = photoWidthPx.coerceAtMost(srcW.toFloat())
            } else if (photoRightPx > srcW) {
                photoRightPx = srcW.toFloat()
                photoLeftPx = (srcW - photoWidthPx).coerceAtLeast(0f)
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
