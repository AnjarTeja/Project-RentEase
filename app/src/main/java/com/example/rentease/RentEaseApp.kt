package com.example.rentease

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Custom Application class for app-wide initialization.
 */
class RentEaseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        enableFirestoreOfflinePersistence()
    }

    private fun enableFirestoreOfflinePersistence() {
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) {
            // Settings already set, ignore
            e.printStackTrace()
        }
    }
}
