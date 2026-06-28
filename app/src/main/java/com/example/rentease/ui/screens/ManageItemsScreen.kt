package com.example.rentease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.Item
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.CategoryFilterChips
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.FirebaseAuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManageItemsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var allItems by remember { mutableStateOf(listOf<Item>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var accessError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val authManager = FirebaseAuthManager()
        authManager.getUserRole(
            onSuccess = { role ->
                if (role != "admin" && role != "petugas") {
                    accessError = "Anda tidak memiliki akses ke halaman ini"
                }
            },
            onFailure = { accessError = "Gagal memverifikasi akses" }
        )
    }

    DisposableEffect(Unit) {
        val listener = db.collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("index") == true) {
                        db.collection("items").get()
                            .addOnSuccessListener { documents ->
                                allItems = documents.mapNotNull { doc ->
                                    try {
                                        Item(
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
                                    } catch (_: Exception) { null }
                                }.sortedByDescending { it.createdAt }
                                isLoading = false
                                errorMessage = null
                            }
                            .addOnFailureListener {
                                errorMessage = "Gagal memuat barang"
                                isLoading = false
                            }
                    } else {
                        errorMessage = "Gagal memuat barang: ${error.localizedMessage}"
                        isLoading = false
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            Item(
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
                        } catch (_: Exception) { null }
                    }
                    isLoading = false
                    errorMessage = null
                }
            }
        onDispose { listener.remove() }
    }

    val filteredItems = remember(allItems, searchQuery, selectedCategory) {
        var result = allItems
        if (searchQuery.isNotBlank()) {
            result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        if (selectedCategory != null) {
            result = result.filter { it.category == selectedCategory }
        }
        result
    }

    fun confirmDelete(item: Item) {
        deleteError = null
        db.collection("rentals")
            .whereEqualTo("itemId", item.id)
            .whereIn("status", listOf("pending", "approved"))
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    itemToDelete = item
                    showDeleteDialog = true
                } else {
                    deleteError = "Tidak bisa menghapus '${item.name}' karena masih ada ${snapshot.size()} transaksi aktif/pending."
                }
            }
            .addOnFailureListener {
                db.collection("rentals")
                    .whereEqualTo("itemId", item.id)
                    .get()
                    .addOnSuccessListener { allRentals ->
                        val activeCount = allRentals.documents.count {
                            val s = it.getString("status")
                            s == "pending" || s == "approved"
                        }
                        if (activeCount == 0) {
                            itemToDelete = item
                            showDeleteDialog = true
                        } else {
                            deleteError = "Tidak bisa menghapus '${item.name}' karena masih ada $activeCount transaksi aktif/pending."
                        }
                    }
                    .addOnFailureListener {
                        itemToDelete = item
                        showDeleteDialog = true
                    }
            }
    }

    fun deleteItem() {
        val item = itemToDelete ?: return
        isDeleting = true
        db.collection("items").document(item.id)
            .delete()
            .addOnSuccessListener {
                isDeleting = false
                showDeleteDialog = false
                itemToDelete = null
            }
            .addOnFailureListener {
                isDeleting = false
                deleteError = "Gagal menghapus barang"
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        if (accessError != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(title = "Akses Ditolak", onBackClick = onBack)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = accessError!!,
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(
                    title = "Kelola Barang",
                onBackClick = onBack,
                trailingIcon = {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Screen.AddEditItem.createRoute(fromUser = false)) },
                        containerColor = Primary,
                        text = { Text("Tambah", color = TechCardBg) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null, tint = TechCardBg) }
                    )
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari barang...", color = TextHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextHint) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                    cursorColor = Primary,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                )
            )

            if (deleteError != null) {
                Text(
                    text = deleteError!!,
                    color = ErrorColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            CategoryFilterChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = ErrorColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    filteredItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Belum ada data barang" else "Tidak ditemukan barang \"$searchQuery\"",
                                color = TextHint,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp, vertical = 8.dp
                            )
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextDark,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                StatusBadge(item.status)
                                                ApprovalBadge(item.approvalStatus)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Rp ${String.format("%,.0f", item.price)} | Stok: ${item.stock} | ${item.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextLight
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = dateFormat.format(Date(item.createdAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHint
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = {
                                                navController.navigate(Screen.AddEditItem.createRoute(item.id))
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit",
                                                    tint = Primary, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { confirmDelete(item) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Hapus",
                                                    tint = ErrorColor, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) { showDeleteDialog = false; itemToDelete = null } },
            title = { Text("Hapus Barang", color = TextDark) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus '${itemToDelete!!.name}'? Tindakan ini tidak dapat dibatalkan.",
                    color = TextLight
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteItem() },
                    enabled = !isDeleting
                ) {
                    Text(if (isDeleting) "Menghapus..." else "Ya, Hapus", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; itemToDelete = null }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, bgColor, textColor) = when (status) {
        Item.STATUS_AVAILABLE -> Triple("Tersedia", SuccessColor.copy(alpha = 0.15f), SuccessColor)
        Item.STATUS_RENTED -> Triple("Disewa", WarningColor.copy(alpha = 0.15f), WarningColor)
        Item.STATUS_MAINTENANCE -> Triple("Perbaikan", ErrorColor.copy(alpha = 0.15f), ErrorColor)
        else -> Triple(status, TechCardBg, TextLight)
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ApprovalBadge(approvalStatus: String) {
    if (approvalStatus == Item.APPROVAL_APPROVED) return
    val (label, bgColor, textColor) = when (approvalStatus) {
        Item.APPROVAL_PENDING -> Triple("Pending", WarningColor.copy(alpha = 0.15f), WarningColor)
        Item.APPROVAL_REJECTED -> Triple("Ditolak", ErrorColor.copy(alpha = 0.15f), ErrorColor)
        else -> return
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
