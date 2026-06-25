package com.example.rentease.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.Item
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.CategoryFilterChips
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PurpleAccent
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AllItemsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val allItems = remember { mutableStateListOf<Item>() }
    val filteredItems = remember { mutableStateListOf<Item>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    fun applyFilter() {
        val query = searchQuery.trim().lowercase()
        var result = allItems.toList()
        if (query.isNotEmpty()) {
            result = result.filter { it.name.lowercase().contains(query) }
        }
        if (selectedCategory != null) {
            result = result.filter { it.category == selectedCategory }
        }
        filteredItems.clear()
        filteredItems.addAll(result)
    }

    LaunchedEffect(Unit) {
        db.collection("items")
            .get()
            .addOnSuccessListener { docs ->
                val items = docs.mapNotNull { doc ->
                    if (doc.exists()) {
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
                    } else null
                }
                allItems.clear()
                allItems.addAll(items.sortedByDescending { it.createdAt })
                isLoading = false
                applyFilter()
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Gagal memuat barang: ${e.message}"
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Semua Barang", onBackClick = onBack)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; applyFilter() },
                    placeholder = { Text("Cari barang...", color = TextHint) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextHint) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; applyFilter() }) {
                                Icon(Icons.Default.Clear, "Clear", tint = TextHint)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                    onCategorySelected = { selectedCategory = it; applyFilter() }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
                errorMessage.isNotEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = ErrorColor)
                }
                filteredItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotEmpty()) "Barang '$searchQuery' tidak ditemukan"
                        else "Belum ada barang",
                        color = TextLight
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        AllItemCard(item = item, onClick = {
                            navController.navigate(Screen.ItemDetail.createRoute(item.id))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AllItemCard(item: Item, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        radius = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl.ifBlank { null },
                contentDescription = item.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.price) + "/hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(
                        text = when (item.status) {
                            Item.STATUS_AVAILABLE -> "Tersedia"
                            Item.STATUS_RENTED -> "Disewa"
                            Item.STATUS_MAINTENANCE -> "Perbaikan"
                            else -> item.status
                        },
                        color = when (item.status) {
                            Item.STATUS_AVAILABLE -> SuccessColor
                            Item.STATUS_RENTED -> WarningColor
                            Item.STATUS_MAINTENANCE -> ErrorColor
                            else -> TextHint
                        }
                    )
                    StatusBadge(
                        text = when (item.approvalStatus) {
                            Item.APPROVAL_PENDING -> "Pending"
                            Item.APPROVAL_APPROVED -> "Disetujui"
                            Item.APPROVAL_REJECTED -> "Ditolak"
                            else -> item.approvalStatus
                        },
                        color = when (item.approvalStatus) {
                            Item.APPROVAL_PENDING -> WarningColor
                            Item.APPROVAL_APPROVED -> SuccessColor
                            Item.APPROVAL_REJECTED -> ErrorColor
                            else -> TextHint
                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stok: ${item.stock} | ${item.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = MaterialTheme.typography.labelSmall.fontSize
        )
    }
}
