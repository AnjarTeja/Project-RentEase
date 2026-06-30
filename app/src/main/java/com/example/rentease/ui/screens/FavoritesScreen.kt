package com.example.rentease.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.ListenerRegistration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ImageUploadHelper
import com.example.rentease.Item
import com.example.rentease.NetworkUtils
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.CategoryFilterChips
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

@Composable
fun FavoritesScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val context = LocalContext.current
    val favoriteItems = remember { mutableStateListOf<Item>() }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val favListenerReg = remember { mutableStateOf<ListenerRegistration?>(null) }

    var fetchError by remember { mutableStateOf(false) }

    fun processItemDocs(docs: com.google.firebase.firestore.QuerySnapshot): List<Item> {
        return docs.mapNotNull { doc ->
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
    }

    fun fetchItems(itemIds: List<String>) {
        if (itemIds.isEmpty()) {
            favoriteItems.clear()
            isLoading = false
            return
        }
        val batchSize = 10
        val allItems = mutableListOf<Item>()
        var completed = 0
        val totalBatches = (itemIds.size + batchSize - 1) / batchSize

        if (totalBatches == 0) {
            favoriteItems.clear()
            isLoading = false
            return
        }

        for (i in 0 until totalBatches) {
            val start = i * batchSize
            val end = min(start + batchSize, itemIds.size)
            val batch = itemIds.subList(start, end)

            db.collection("items")
                .whereEqualTo("status", Item.STATUS_AVAILABLE)
                .whereEqualTo("approvalStatus", Item.APPROVAL_APPROVED)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener { docs ->
                    allItems.addAll(processItemDocs(docs))
                    completed++
                    if (completed == totalBatches) {
                        favoriteItems.clear()
                        favoriteItems.addAll(allItems)
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    if (e.message?.contains("index") == true || e.message?.contains("FAILED_PRECONDITION") == true) {
                        db.collection("items")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                            .get()
                            .addOnSuccessListener { docs ->
                                val filtered = processItemDocs(docs).filter {
                                    it.status == Item.STATUS_AVAILABLE && it.approvalStatus == Item.APPROVAL_APPROVED
                                }
                                allItems.addAll(filtered)
                                completed++
                                if (completed == totalBatches) {
                                    favoriteItems.clear()
                                    favoriteItems.addAll(allItems)
                                    isLoading = false
                                    fetchError = false
                                }
                            }
                            .addOnFailureListener {
                                completed++
                                if (completed == totalBatches) {
                                    isLoading = false
                                }
                            }
                    } else {
                        completed++
                        if (completed == totalBatches) {
                            isLoading = false
                            fetchError = true
                        }
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        val uid = authManager.getCurrentUserUID()
        var reg: ListenerRegistration? = null
        if (uid != null) {
            reg = db.collection("favorites")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    val itemIds = snapshots.documents.mapNotNull { it.getString("itemId") }
                    isLoading = true
                    fetchItems(itemIds)
                }
            favListenerReg.value = reg
        } else {
            isLoading = false
        }
        onDispose { reg?.remove() }
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
                Toast.makeText(context, "Dihapus dari favorit", Toast.LENGTH_SHORT).show()
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
                    Text(
                        text = if (fetchError) "Gagal memuat favorit" else "Belum ada favorit",
                        color = TextLight
                    )
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
                                        if (item.imageUrl.isNotBlank()) {
                                            val imageModel = remember(item.imageUrl) { ImageUploadHelper.imageModelFromUrl(item.imageUrl) }
                                            AsyncImage(
                                                model = imageModel,
                                                contentDescription = item.name,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(com.example.rentease.ui.theme.TechCardBg),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Primary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Inventory,
                                                    contentDescription = null,
                                                    tint = TextHint.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
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
