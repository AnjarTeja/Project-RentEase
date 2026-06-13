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

    // Login user with role detection from Firestore
    fun loginUser(
        email: String,
        password: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // All users (including staff) authenticate via Firebase Auth
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
                    exception.message?.contains("badly formatted") == true -> "Format email tidak valid"
                    exception.message?.contains("too many attempts") == true -> "Terlalu banyak percobaan. Coba lagi nanti."
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
                    // If document doesn't exist, create one with default user role
                    val email = firebaseAuth.currentUser?.email ?: ""
                    val userData = hashMapOf<String, Any>(
                        "name" to (firebaseAuth.currentUser?.displayName ?: "Pengguna"),
                        "email" to email,
                        "phone" to "",
                        "role" to ROLE_USER,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(userData)
                        .addOnSuccessListener {
                            cachedUserRole = ROLE_USER
                            lastCachedUid = uid
                            onSuccess(ROLE_USER)
                        }
                        .addOnFailureListener {
                            cachedUserRole = ROLE_USER
                            lastCachedUid = uid
                            onSuccess(ROLE_USER)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Failed to get user role: ${exception.message}")
                onFailure("Gagal mengambil role pengguna: ${exception.message}")
            }
    }

    // Verify that current user has the required role
    fun verifyUserRole(
        requiredRole: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        getUserRole(
            onSuccess = { role ->
                if (role == requiredRole || role == ROLE_ADMIN) {
                    onSuccess()
                } else {
                    Log.w(TAG, "Unauthorized: required=$requiredRole, actual=$role")
                    onFailure()
                }
            },
            onFailure = { onFailure() }
        )
    }

    // Clear cache on logout
    fun clearCache() {
        cachedUserRole = null
        lastCachedUid = null
    }

    // Get user data from Firestore
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

        // Try to get document by UID
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(document.data ?: emptyMap())
                } else {
                    // If document doesn't exist, check by email (migration scenario)
                    if (email != null) {
                        firestore.collection("users").whereEqualTo("email", email).get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    val existingDoc = querySnapshot.documents[0]
                                    val data = existingDoc.data ?: mutableMapOf<String, Any>()
                                    
                                    // Migrate data to UID-based document
                                    firestore.collection("users").document(uid).set(data)
                                        .addOnSuccessListener {
                                            if (existingDoc.id != uid) {
                                                firestore.collection("users").document(existingDoc.id).delete()
                                            }
                                            onSuccess(data)
                                        }
                                        .addOnFailureListener {
                                            onSuccess(data)
                                        }
                                } else {
                                    // Create default document for authenticated user
                                    val userData: Map<String, Any> = mapOf(
                                        "name" to "Pengguna",
                                        "email" to email,
                                        "phone" to "",
                                        "role" to ROLE_USER,
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    firestore.collection("users").document(uid).set(userData)
                                        .addOnSuccessListener { onSuccess(userData) }
                                        .addOnFailureListener { onFailure("Gagal membuat data pengguna.") }
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

    // Send password reset email
    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Log.d(TAG, "Password reset email sent to: $email")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Password reset failed: ${exception.message}")
                val errorMessage = when {
                    exception.message?.contains("no user record") == true -> "Email tidak terdaftar"
                    exception.message?.contains("badly formatted") == true -> "Format email tidak valid"
                    else -> "Gagal mengirim email reset: ${exception.message}"
                }
                onFailure(errorMessage)
            }
    }

    // Logout user
    fun logout() {
        clearCache()
        firebaseAuth.signOut()
        Log.d(TAG, "Logout successful")
    }
}
