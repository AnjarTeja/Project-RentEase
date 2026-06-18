package com.example.rentease.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PrimaryLight
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileUserScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val authManager = remember { FirebaseAuthManager() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    var name by remember { mutableStateOf("Pengguna") }
    var email by remember { mutableStateOf("-") }
    var phone by remember { mutableStateOf("-") }
    var nik by remember { mutableStateOf("-") }
    var address by remember { mutableStateOf("-") }
    var joined by remember { mutableStateOf("-") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editNik by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    fun loadProfile() {
        authManager.getUserData(
            onSuccess = { data ->
                name = data["name"] as? String ?: "Pengguna"
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
            },
            onFailure = { e ->
                error = e
                loading = false
            }
        )
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploading = true
            uploadError = null
            val uid = authManager.getCurrentUserUID()
            if (uid == null) { uploading = false; uploadError = "Sesi tidak valid"; return@rememberLauncherForActivityResult }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap == null) { uploadError = "Gagal membaca gambar"; uploading = false; return@rememberLauncherForActivityResult }

                val maxSize = 512
                val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                val resized = if (ratio < 1) {
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else bitmap

                val baos = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val data = baos.toByteArray()
                val base64 = "data:image/jpeg;base64,${android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)}"

                db.collection("users").document(uid)
                    .set(mapOf("profileImageUrl" to base64), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener { profileImageUrl = base64; loadProfile(); uploading = false }
                    .addOnFailureListener { e -> uploadError = "Gagal simpan: ${e.message}"; uploading = false }
            } catch (e: Exception) {
                uploadError = "Error: ${e.message}"
                uploading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadProfile() }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                title = if (isEditing) "Edit Profil" else "Profil Saya",
                onBackClick = if (isEditing) {{ isEditing = false; saveError = null }} else onBack
            )

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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileHeaderCard(
                        name = name,
                        email = email,
                        profileImageUrl = profileImageUrl,
                        isEditing = isEditing,
                        uploading = uploading,
                        uploadError = uploadError,
                        onPickImage = { imagePicker.launch("image/*") }
                    )

                    Text(
                        text = "Informasi Profil",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextLight,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isEditing) {
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

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileActionButton(
                                text = "Batal",
                                onClick = { isEditing = false; saveError = null },
                                modifier = Modifier.weight(1f),
                                backgroundColor = Color(0xFF2A2A4E)
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
                                backgroundColor = Primary
                            )
                        }
                    } else {
                        ProfileInfoCard(
                            phone = phone,
                            nik = nik,
                            address = address,
                            joined = joined
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                modifier = Modifier.weight(1f),
                                backgroundColor = Primary
                            )
                            ProfileActionButton(
                                text = "Keluar",
                                icon = Icons.AutoMirrored.Filled.ExitToApp,
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.weight(1f),
                                backgroundColor = ErrorColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar Akun", color = TextDark, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin keluar?", color = TextLight) },
            confirmButton = {
                TextButton(onClick = {
                    authManager.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }) {
                    Text("Ya, Keluar", color = ErrorColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String,
    email: String,
    profileImageUrl: String?,
    isEditing: Boolean,
    uploading: Boolean,
    uploadError: String?,
    onPickImage: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .then(if (isEditing) Modifier.clickable { onPickImage() } else Modifier)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, Primary.copy(alpha = 0.5f), CircleShape)
                        .background(TechCardBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profileImageUrl.isNullOrBlank()) {
                        key(profileImageUrl) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Foto Profil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryLight,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Ganti Foto",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            if (uploading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mengunggah...", style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
            if (uploadError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uploadError, style = MaterialTheme.typography.labelSmall, color = ErrorColor, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(name, style = MaterialTheme.typography.titleLarge, color = TextDark, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(email, style = MaterialTheme.typography.bodySmall, color = TextLight)
            Spacer(modifier = Modifier.height(10.dp))
            RoleBadge(role = "Pengguna")
        }
    }
}

@Composable
private fun ProfileInfoCard(phone: String, nik: String, address: String, joined: String) {
    GlowCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(0.dp)) {
            ProfileInfoRow(Icons.Default.Call, "No. Telepon", phone)
            HorizontalDivider(color = TextHint.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
            ProfileInfoRow(Icons.Default.VpnKey, "NIK", nik)
            HorizontalDivider(color = TextHint.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
            ProfileInfoRow(Icons.Default.Home, "Alamat", address)
            HorizontalDivider(color = TextHint.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
            ProfileInfoRow(Icons.Default.CalendarMonth, "Bergabung", joined, TextHint)
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String, valueColor: Color = TextDark) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextHint)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EditProfileFields(
    editName: String, editPhone: String, editNik: String, editAddress: String,
    onNameChange: (String) -> Unit, onPhoneChange: (String) -> Unit,
    onNikChange: (String) -> Unit, onAddressChange: (String) -> Unit,
    saveError: String?
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = editName, onValueChange = onNameChange, label = { Text("Nama Lengkap") },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Primary) },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors())
            OutlinedTextField(value = editPhone, onValueChange = onPhoneChange, label = { Text("No. Telepon") },
                leadingIcon = { Icon(Icons.Default.Call, null, tint = Primary) },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors())
            OutlinedTextField(value = editNik, onValueChange = onNikChange, label = { Text("NIK") },
                leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = Primary) },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors())
            OutlinedTextField(value = editAddress, onValueChange = onAddressChange, label = { Text("Alamat") },
                leadingIcon = { Icon(Icons.Default.Home, null, tint = Primary) },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors())
        }
    }
    if (saveError != null) {
        Text(saveError, color = ErrorColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ProfileActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    backgroundColor: Color = Primary, icon: ImageVector? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = TechCardBg, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = TechCardBg, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
    focusedLabelColor = Primary,
    unfocusedLabelColor = TextHint,
    cursorColor = Primary,
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark
)

private fun uploadImageToFirebase(
    context: android.content.Context,
    uri: Uri,
    storageRef: com.google.firebase.storage.StorageReference,
    uid: String,
    callback: (url: String?, error: String?) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap == null) { callback(null, "Gagal membaca gambar"); return }

        val maxSize = 1024
        val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        val resized = if (ratio < 1) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap

        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        val ref = storageRef.child("profiles/${uid}_${System.currentTimeMillis()}.jpg")
        ref.putBytes(data)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    callback(downloadUri.toString(), null)
                }.addOnFailureListener {
                    callback(null, "Gagal mendapatkan URL")
                }
            }
            .addOnFailureListener { e ->
                callback(null, "Gagal upload: ${e.message}")
            }
    } catch (e: Exception) {
        callback(null, "Error: ${e.message}")
    }
}
