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

    fun seedPredefinedAccounts(onComplete: () -> Unit) {
        val accounts = listOf(
            Triple("admin@gmail.com", "12345678", ROLE_ADMIN),
            Triple("petugas@gmail.com", "12345678", ROLE_PETUGAS)
        )
        seedNextAccount(accounts, 0, onComplete)
    }

    private fun seedNextAccount(
        accounts: List<Triple<String, String, String>>,
        index: Int,
        onComplete: () -> Unit
    ) {
        if (index >= accounts.size) {
            firebaseAuth.signOut()
            onComplete()
            return
        }
        val (email, password, role) = accounts[index]

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener
                val userData = hashMapOf(
                    "name" to when (role) {
                        ROLE_ADMIN -> "Administrator"
                        ROLE_PETUGAS -> "Petugas"
                        else -> role
                    },
                    "email" to email,
                    "phone" to "",
                    "role" to role,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .set(userData, SetOptions.merge())
                    .addOnCompleteListener {
                        firebaseAuth.signOut()
                        seedNextAccount(accounts, index + 1, onComplete)
                    }
            }
            .addOnFailureListener { exception ->
                if (exception.message?.contains("email address is already in use") == true) {
                    // Account exists — ensure Firestore document has correct role
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid ?: ""
                            firestore.collection("users").document(uid)
                                .set(mapOf("role" to role), SetOptions.merge())
                                .addOnCompleteListener {
                                    firebaseAuth.signOut()
                                    seedNextAccount(accounts, index + 1, onComplete)
                                }
                        }
                        .addOnFailureListener {
                            firebaseAuth.signOut()
                            seedNextAccount(accounts, index + 1, onComplete)
                        }
                } else {
                    firebaseAuth.signOut()
                    seedNextAccount(accounts, index + 1, onComplete)
                }
            }
    }

    // Login or auto-register predefined accounts (admin/petugas)
    fun loginOrRegisterPredefinedAccount(
        email: String,
        password: String,
        role: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Login successful for predefined account: $email")
                getUserRole(onSuccess = onSuccess, onFailure = onFailure)
            }
            .addOnFailureListener { signInError ->
                val signInMsg = signInError.message ?: ""
                Log.d(TAG, "Sign-in failed for $email: $signInMsg")

                if (signInMsg.contains("no user record", true)) {
                    // Account doesn't exist — create it
                    createPredefinedAccount(email, password, role, onSuccess, onFailure)
                } else if (signInMsg.contains("password is invalid", true) ||
                           signInMsg.contains("wrong password", true) ||
                           signInMsg.contains("invalid password", true)) {
                    // Account exists but wrong password
                    val action = if (email.contains("admin") || email.contains("petugas")) {
                        "Hapus dulu akun $email di Firebase Console (Authentication → Users), lalu login lagi"
                    } else {
                        "Coba reset kata sandi"
                    }
                    onFailure("Kata sandi salah untuk $email. $action.")
                } else if (signInMsg.contains("too many attempts", true)) {
                    onFailure("Terlalu banyak percobaan. Tunggu beberapa saat lalu coba lagi.")
                } else {
                    // Unknown error — try to create (might be transient)
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid
                            if (uid == null) {
                                onFailure("Gagal membuat akun: UID kosong")
                                return@addOnSuccessListener
                            }
                            savePredefinedAccount(uid, email, password, role, onSuccess, onFailure)
                        }
                        .addOnFailureListener { createError ->
                            val createMsg = createError.message ?: ""
                            if (createMsg.contains("email address is already in use", true)) {
                                // Account exists but sign-in failed for non-password reason
                                onFailure("Akun $email sudah ada. Error: $signInMsg. Coba hapus dulu akun di Firebase Console.")
                            } else if (createMsg.contains("not enabled", true)) {
                                onFailure("Login Email/Password belum diaktifkan di Firebase Console")
                            } else {
                                onFailure("Login gagal: $signInMsg")
                            }
                        }
                }
            }
    }

    private fun createPredefinedAccount(
        email: String,
        password: String,
        role: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "Creating predefined account: $email")
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    onFailure("Gagal membuat akun: UID kosong")
                    return@addOnSuccessListener
                }
                savePredefinedAccount(uid, email, password, role, onSuccess, onFailure)
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "Failed to create predefined account: ${e.message}")
                val msg = when {
                    e.message?.contains("not enabled", true) == true ->
                        "Login Email/Password belum diaktifkan di Firebase Console"
                    e.message?.contains("email", true) == true ->
                        "Email sudah terdaftar atau tidak valid"
                    e.message?.contains("password", true) == true ->
                        "Kata sandi minimal 6 karakter"
                    else ->
                        "Gagal membuat akun: ${e.message}"
                }
                onFailure(msg)
            }
    }

    private fun savePredefinedAccount(
        uid: String,
        email: String,
        password: String,
        role: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val name = when (role) {
            ROLE_ADMIN -> "Administrator"
            ROLE_PETUGAS -> "Petugas"
            else -> role
        }
        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to "",
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                cachedUserRole = role
                lastCachedUid = uid
                Log.d(TAG, "Predefined account saved: $email -> $role")
                onSuccess(role)
            }
            .addOnFailureListener { e ->
                cachedUserRole = role
                lastCachedUid = uid
                Log.d(TAG, "Account created but Firestore save failed: ${e.message}")
                onSuccess(role)
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
