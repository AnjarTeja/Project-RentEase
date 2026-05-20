package com.example.rentease

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirebaseAuthManager {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseAuthManager"

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_PETUGAS = "petugas"
        const val ROLE_ADMIN = "admin"
    }

    // Pre-defined credentials for staff
    private val staffAccounts = mapOf(
        "petugas@gmail.com" to Pair("petugas123", ROLE_PETUGAS),
        "admin@gmail.com" to Pair("admin123", ROLE_ADMIN)
    )

    // In-memory cache for user role to reduce Firestore queries
    private var cachedUserRole: String? = null
    private var lastCachedUid: String? = null

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    // Get current user email
    fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    // Get current user UID
    fun getCurrentUserUID(): String? {
        return firebaseAuth.currentUser?.uid
    }

    // Register new user (only for user role)
    fun registerUser(
        email: String,
        password: String,
        name: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    // Save user data to Firestore with role
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "role" to ROLE_USER,
                        "createdAt" to System.currentTimeMillis()
                    )
                    
                    firestore.collection("users").document(uid)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "User registered successfully with role: $ROLE_USER")
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "Failed to save user data: ${exception.message}")
                            onFailure("Gagal menyimpan data pengguna: ${exception.message}")
                        }
                } else {
                    onFailure("UID tidak valid")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Registration failed: ${exception.message}")
                val errorMessage = when {
                    exception.message?.contains("email") == true -> "Email sudah terdaftar atau format tidak valid"
                    exception.message?.contains("password") == true -> "Kata sandi harus minimal 6 karakter"
                    else -> exception.message ?: "Pendaftaran gagal"
                }
                onFailure(errorMessage)
            }
    }

    // Login user with role detection
    fun loginUser(
        email: String,
        password: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val staffAccount = staffAccounts[email]
        if (staffAccount != null) {
            val (correctPassword, role) = staffAccount
            if (password == correctPassword) {
                // Ensure staff account exists in Firebase Auth
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Log.d(TAG, "Staff login successful in Auth: $role")
                        onSuccess(role)
                    }
                    .addOnFailureListener { loginException ->
                        // If login fails (user might not exist or password mismatch in Auth), try to create it
                        firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                Log.d(TAG, "Staff account created in Auth: $role")
                                onSuccess(role)
                            }
                            .addOnFailureListener { createException ->
                                Log.e(TAG, "Staff auth failed. Login err: ${loginException.message}, Create err: ${createException.message}")
                                // If we can't create it (maybe it exists but wrong password), let's try to sign in one more time
                                // Or we just fallback to the old behavior so they aren't blocked from logging in
                                if (createException.message?.contains("already in use") == true) {
                                    onFailure("Email staff sudah digunakan oleh user lain. Hubungi admin.")
                                } else {
                                    // Fallback: just proceed without valid Auth so they can at least enter the app
                                    // (Though editing profile won't work)
                                    Log.d(TAG, "Falling back to unauthenticated staff session")
                                    onSuccess(role)
                                }
                            }
                    }
                return
            } else {
                onFailure("Email atau kata sandi salah")
                return
            }
        }

        // Regular user login via Firebase Auth
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "User login successful")
                getUserRole(
                    onSuccess = { role ->
                        onSuccess(role)
                    },
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Login failed: ${exception.message}")
                val errorMessage = when {
                    exception.message?.contains("no user record") == true -> "Email tidak terdaftar"
                    exception.message?.contains("password is invalid") == true -> "Kata sandi salah"
                    else -> "Email atau kata sandi salah"
                }
                onFailure(errorMessage)
            }
    }

    // Get user role from Firestore with caching
    fun getUserRole(
        onSuccess: (role: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            onFailure("User tidak ditemukan")
            return
        }

        // Check cache first (same user, same session)
        if (cachedUserRole != null && lastCachedUid == uid) {
            Log.d(TAG, "User role retrieved from cache: $cachedUserRole")
            onSuccess(cachedUserRole!!)
            return
        }

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: ROLE_USER
                    // Cache the role
                    cachedUserRole = role
                    lastCachedUid = uid
                    Log.d(TAG, "User role retrieved: $role")
                    onSuccess(role)
                } else {
                    // Default to user role if document doesn't exist
                    cachedUserRole = ROLE_USER
                    lastCachedUid = uid
                    onSuccess(ROLE_USER)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Failed to get user role: ${exception.message}")
                onFailure("Gagal mengambil role pengguna: ${exception.message}")
            }
    }

    // Clear cache on logout
    fun clearCache() {
        cachedUserRole = null
        lastCachedUid = null
    }

    // Get user data from Firestore (auto-create or migrate for staff if missing)
    fun getUserData(
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid
        val email = firebaseAuth.currentUser?.email
        
        if (uid == null) {
            onFailure("User tidak ditemukan")
            return
        }

        // 1. First try to get document by UID
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(document.data ?: emptyMap())
                } else {
                    // 2. UID not found, check if document exists by email (Migration check)
                    if (email != null) {
                        firestore.collection("users").whereEqualTo("email", email).get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    // Found existing document with this email!
                                    val existingDoc = querySnapshot.documents[0]
                                    val data = existingDoc.data ?: mutableMapOf<String, Any>()
                                    
                                    // Move data to the new UID document
                                    firestore.collection("users").document(uid).set(data)
                                        .addOnSuccessListener {
                                            // Delete the old document if it has a different ID
                                            if (existingDoc.id != uid) {
                                                firestore.collection("users").document(existingDoc.id).delete()
                                            }
                                            onSuccess(data)
                                        }
                                        .addOnFailureListener {
                                            onSuccess(data) // Return data anyway
                                        }
                                } else {
                                    // 3. No document by email either, create new for staff
                                    val role = determineRoleFromEmail(email)
                                    if (role == ROLE_PETUGAS || role == ROLE_ADMIN) {
                                        val staffName = if (role == ROLE_ADMIN) "Administrator" else "Petugas"
                                        val userData: Map<String, Any> = mapOf(
                                            "name" to staffName,
                                            "email" to email,
                                            "phone" to "",
                                            "role" to role,
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                        firestore.collection("users").document(uid).set(userData)
                                            .addOnSuccessListener { onSuccess(userData) }
                                            .addOnFailureListener { onSuccess(userData) }
                                    } else {
                                        onFailure("Data pengguna tidak ditemukan.")
                                    }
                                }
                            }
                            .addOnFailureListener { onFailure("Gagal sinkronisasi data.") }
                    } else {
                        onFailure("Email tidak ditemukan.")
                    }
                }
            }
            .addOnFailureListener { exception ->
                onFailure("Gagal mengambil data: ${exception.message}")
            }
    }

    // Helper function to determine role from email
    private fun determineRoleFromEmail(email: String?): String {
        return when (email) {
            "petugas@gmail.com" -> ROLE_PETUGAS
            "admin@gmail.com" -> ROLE_ADMIN
            else -> ROLE_USER
        }
    }

    // Logout user
    fun logout() {
        clearCache()
        firebaseAuth.signOut()
        Log.d(TAG, "Logout successful")
    }
}
