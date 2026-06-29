package com.example.rentease.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ImageUploadHelper
import com.example.rentease.R

import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.NebulaGradient
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.components.StatCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorRed
import com.example.rentease.ui.theme.BlueDark
import com.example.rentease.ui.theme.BlueLight
import com.example.rentease.ui.theme.PrimaryBlue
import com.example.rentease.ui.theme.SuccessGreen
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextPrimary
import com.example.rentease.ui.theme.TextSecondary
import com.example.rentease.ui.theme.WarningOrange
import com.example.rentease.ui.theme.White
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    var nik by remember { mutableStateOf("-") }
    var address by remember { mutableStateOf("-") }
    var joined by remember { mutableStateOf("-") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var totalUsers by remember { mutableStateOf("-") }
    var totalTransactions by remember { mutableStateOf("-") }
    var totalItems by remember { mutableStateOf("-") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editNik by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Change password state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun loadProfile() {
        authManager.getUserData(
            onSuccess = { data ->
                name = data["name"] as? String ?: "Administrator"
                email = data["email"] as? String ?: authManager.getCurrentUserEmail() ?: "-"
                phone = data["phone"] as? String ?: "-"
                nik = data["nik"] as? String ?: "-"
                address = data["address"] as? String ?: "-"
                profileImageUrl = data["profileImageUrl"] as? String
                val createdAt = data["createdAt"]
                joined = if (createdAt is Long) {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    sdf.format(Date(createdAt))
                } else {
                    createdAt?.toString() ?: "-"
                }
                loading = false
                error = null
                isRefreshing = false
            },
            onFailure = { e ->
                error = e
                loading = false
                isRefreshing = false
            }
        )
    }

    fun loadStats() {
        db.collection("users").get().addOnSuccessListener { totalUsers = it.size().toString() }.addOnFailureListener { }
        db.collection("rentals").get().addOnSuccessListener { totalTransactions = it.size().toString() }.addOnFailureListener { }
        db.collection("items").get().addOnSuccessListener { totalItems = it.size().toString() }.addOnFailureListener { }
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
                loadProfile()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            when {
                loading && !isRefreshing -> ShimmerProfileSkeleton()
                error != null && !isRefreshing -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Gagal memuat profil",
                            color = ErrorRed,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error ?: "",
                            color = TextHint,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            onClick = { loading = true; error = null; loadProfile() },
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue
                        ) {
                            Text(
                                "Coba Lagi",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                color = White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                else -> PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { isRefreshing = true; loadProfile() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ProfileHeaderSection(
                            name = name,
                            email = email,
                            profileImageUrl = profileImageUrl,
                            isEditing = isEditing,
                            uploading = uploading,
                            uploadError = uploadError,
                            onBack = { navController.popBackStack() },
                            onPickImage = { imagePicker.launch("image/*") },
                            onDeletePhoto = { handleDeletePhoto() },
                            onPreview = { showPreview = true }
                        )

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Informasi Profil",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Color(0xFFE0E0E0))
                                )
                            }

                            AnimatedVisibility(
                                visible = isEditing,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                EditProfileFields(
                                    editName = editName,
                                    editPhone = editPhone,
                                    editNik = editNik,
                                    editAddress = editAddress,
                                    onNameChange = { editName = it },
                                    onPhoneChange = { editPhone = it },
                                    onNikChange = { editNik = it },
                                    onAddressChange = { editAddress = it },
                                    saveError = saveError
                                )
                            }

                            AnimatedVisibility(
                                visible = !isEditing,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                ProfileInfoCard(
                                    phone = phone,
                                    nik = nik,
                                    address = address,
                                    joined = joined
                                )
                            }

                            if (!isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Statistik Sistem",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(Color(0xFFE0E0E0))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Person,
                                        value = totalUsers,
                                        label = "Pengguna",
                                        iconTint = PrimaryBlue,
                                        valueColor = PrimaryBlue
                                    )
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.ShoppingCart,
                                        value = totalTransactions,
                                        label = "Transaksi",
                                        iconTint = SuccessGreen,
                                        valueColor = SuccessGreen
                                    )
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Inventory,
                                        value = totalItems,
                                        label = "Barang",
                                        iconTint = WarningOrange,
                                        valueColor = WarningOrange
                                    )
                                }
                            }

                            if (isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ProfileActionButton(
                                        text = "Batal",
                                        onClick = { isEditing = false; saveError = null },
                                        modifier = Modifier.weight(1f),
                                        backgroundColor = White,
                                        textColor = TextPrimary,
                                        borderColor = Color(0xFFDDDDDD)
                                    )
                                    ProfileActionButton(
                                        text = "Simpan",
                                        onClick = {
                                            if (editName.isBlank()) {
                                                saveError = "Nama tidak boleh kosong"
                                                return@ProfileActionButton
                                            }
                                            saveError = null
                                            val uid = authManager.getCurrentUserUID()
                                            if (uid == null) {
                                                saveError = "Sesi tidak valid, silakan login ulang"
                                                return@ProfileActionButton
                                            }
                                            val updates = hashMapOf<String, Any>(
                                                "name" to editName,
                                                "phone" to editPhone,
                                                "nik" to editNik,
                                                "address" to editAddress
                                            )
                                            db.collection("users").document(uid)
                                                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                                                .addOnSuccessListener { loadProfile(); isEditing = false }
                                                .addOnFailureListener { e -> saveError = "Gagal menyimpan: ${e.message}" }
                                        },
                                        modifier = Modifier.weight(1f),
                                        backgroundColor = PrimaryBlue,
                                        textColor = White
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ProfileActionButton(
                                        text = "Edit Profil",
                                        icon = Icons.Default.Edit,
                                        onClick = {
                                            editName = name
                                            editPhone = phone
                                            editNik = nik
                                            editAddress = address
                                            saveError = null
                                            isEditing = true
                                        },
                                        backgroundColor = PrimaryBlue,
                                        textColor = White
                                    )
                                    ProfileActionButton(
                                        text = "Ubah Kata Sandi",
                                        icon = Icons.Default.Lock,
                                        onClick = {
                                            currentPassword = ""
                                            newPassword = ""
                                            confirmPassword = ""
                                            passwordError = null
                                            showChangePasswordDialog = true
                                        },
                                        backgroundColor = White,
                                        textColor = WarningOrange,
                                        borderColor = WarningOrange.copy(alpha = 0.4f)
                                    )
                                    ProfileActionButton(
                                        text = "Keluar",
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        onClick = { showLogoutDialog = true },
                                        backgroundColor = ErrorRed,
                                        textColor = White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showPreview && !profileImageUrl.isNullOrBlank()) {
        AvatarPreviewDialog(
            imageUrl = profileImageUrl!!,
            onDismiss = { showPreview = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar Akun", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin keluar dari panel Administrator?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    authManager.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }) {
                    Text("Ya, Keluar", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Ubah Kata Sandi", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Kata sandi saat ini") },
                        singleLine = true,
                        colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Kata sandi baru") },
                        singleLine = true,
                        colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi kata sandi baru") },
                        singleLine = true,
                        colors = fieldColors()
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = ErrorRed,
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
                    Text("Simpan", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ProfileHeaderSection(
    name: String,
    email: String,
    profileImageUrl: String?,
    isEditing: Boolean,
    uploading: Boolean,
    uploadError: String?,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    onDeletePhoto: () -> Unit,
    onPreview: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PrimaryBlue, BlueDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 20.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = White
                    )
                }
                Text(
                    text = if (isEditing) "Edit Profil" else "Profil Admin",
                    style = MaterialTheme.typography.titleLarge,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !uploading) { if (isEditing) onPickImage() else onPreview() }
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, White.copy(alpha = 0.8f), CircleShape)
                        .background(BlueLight)
                        .align(Alignment.Center),
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
                            tint = PrimaryBlue.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Ganti Foto",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Ubah Foto",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (!profileImageUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = onDeletePhoto,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .background(ErrorRed.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Foto",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            if (uploading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Mengunggah...",
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.8f)
                )
            }
            if (uploadError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uploadError,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFCDD2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                color = White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium,
                color = White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            RoleBadge(role = "Admin", textColor = PrimaryBlue)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileInfoCard(phone: String, nik: String, address: String, joined: String) {
    GlowCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(0.dp)) {
            ProfileInfoRow(Icons.Default.Call, "No. Telepon", phone)
            HorizontalDivider(
                color = TextHint.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            ProfileInfoRow(Icons.Default.VpnKey, "NIK", nik)
            HorizontalDivider(
                color = TextHint.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            ProfileInfoRow(Icons.Default.Home, "Alamat", address)
            HorizontalDivider(
                color = TextHint.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            ProfileInfoRow(
                Icons.Default.CalendarMonth,
                "Bergabung",
                joined,
                TextHint
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextHint
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EditProfileFields(
    editName: String,
    editPhone: String,
    editNik: String,
    editAddress: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNikChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    saveError: String?
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = editName,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap") },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryBlue) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            OutlinedTextField(
                value = editPhone,
                onValueChange = onPhoneChange,
                label = { Text("No. Telepon") },
                leadingIcon = { Icon(Icons.Default.Call, null, tint = PrimaryBlue) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            OutlinedTextField(
                value = editNik,
                onValueChange = onNikChange,
                label = { Text("NIK") },
                leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = PrimaryBlue) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            OutlinedTextField(
                value = editAddress,
                onValueChange = onAddressChange,
                label = { Text("Alamat") },
                leadingIcon = { Icon(Icons.Default.Home, null, tint = PrimaryBlue) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
        }
    }
    if (saveError != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            saveError,
            color = ErrorRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun ProfileActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PrimaryBlue,
    textColor: Color = White,
    icon: ImageVector? = null,
    borderColor: Color? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = borderColor?.let { BorderStroke(1.dp, it) },
        shadowElevation = if (borderColor != null) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AvatarPreviewDialog(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Foto Profil",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ShimmerProfileSkeleton() {
    val shimmerColors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF252545),
        Color(0xFF1A1A2E)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    @Composable
    fun ShimmerBlock(
        modifier: Modifier,
        shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(translateAnim.value - 300, 0f),
                        end = Offset(translateAnim.value, 0f)
                    )
                )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        ShimmerBlock(
            modifier = Modifier.size(120.dp),
            shape = CircleShape
        )

        Spacer(modifier = Modifier.height(20.dp))

        ShimmerBlock(
            modifier = Modifier.size(width = 160.dp, height = 20.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        ShimmerBlock(
            modifier = Modifier.size(width = 120.dp, height = 14.dp),
            shape = RoundedCornerShape(7.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ShimmerBlock(
            modifier = Modifier.size(width = 80.dp, height = 26.dp),
            shape = RoundedCornerShape(13.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        ShimmerBlock(
            modifier = Modifier.size(width = 100.dp, height = 16.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBlock(
                        modifier = Modifier.size(width = 80.dp, height = 12.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBlock(
                        modifier = Modifier.size(width = 140.dp, height = 14.dp),
                        shape = RoundedCornerShape(7.dp)
                    )
                }
            }
            if (it < 3) {
                HorizontalDivider(
                    color = TextHint.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBlock(
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp)
            )
            ShimmerBlock(
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextHint,
    cursorColor = PrimaryBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
