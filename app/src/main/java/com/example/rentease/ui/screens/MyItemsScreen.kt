package com.example.rentease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.ImageUploadHelper
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.Item
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.CategoryFilterChips
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PurpleAccent
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MyItemsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val allItems = remember { mutableStateListOf<Item>() }
    val filteredItems = remember { mutableStateListOf<Item>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Item?>(null) }

    fun loadItems() {
        val uid = authManager.getCurrentUserUID() ?: return
        isLoading = true
        db.collection("items")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { docs ->
                val items = docs.mapNotNull { doc ->
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
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.createdAt }
                allItems.clear()
                allItems.addAll(items)
                filteredItems.clear()
                filteredItems.addAll(items)
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(Unit) {
        loadItems()
    }

    fun applySearch(query: String, category: String? = selectedCategory) {
        searchQuery = query
        var result = allItems.toList()
        if (query.isNotBlank()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) }
        }
        if (category != null) {
            result = result.filter { it.category == category }
        }
        filteredItems.clear()
        filteredItems.addAll(result)
    }

    fun deleteItem(item: Item) {
        db.collection("rentals")
            .whereEqualTo("itemId", item.id)
            .get()
            .addOnSuccessListener { snapshot ->
                val hasActiveRental = snapshot.documents.any { doc ->
                    val s = doc.getString("status")
                    s == "pending" || s == "approved"
                }
                if (hasActiveRental) {
                    deleteTarget = null
                } else {
                    db.collection("items").document(item.id).delete()
                        .addOnSuccessListener { deleteTarget = null; loadItems() }
                }
            }
            .addOnFailureListener { deleteTarget = null }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                title = "Barang Saya",
                onBackClick = onBack,
                trailingIcon = {
                    IconButton(onClick = { navController.navigate(Screen.AddEditItem.createRoute(fromUser = true)) }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Primary)
                    }
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { applySearch(it) },
                placeholder = { Text("Cari barang...", color = TextHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextHint) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                    cursorColor = Primary,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            CategoryFilterChips(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    applySearch(searchQuery, it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotEmpty()) "Barang \"$searchQuery\" tidak ditemukan"
                        else "Anda belum memiliki barang",
                        color = TextLight
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), radius = 12.dp) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                val imageModel = remember(item.imageUrl) { ImageUploadHelper.imageModelFromUrl(item.imageUrl) }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .width(80.dp).height(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TechCardBg),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.name, style = MaterialTheme.typography.titleSmall, color = TextDark)
                                    Text(
                                        text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.price) + "/hari",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Primary
                                    )
                                    Row {
                                        RoleBadge(
                                            role = when (item.status) {
                                                Item.STATUS_AVAILABLE -> "Tersedia"
                                                Item.STATUS_RENTED -> "Disewa"
                                                else -> item.status
                                            },
                                            textColor = when (item.status) {
                                                Item.STATUS_AVAILABLE -> SuccessColor
                                                Item.STATUS_RENTED -> WarningColor
                                                else -> TextLight
                                            }
                                        )
                                        if (item.approvalStatus == Item.APPROVAL_PENDING) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            RoleBadge(role = "Pending", textColor = PurpleAccent)
                                        }
                                    }
                                }
                                Column {
                                    IconButton(onClick = { navController.navigate(Screen.AddEditItem.createRoute(item.id, fromUser = true)) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary)
                                    }
                                    IconButton(onClick = { deleteTarget = item }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ErrorColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus Barang", color = TextDark) },
            text = { Text("Apakah Anda yakin ingin menghapus '${deleteTarget!!.name}'?", color = TextLight) },
            confirmButton = {
                TextButton(onClick = { deleteItem(deleteTarget!!) }) {
                    Text("Ya, Hapus", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Batal", color = TextLight)
                }
            },
            containerColor = TechCardBg
        )
    }
}
