package com.example.ml

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.model.DetectedFeature
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object MlKitCropHelper {

    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFeature> = withContext(Dispatchers.Default) {
        try {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .build()

            val detector = FaceDetection.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()

            faces.mapIndexed { index, face ->
                val box = face.boundingBox
                // Add padding around face
                val padW = box.width() * 0.25f
                val padH = box.height() * 0.35f
                val left = (box.left - padW) / bitmap.width
                val top = (box.top - padH) / bitmap.height
                val right = (box.right + padW) / bitmap.width
                val bottom = (box.bottom + padH) / bitmap.height

                DetectedFeature(
                    id = UUID.randomUUID().toString(),
                    label = "Face #${index + 1}",
                    normalizedBounds = RectF(
                        left.coerceIn(0f, 1f),
                        top.coerceIn(0f, 1f),
                        right.coerceIn(0f, 1f),
                        bottom.coerceIn(0f, 1f)
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun detectText(bitmap: Bitmap): List<DetectedFeature> = withContext(Dispatchers.Default) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()

            visionText.textBlocks.mapIndexedNotNull { index, block ->
                val box = block.boundingBox ?: return@mapIndexedNotNull null
                val pad = 16f
                val left = (box.left - pad) / bitmap.width
                val top = (box.top - pad) / bitmap.height
                val right = (box.right + pad) / bitmap.width
                val bottom = (box.bottom + pad) / bitmap.height

                DetectedFeature(
                    id = UUID.randomUUID().toString(),
                    label = "Text: ${block.text.take(15)}...",
                    normalizedBounds = RectF(
                        left.coerceIn(0f, 1f),
                        top.coerceIn(0f, 1f),
                        right.coerceIn(0f, 1f),
                        bottom.coerceIn(0f, 1f)
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
