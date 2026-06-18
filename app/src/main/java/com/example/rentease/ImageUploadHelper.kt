package com.example.rentease

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream
import java.util.UUID

object ImageUploadHelper {

    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        folder: String = "items",
        onSuccess: (downloadUrl: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: "unknown"
        val fileName = "${folder}/${uid}_${UUID.randomUUID()}.jpg"
        val ref = storage.child(fileName)

        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                onFailure("Gagal membaca gambar")
                return
            }

            val maxSize = 1024
            val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
            val resized = if (ratio < 1) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else bitmap

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            val uploadTask: UploadTask = ref.putBytes(data)
            uploadTask
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }.addOnFailureListener {
                        onFailure("Gagal mendapatkan URL gambar")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure("Gagal upload: ${e.message}")
                }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackUpload(context, imageUri, ref, onSuccess, onFailure)
        }
    }

    private fun fallbackUpload(
        context: Context,
        imageUri: Uri,
        ref: com.google.firebase.storage.StorageReference,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                onFailure("Gagal upload: ${e.message}")
            }
    }
}
