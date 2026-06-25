package com.example.rentease.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ImageUploadHelper
import com.example.rentease.R
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.components.InfoRow
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.components.StatCard
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PrimaryLight
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileAdminScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val authManager = remember { FirebaseAuthManager() }
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    var name by remember { mutableStateOf("Administrator") }
    var email by remember { mutableStateOf("-") }
    var phone by remember { mutableStateOf("-") }
    var joined by remember { mutableStateOf("-") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var totalUsers by remember { mutableStateOf("-") }
    var totalTransactions by remember { mutableStateOf("-") }
    var totalItems by remember { mutableStateOf("-") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    fun loadProfile() {
        authManager.getUserData(
            onSuccess = { data ->
                name = data["name"] as? String ?: "Administrator"
                email = data["email"] as? String ?: authManager.getCurrentUserEmail() ?: "-"
                phone = data["phone"] as? String ?: "-"
                profileImageUrl = data["profileImageUrl"] as? String
                val createdAt = data["createdAt"]
                joined = if (createdAt is Long) {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    sdf.format(Date(createdAt))
                } else {
                    createdAt?.toString() ?: "-"
                }
                loading = false
            },
            onFailure = { e ->
                error = e
                loading = false
            }
        )
    }

    fun loadStats() {
        db.collection("users").get().addOnSuccessListener { totalUsers = it.size().toString() }
        db.collection("rentals").get().addOnSuccessListener { totalTransactions = it.size().toString() }
        db.collection("items").get().addOnSuccessListener { totalItems = it.size().toString() }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploading = true
            uploadError = null
            ImageUploadHelper.uploadProfileImage(
                imageUri = uri,
                context = context,
                onSuccess = { downloadUrl ->
                    val uid = authManager.getCurrentUserUID()
                    if (uid == null) {
                        uploadError = "Sesi tidak valid"
                        uploading = false
                        return@uploadProfileImage
                    }
                    db.collection("users").document(uid)
                        .set(
                            mapOf("profileImageUrl" to downloadUrl),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .addOnSuccessListener {
                            profileImageUrl = downloadUrl
                            loadProfile()
                            uploading = false
                        }
                        .addOnFailureListener { e ->
                            uploadError = "Gagal simpan: ${e.message}"
                            uploading = false
                        }
                },
                onFailure = { msg ->
                    uploadError = msg
                    uploading = false
                }
            )
        }
    }

    fun handleDeletePhoto() {
        val uid = authManager.getCurrentUserUID() ?: return
        uploading = true
        db.collection("users").document(uid)
            .set(
                mapOf("profileImageUrl" to ""),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                profileImageUrl = null
                uploading = false
            }
            .addOnFailureListener { e ->
                uploadError = "Gagal menghapus: ${e.message}"
                uploading = false
            }
    }

    LaunchedEffect(Unit) {
        loadProfile()
        loadStats()
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppToolbar(title = "Profile Admin", onBackClick = onBack)

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextHint)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "", color = ErrorColor)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(96.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(TechCardBg)
                                        .clickable { imagePicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profileImageUrl.isNullOrBlank()) {
                                        key(profileImageUrl) {
                                            val imageModel = androidx.compose.runtime.remember(profileImageUrl) {
                                                val url = profileImageUrl
                                                if (url != null && url.startsWith("data:image") && url.contains("base64,")) {
                                                    try {
                                                        val base64String = url.substringAfter("base64,")
                                                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                                        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                    } catch (e: Exception) {
                                                        url
                                                    }
                                                } else {
                                                    url
                                                }
                                            }
                                            AsyncImage(
                                                model = imageModel,
                                                contentDescription = "Foto Profil",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                                                error = painterResource(R.drawable.ic_launcher_foreground)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .clickable { imagePicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Ganti Foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            "Ubah",
                                            color = Color.White.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                if (!profileImageUrl.isNullOrBlank()) {
                                    IconButton(
                                        onClick = { handleDeletePhoto() },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(28.dp)
                                            .background(ErrorColor.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus Foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            if (uploading) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Mengunggah...", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                            if (uploadError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(uploadError!!, style = MaterialTheme.typography.labelSmall, color = ErrorColor, textAlign = TextAlign.Center)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(name, style = MaterialTheme.typography.titleLarge, color = TextDark)
                            RoleBadge(role = "Admin")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(email, style = MaterialTheme.typography.bodySmall, color = TextLight)
                        }
                    }

                    InfoRow(icon = Icons.Default.Call, label = "No. Telepon", value = phone)
                    InfoRow(icon = Icons.Default.CalendarMonth, label = "Bergabung", value = joined)

                    Text("Statistik Sistem", style = MaterialTheme.typography.titleMedium, color = TextDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            icon = Icons.Default.Person,
                            value = totalUsers,
                            label = "Pengguna",
                            modifier = Modifier.weight(1f),
                            iconTint = Primary,
                            valueColor = Primary
                        )
                        StatCard(
                            icon = Icons.Default.ShoppingCart,
                            value = totalTransactions,
                            label = "Transaksi",
                            modifier = Modifier.weight(1f),
                            iconTint = SuccessColor,
                            valueColor = SuccessColor
                        )
                        StatCard(
                            icon = Icons.Default.Inventory,
                            value = totalItems,
                            label = "Barang",
                            modifier = Modifier.weight(1f),
                            iconTint = WarningColor,
                            valueColor = WarningColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    GlowButton(
                        text = "Edit Profil",
                        onClick = {
                            editName = name
                            editPhone = phone
                            showEditDialog = true
                        }
                    )

                    GlowButton(
                        text = "Ubah Kata Sandi",
                        onClick = {
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            passwordError = null
                            showChangePasswordDialog = true
                        },
                        backgroundColor = WarningColor
                    )

                    GlowButton(
                        text = "Keluar",
                        onClick = { showLogoutDialog = true },
                        backgroundColor = ErrorColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profil", color = TextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextHint,
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("No. Telepon") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextHint,
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uid = authManager.getCurrentUserUID() ?: return@TextButton
                    val updates = hashMapOf<String, Any>(
                        "name" to editName,
                        "phone" to editPhone
                    )
                    db.collection("users").document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            showEditDialog = false
                            loadProfile()
                        }
                }) {
                    Text("Simpan", color = SuccessColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = com.example.rentease.ui.theme.TechCardBg
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Ubah Kata Sandi", color = TextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Kata sandi saat ini") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextHint,
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Kata sandi baru") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextHint,
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi kata sandi baru") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Primary,
                            unfocusedLabelColor = TextHint,
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val user = auth.currentUser ?: return@TextButton
                    val emailUser = user.email ?: return@TextButton
                    if (currentPassword.isEmpty()) {
                        passwordError = "Kata sandi saat ini wajib diisi"
                        return@TextButton
                    }
                    if (newPassword.length < 6) {
                        passwordError = "Kata sandi baru minimal 6 karakter"
                        return@TextButton
                    }
                    if (newPassword != confirmPassword) {
                        passwordError = "Konfirmasi kata sandi tidak cocok"
                        return@TextButton
                    }
                    passwordError = null
                    val credential = EmailAuthProvider.getCredential(emailUser, currentPassword)
                    user.reauthenticate(credential)
                        .addOnSuccessListener {
                            user.updatePassword(newPassword)
                                .addOnSuccessListener {
                                    showChangePasswordDialog = false
                                    passwordError = null
                                }
                                .addOnFailureListener { e ->
                                    passwordError = "Gagal mengubah: ${e.message}"
                                }
                        }
                        .addOnFailureListener {
                            passwordError = "Kata sandi saat ini salah"
                        }
                }) {
                    Text("Simpan", color = SuccessColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = com.example.rentease.ui.theme.TechCardBg
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar Akun Admin", color = TextDark) },
            text = { Text("Apakah Anda yakin ingin keluar dari panel Administrator?", color = TextLight) },
            confirmButton = {
                TextButton(onClick = {
                    authManager.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Ya, Keluar", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = com.example.rentease.ui.theme.TechCardBg
        )
    }
}
