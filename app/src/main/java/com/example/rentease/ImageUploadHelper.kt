package com.example.rentease

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object ImageUploadHelper {

    private val auth = FirebaseAuth.getInstance()

    fun uploadProfileImage(
        imageUri: Uri,
        context: Context? = null,
        onSuccess: (downloadUrl: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onFailure("Sesi tidak valid")
            return
        }

        if (context == null) {
            onFailure("Context diperlukan untuk memproses gambar")
            return
        }

        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                onFailure("Gagal membaca file gambar")
                return
            }
            val rawBytes = inputStream.readBytes()
            inputStream.close()

            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)

            if (bitmap != null) {
                val fixedBitmap = try {
                    val bais = ByteArrayInputStream(rawBytes)
                    val exif = ExifInterface(bais)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    }
                    if (!matrix.isIdentity) {
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else {
                        bitmap
                    }
                } catch (e: Exception) {
                    bitmap
                }

                val maxSize = 256 // Kecilkan ukuran untuk Base64 agar database tidak penuh
                val ratio = minOf(maxSize.toFloat() / fixedBitmap.width, maxSize.toFloat() / fixedBitmap.height)
                val resized = if (ratio < 1f) {
                    Bitmap.createScaledBitmap(
                        fixedBitmap,
                        (fixedBitmap.width * ratio).toInt(),
                        (fixedBitmap.height * ratio).toInt(),
                        true
                    )
                } else fixedBitmap

                val baos = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 70, baos) // Kualitas 70%
                val compressedBytes = baos.toByteArray()
                
                if (resized != bitmap) resized.recycle()
                if (fixedBitmap != bitmap) fixedBitmap.recycle()
                bitmap.recycle()
                
                // Gunakan Base64 langsung agar mengabaikan error Firebase Storage
                val base64String = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                onSuccess("data:image/jpeg;base64,$base64String")
            } else {
                onFailure("Gagal decode gambar, pastikan file berupa gambar yang valid")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onFailure("Gagal memproses gambar: ${e.message}")
        }
    }

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        folder: String = "items",
        onSuccess: (downloadUrl: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                onFailure("Gagal membaca gambar")
                return
            }
            val rawBytes = inputStream.readBytes()
            inputStream.close()

            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)

            val uploadBytes = if (bitmap != null) {
                val fixedBitmap = try {
                    val bais = ByteArrayInputStream(rawBytes)
                    val exif = ExifInterface(bais)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    }
                    if (!matrix.isIdentity) {
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else {
                        bitmap
                    }
                } catch (e: Exception) {
                    bitmap
                }

                val maxSize = 1024
                val ratio = minOf(maxSize.toFloat() / fixedBitmap.width, maxSize.toFloat() / fixedBitmap.height)
                val resized = if (ratio < 1f) {
                    Bitmap.createScaledBitmap(fixedBitmap, (fixedBitmap.width * ratio).toInt(), (fixedBitmap.height * ratio).toInt(), true)
                } else fixedBitmap

                val baos = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val compressedBytes = baos.toByteArray()

                if (resized != bitmap) resized.recycle()
                if (fixedBitmap != bitmap) fixedBitmap.recycle()
                bitmap.recycle()
                
                compressedBytes
            } else {
                rawBytes
            }

            // Gunakan Base64 langsung agar mengabaikan error Firebase Storage
            val base64String = android.util.Base64.encodeToString(uploadBytes, android.util.Base64.NO_WRAP)
            onSuccess("data:image/jpeg;base64,$base64String")

        } catch (e: Exception) {
            e.printStackTrace()
            onFailure("Gagal upload: ${e.message}")
        }
    }

    fun imageModelFromUrl(url: String): Any {
        return if (url.isNotEmpty() && url.startsWith("data:image") && url.contains("base64,")) {
            try {
                val base64String = url.substringAfter("base64,")
                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: url
            } catch (e: Exception) {
                url
            }
        } else {
            url
        }
    }
}
