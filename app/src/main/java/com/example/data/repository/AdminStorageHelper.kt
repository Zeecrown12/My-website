package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

object AdminStorageHelper {

    private const val MAX_UPLOAD_BYTES = 4_800_000

    suspend fun uploadImageUri(
        context: Context,
        uri: Uri,
        folderPath: String,
        fixedFileName: String? = null,
        maxDimensionPx: Int = 1440,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<String> {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == "http" || scheme == "https") {
            return Result.success(uri.toString())
        }
        return try {
            onProgress?.invoke(0.05f)
            val jpegBytes = withContext(Dispatchers.IO) {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Unable to open selected image from device.")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    throw IllegalArgumentException("Selected file is not a supported image.")
                }

                var sampleSize = 1
                while (bounds.outWidth / sampleSize > maxDimensionPx || bounds.outHeight / sampleSize > maxDimensionPx) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                    ?: throw IllegalStateException("Unable to decode image.")

                var quality = 86
                var outBytes: ByteArray
                do {
                    val baos = ByteArrayOutputStream()
                    decoded.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    outBytes = baos.toByteArray()
                    quality -= 10
                } while (outBytes.size > MAX_UPLOAD_BYTES && quality >= 46)
                decoded.recycle()

                if (outBytes.size > MAX_UPLOAD_BYTES) {
                    throw IllegalStateException("Compressed image still exceeds 5MB upload limit.")
                }
                outBytes
            }

            onProgress?.invoke(0.2f)
            val fileName = fixedFileName ?: "${UUID.randomUUID().toString().take(12)}.jpg"
            val cleanFolder = folderPath.trim('/')
            val storageRef = Firebase.storage.reference.child("$cleanFolder/$fileName")
            val uploadTask = storageRef.putBytes(jpegBytes)
            uploadTask.addOnProgressListener { snap ->
                val total = snap.totalByteCount.coerceAtLeast(1L)
                val pct = 0.2f + 0.75f * (snap.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                onProgress?.invoke(pct)
            }
            uploadTask.await()
            val url = storageRef.downloadUrl.await().toString()
            onProgress?.invoke(1f)
            Result.success(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            val friendly = when (e.errorCode) {
                StorageException.ERROR_BUCKET_NOT_FOUND, StorageException.ERROR_PROJECT_NOT_FOUND ->
                    "Firebase Storage bucket is not provisioned yet. Enable Storage in Firebase Console to host cloud images."
                StorageException.ERROR_NOT_AUTHORIZED, StorageException.ERROR_NOT_AUTHENTICATED ->
                    "Your admin session lacks Storage upload permission for this folder."
                else -> e.localizedMessage ?: "Firebase Storage upload failed."
            }
            Result.failure(IllegalStateException(friendly, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
