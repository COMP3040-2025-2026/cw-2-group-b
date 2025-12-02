package com.nottingham.mynottingham.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Repository for compressing images to Base64 Data URI
 *
 * Compresses images directly to Base64 Data URI format for storage in Firebase Realtime Database.
 * No third-party image hosting service required.
 */
class ImageUploadRepository {

    /**
     * Compress image and convert to Base64 Data URI
     * Return format: data:image/jpeg;base64,xxxxx
     * Can be used directly with Glide or stored in database
     */
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        folder: String,
        userId: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Read image from URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure(Exception("Cannot open image"))

                // Decode with inJustDecodeBounds to get dimensions first
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                // Calculate sample size to reduce memory usage
                val maxDimension = MAX_IMAGE_DIMENSION
                options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
                options.inJustDecodeBounds = false

                // Decode the actual bitmap
                val inputStream2 = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure(Exception("Cannot open image"))

                var bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
                inputStream2.close()

                if (bitmap == null) {
                    return@withContext Result.failure(Exception("Failed to decode image"))
                }

                // Scale down if still too large
                bitmap = scaleBitmapIfNeeded(bitmap, maxDimension)

                // Compress to JPEG with adaptive quality
                val outputStream = ByteArrayOutputStream()
                var quality = INITIAL_QUALITY

                do {
                    outputStream.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    val size = outputStream.size()

                    android.util.Log.d("ImageUploadRepository", "Quality: $quality%, Size: ${size / 1024} KB")

                    if (size <= MAX_IMAGE_SIZE_BYTES) {
                        break
                    }
                    quality -= 10
                } while (quality >= MIN_QUALITY)

                val imageBytes = outputStream.toByteArray()
                bitmap.recycle()

                android.util.Log.d("ImageUploadRepository", "Final image size: ${imageBytes.size / 1024} KB")

                // Convert to Base64 Data URI
                val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                val dataUri = "data:image/jpeg;base64,$base64"

                android.util.Log.d("ImageUploadRepository", "Data URI length: ${dataUri.length} chars")

                Result.success(dataUri)
            } catch (e: Exception) {
                android.util.Log.e("ImageUploadRepository", "Error processing image: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Calculate inSampleSize for BitmapFactory
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Scale bitmap if dimensions exceed max
     */
    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val scale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }

        return scaledBitmap
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return Result.success(Unit)
    }

    companion object {
        // Maximum image dimension (width or height)
        private const val MAX_IMAGE_DIMENSION = 800

        // Target maximum file size (150KB, ~200KB after Base64, suitable for database storage)
        private const val MAX_IMAGE_SIZE_BYTES = 150 * 1024

        // Compression quality range
        private const val INITIAL_QUALITY = 80
        private const val MIN_QUALITY = 30

        const val FOLDER_CHAT_IMAGES = "chat_images"
        const val FOLDER_FORUM_IMAGES = "forum_images"
        const val FOLDER_PROFILE_IMAGES = "profile_images"
    }
}
