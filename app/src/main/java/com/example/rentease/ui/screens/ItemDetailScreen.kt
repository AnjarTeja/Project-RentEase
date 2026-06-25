package com.example.rentease.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Report
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.Item
import com.example.rentease.NotificationHelper
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowButton
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ItemDetailScreen(
    navController: NavHostController,
    onBack: () -> Unit = {},
    itemId: String
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val context = LocalContext.current
    var item by remember { mutableStateOf<Item?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var favoriteDocId by remember { mutableStateOf<String?>(null) }
    var showRentDialog by remember { mutableStateOf(false) }
    var rentDuration by remember { mutableStateOf("") }
    var rentNote by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf<String?>(null) }

    val uid = auth.currentUser?.uid

    LaunchedEffect(itemId) {
        db.collection("items").document(itemId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    item = Item(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        ownerId = doc.getString("ownerId") ?: "",
                        status = doc.getString("status") ?: Item.STATUS_AVAILABLE,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        approvalStatus = doc.getString("approvalStatus") ?: Item.APPROVAL_APPROVED,
                        rentCount = (doc.getLong("rentCount") ?: 0L).toInt(),
                        stock = (doc.getLong("stock") ?: 1L).toInt(),
                        category = doc.getString("category") ?: Item.CATEGORY_CAMERA
                    )
                    isLoading = false
                } else {
                    isLoading = false
                    errorMessage = "Barang tidak ditemukan"
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Gagal memuat: ${e.message}"
            }

        if (uid != null) {
            db.collection("favorites")
                .whereEqualTo("userId", uid)
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        isFavorite = true
                        favoriteDocId = snapshot.documents[0].id
                    }
                }

            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    userRole = doc.getString("role")
                }
        }
    }

    fun toggleFavorite() {
        if (uid == null) return
        if (isFavorite) {
            if (favoriteDocId != null) {
                db.collection("favorites").document(favoriteDocId!!).delete()
                isFavorite = false
                favoriteDocId = null
            } else {
                db.collection("favorites")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("itemId", itemId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (doc in snapshot) {
                            db.collection("favorites").document(doc.id).delete()
                        }
                        isFavorite = false
                    }
            }
        } else {
            val favData = hashMapOf("userId" to uid, "itemId" to itemId, "createdAt" to System.currentTimeMillis())
            db.collection("favorites").add(favData)
                .addOnSuccessListener { doc ->
                    isFavorite = true
                    favoriteDocId = doc.id
                }
                .addOnFailureListener {
                    isFavorite = false
                }
        }
    }

    fun submitRent() {
        val duration = rentDuration.toIntOrNull()
        if (duration == null || duration <= 0) return
        isSubmitting = true
        authManager.getUserData(
            onSuccess = { userData ->
                val renterName = userData["name"] as? String ?: "Pengguna"
                val renterEmail = userData["email"] as? String ?: authManager.getCurrentUserEmail() ?: ""
                val renterId = authManager.getCurrentUserUID() ?: ""
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val startDate = sdf.format(Date())
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, duration)
                val endDate = sdf.format(calendar.time)

                val rental = RentalRequest(
                    itemName = item?.name ?: "",
                    itemId = itemId,
                    renterName = renterName,
                    renterEmail = renterEmail,
                    renterId = renterId,
                    ownerId = item?.ownerId ?: "",
                    startDate = startDate,
                    endDate = endDate,
                    duration = duration,
                    note = rentNote,
                    status = RentalRequest.STATUS_PENDING,
                    createdAt = System.currentTimeMillis(),
                    pricePerDay = item?.price ?: 0.0,
                    itemImageUrl = item?.imageUrl ?: ""
                )

                db.collection("rentals").add(rental)
                    .addOnSuccessListener { docRef ->
                        val chat = hashMapOf(
                            "rentalId" to docRef.id,
                            "itemId" to itemId,
                            "itemName" to (item?.name ?: ""),
                            "renterId" to renterId,
                            "renterName" to renterName,
                            "ownerId" to (item?.ownerId ?: ""),
                            "ownerName" to "Pemilik Barang",
                            "lastMessage" to "Halo, saya ingin bertanya tentang ${item?.name}",
                            "lastMessageTime" to System.currentTimeMillis(),
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(item?.ownerId ?: "").get()
                            .addOnSuccessListener { ownerDoc ->
                                chat["ownerName"] = ownerDoc.getString("name") ?: "Pemilik Barang"
                                db.collection("chats").add(chat)
                            }
                            .addOnFailureListener { db.collection("chats").add(chat) }

                        NotificationHelper.showOwnerRentalRequestNotification(
                            context, renterName, item?.name ?: "", duration
                        )
                        showRentDialog = false
                        isSubmitting = false
                        onBack()
                    }
                    .addOnFailureListener { isSubmitting = false }
            },
            onFailure = { isSubmitting = false }
        )
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                title = "Detail Barang",
                onBackClick = onBack,
                trailingIcon = {
                    IconButton(onClick = { toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) ErrorColor else TextLight
                        )
                    }
                }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = ErrorColor, modifier = Modifier.padding(16.dp))
            } else if (item != null) {
                val currentItem = item!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = currentItem.imageUrl,
                        contentDescription = currentItem.name,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentItem.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(currentItem.price) + " / hari",
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        RoleBadge(role = currentItem.category)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentItem.status == Item.STATUS_AVAILABLE && currentItem.stock > 0) "Tersedia" else "Tidak Tersedia",
                            color = if (currentItem.status == Item.STATUS_AVAILABLE && currentItem.stock > 0) SuccessColor else ErrorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = currentItem.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Stok: ${currentItem.stock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uid != currentItem.ownerId && userRole != FirebaseAuthManager.ROLE_PETUGAS) {
                        val canRent = currentItem.status == Item.STATUS_AVAILABLE && currentItem.stock > 0
                        GlowButton(
                            text = if (canRent) "Ajukan Sewa" else "Tidak Tersedia",
                            onClick = { showRentDialog = true },
                            enabled = canRent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    GlowButton(
                        text = "Laporkan Barang",
                        onClick = { navController.navigate(Screen.ReportItem.createRoute(itemId, currentItem.name)) },
                        backgroundColor = ErrorColor
                    )
                }
            }
        }
    }

    if (showRentDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showRentDialog = false },
            title = { Text("Ajukan Sewa", color = TextDark) },
            text = {
                Column {
                    OutlinedTextField(
                        value = rentDuration,
                        onValueChange = { rentDuration = it },
                        label = { Text("Durasi (hari)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rentNote,
                        onValueChange = { rentNote = it },
                        label = { Text("Catatan (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { submitRent() },
                    enabled = !isSubmitting
                ) {
                    Text(if (isSubmitting) "Memproses..." else "Kirim", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRentDialog = false }) {
                    Text("Batal", color = TextLight)
                }
            },
            containerColor = com.example.rentease.ui.theme.TechCardBg
        )
    }
}
