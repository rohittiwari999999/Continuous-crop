package com.example.data.model

enum class PassportCropMode(
    val label: String,
    val description: String
) {
    PHOTO_ONLY(
        label = "Passport (Photo Only)",
        description = "Generous passport photo crop with full hair, chest & shoulders"
    ),
    WITH_SIGNATURE(
        label = "Passport with Signature",
        description = "Combined crop of student photo and signature below photo"
    )
}
