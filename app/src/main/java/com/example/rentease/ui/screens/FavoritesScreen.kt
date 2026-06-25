package com.example.rentease.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.Item
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.CategoryFilterChips
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FavoritesScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val favoriteItems = remember { mutableStateListOf<Item>() }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    fun loadFavorites() {
        val uid = authManager.getCurrentUserUID() ?: return
        isLoading = true

        db.collection("favorites")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val itemIds = snapshot.mapNotNull { it.getString("itemId") }
                if (itemIds.isEmpty()) {
                    favoriteItems.clear()
                    isLoading = false
                    return@addOnSuccessListener
                }

                var loaded = 0
                val items = mutableListOf<Item>()
                for (itemId in itemIds) {
                    db.collection("items").document(itemId).get()
                        .addOnSuccessListener { doc ->
                            loaded++
                            if (doc.exists()) {
                                items.add(
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
                                )
                            }
                            if (loaded >= itemIds.size) {
                                favoriteItems.clear()
                                favoriteItems.addAll(items)
                                isLoading = false
                            }
                        }
                        .addOnFailureListener {
                            loaded++
                            if (loaded >= itemIds.size) {
                                favoriteItems.clear()
                                favoriteItems.addAll(items)
                                isLoading = false
                            }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        loadFavorites()
    }

    fun removeFavorite(itemId: String) {
        val uid = authManager.getCurrentUserUID() ?: return
        db.collection("favorites")
            .whereEqualTo("userId", uid)
            .whereEqualTo("itemId", itemId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    db.collection("favorites").document(doc.id).delete()
                }
                favoriteItems.removeAll { it.id == itemId }
            }
            .addOnFailureListener {
                favoriteItems.removeAll { it.id == itemId }
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Favorit", onBackClick = onBack)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (favoriteItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada favorit", color = TextLight)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    CategoryFilterChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    val displayItems = if (selectedCategory == null) favoriteItems
                        else favoriteItems.filter { it.category == selectedCategory }
                    if (displayItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada favorit di kategori ini", color = TextLight)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayItems) { item ->
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate(Screen.ItemDetail.createRoute(item.id)) },
                                    radius = 12.dp
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = item.name,
                                            modifier = Modifier.fillMaxWidth().height(100.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextDark,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.price) + "/hari",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Primary
                                        )
                                        IconButton(
                                            onClick = { removeFavorite(item.id) },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
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
    }
}
