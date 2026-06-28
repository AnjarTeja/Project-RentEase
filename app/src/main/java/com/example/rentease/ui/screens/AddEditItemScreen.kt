package com.example.rentease.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ImageUploadHelper
import com.example.rentease.Item
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    navController: NavHostController,
    onBack: () -> Unit = {},
    itemId: String? = null,
    isUserMode: Boolean = false
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("1") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val statuses = listOf("Tersedia", "Disewa", "Perbaikan")
    val categories = Item.CATEGORIES

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val isEditMode = itemId != null

    LaunchedEffect(itemId) {
        if (itemId != null) {
            isLoading = true
            db.collection("items").document(itemId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        name = doc.getString("name") ?: ""
                        description = doc.getString("description") ?: ""
                        val p = doc.getDouble("price")
                        if (p != null) price = p.toInt().toString()
                        val s = doc.getLong("stock")
                        stock = (s?.toInt() ?: 1).toString()
                        val imageUrl = doc.getString("imageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            existingImageUrl = imageUrl
                        }
                        val status = doc.getString("status")
                        selectedStatus = when (status) {
                            Item.STATUS_AVAILABLE -> 0
                            Item.STATUS_RENTED -> 1
                            Item.STATUS_MAINTENANCE -> 2
                            else -> 0
                        }
                        val category = doc.getString("category") ?: Item.CATEGORY_CAMERA
                        selectedCategory = Item.CATEGORIES.indexOf(category).coerceAtLeast(0)
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    errorMessage = "Gagal memuat data barang"
                }
        }
    }

    fun saveItem() {
        if (name.isBlank() || price.isBlank()) {
            errorMessage = "Nama dan Harga tidak boleh kosong"
            return
        }
        val priceVal = price.toDoubleOrNull()
        if (priceVal == null) {
            errorMessage = "Harga tidak valid"
            return
        }
        val stockVal = stock.toIntOrNull() ?: 1
        val statusValue = if (isUserMode) {
            Item.STATUS_AVAILABLE
        } else {
            when (selectedStatus) {
                0 -> Item.STATUS_AVAILABLE
                1 -> Item.STATUS_RENTED
                2 -> Item.STATUS_MAINTENANCE
                else -> Item.STATUS_AVAILABLE
            }
        }
        val approvalStatus = if (isUserMode) Item.APPROVAL_PENDING else Item.APPROVAL_APPROVED
        val category = categories[selectedCategory]

        isSaving = true
        errorMessage = null

        val ownerId = authManager.getCurrentUserUID() ?: ""

        fun saveToFirestore(imageUrl: String) {
            val itemData = hashMapOf<String, Any>(
                "name" to name.trim(),
                "description" to description.trim(),
                "price" to priceVal,
                "stock" to stockVal,
                "status" to statusValue,
                "imageUrl" to imageUrl,
                "approvalStatus" to approvalStatus,
                "category" to category,
                "updatedAt" to System.currentTimeMillis()
            )

            if (isEditMode) {
                db.collection("items").document(itemId!!).update(itemData)
                    .addOnSuccessListener { isSaving = false; onBack() }
                    .addOnFailureListener { isSaving = false; errorMessage = "Gagal memperbarui barang" }
            } else {
                itemData["createdAt"] = System.currentTimeMillis()
                itemData["ownerId"] = ownerId
                itemData["rentCount"] = 0
                db.collection("items").add(itemData)
                    .addOnSuccessListener { isSaving = false; onBack() }
                    .addOnFailureListener { isSaving = false; errorMessage = "Gagal menambahkan barang" }
            }
        }

        // Check if image has changed
        val hasNewImage = selectedImageUri != null && selectedImageUri.toString() != existingImageUrl
        if (hasNewImage) {
            // Upload new image to Firebase Storage first
            com.example.rentease.ImageUploadHelper.uploadImage(
                context = context,
                imageUri = selectedImageUri!!,
                folder = "items",
                onSuccess = { downloadUrl -> saveToFirestore(downloadUrl) },
                onFailure = { msg ->
                    isSaving = false
                    errorMessage = msg
                }
            )
        } else {
            // No new image, use existing URL or empty
            saveToFirestore(if (existingImageUrl.isNotEmpty()) existingImageUrl else "")
        }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AppToolbar(
                title = when {
                    isEditMode && isUserMode -> "Edit Pengajuan Barang"
                    isEditMode -> "Edit Barang"
                    isUserMode -> "Pengajuan Barang"
                    else -> "Tambah Barang"
                },
                onBackClick = onBack
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                radius = 20.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TextButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            if (selectedImageUri != null) "Ganti Foto" else "Pilih Foto",
                            color = Primary
                        )
                    }

                    if (selectedImageUri != null || existingImageUrl.isNotEmpty()) {
                        val imageModel = selectedImageUri ?: ImageUploadHelper.imageModelFromUrl(existingImageUrl)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TechCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Preview foto",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Barang") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Harga per Hari") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stok") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors()
                    )

                    if (!isUserMode) {
                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = statuses[selectedStatus],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = textFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                statuses.forEachIndexed { index, s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = TextDark) },
                                        onClick = { selectedStatus = index; statusExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = categories[selectedCategory],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEachIndexed { index, cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = TextDark) },
                                    onClick = { selectedCategory = index; categoryExpanded = false }
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlowButton(
                        text = when {
                            isSaving -> "Menyimpan..."
                            isEditMode -> "Simpan Perubahan"
                            isUserMode -> "Ajukan Barang"
                            else -> "Simpan Data Barang"
                        },
                        onClick = { saveItem() },
                        enabled = !isSaving,
                        backgroundColor = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
    focusedLabelColor = Primary,
    unfocusedLabelColor = TextHint,
    cursorColor = Primary,
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark
)
