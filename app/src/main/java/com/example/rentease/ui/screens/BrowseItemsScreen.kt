package com.example.rentease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.ImageUploadHelper
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
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BrowseItemsScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val allItems = remember { mutableStateListOf<Item>() }
    val filteredItems = remember { mutableStateListOf<Item>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }
    var sortOption by remember { mutableIntStateOf(0) }
    var sortExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val sortLabels = listOf("Populer", "Termurah", "Termahal", "Terbaru")

    fun applyFilter() {
        var result = allItems.toList()
        val query = searchQuery.trim().lowercase()
        if (query.isNotEmpty()) {
            result = result.filter { it.name.lowercase().contains(query) || it.description.lowercase().contains(query) }
        }
        if (selectedCategory != null) {
            result = result.filter { it.category == selectedCategory }
        }
        val min = priceMin.toDoubleOrNull() ?: 0.0
        val max = priceMax.toDoubleOrNull() ?: Double.MAX_VALUE
        result = result.filter { it.price >= min && it.price <= max }
        when (sortOption) {
            0 -> result = result.sortedByDescending { it.rentCount }
            1 -> result = result.sortedBy { it.price }
            2 -> result = result.sortedByDescending { it.price }
            3 -> result = result.sortedByDescending { it.createdAt }
        }
        filteredItems.clear()
        filteredItems.addAll(result)
    }

    DisposableEffect(Unit) {
        val listener = db.collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Gagal memuat barang: ${error.message}"
                    isLoading = false
                    return@addSnapshotListener
                }
                val items = mutableListOf<Item>()
                for (doc in snapshot ?: emptyList()) {
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
                allItems.clear()
                allItems.addAll(items.sortedByDescending { it.rentCount })
                isLoading = false
                errorMessage = ""
                applyFilter()
            }
        onDispose { listener.remove() }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Cari Barang", onBackClick = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; applyFilter() },
                    placeholder = { Text("Cari barang...", color = TextHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextHint) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; applyFilter() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextHint)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceMin,
                        onValueChange = { priceMin = it; applyFilter() },
                        placeholder = { Text("Min", color = TextHint) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    Text("-", color = TextLight)
                    OutlinedTextField(
                        value = priceMax,
                        onValueChange = { priceMax = it; applyFilter() },
                        placeholder = { Text("Max", color = TextHint) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = TechCardBg.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { sortExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Urutkan: ${sortLabels.getOrElse(sortOption) { "Populer" }}",
                                color = TextDark,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextHint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        sortLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (index == sortOption) FontWeight.Bold else FontWeight.Normal,
                                        color = if (index == sortOption) Primary else TextDark
                                    )
                                },
                                onClick = { sortOption = index; sortExpanded = false; applyFilter() }
                            )
                        }
                    }
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                errorMessage.isNotEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage, color = ErrorColor, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { isLoading = true; errorMessage = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                filteredItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            searchQuery.isNotEmpty() -> "Barang '$searchQuery' tidak ditemukan"
                            selectedCategory != null -> "Tidak ada barang kategori $selectedCategory"
                            else -> "Belum ada barang tersedia"
                        },
                        color = TextLight
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        BrowseItemCard(item = item, onClick = {
                            navController.navigate(Screen.ItemDetail.createRoute(item.id))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseItemCard(item: Item, onClick: () -> Unit) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    }

    fun statusLabel(): Pair<String, androidx.compose.ui.graphics.Color> = when (item.status) {
        Item.STATUS_AVAILABLE -> "Tersedia" to SuccessColor
        Item.STATUS_RENTED -> "Disewa" to WarningColor
        Item.STATUS_MAINTENANCE -> "Perawatan" to ErrorColor
        else -> item.status to TextLight
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        radius = 12.dp
    ) {
        Column(
            modifier = Modifier.clickable { onClick() }.padding(8.dp)
        ) {
            Box {
                val imageModel = remember(item.imageUrl) { ImageUploadHelper.imageModelFromUrl(item.imageUrl) }
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = ContentScale.Crop
                )
                val (label, color) = statusLabel()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(label, color = TechCardBg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                if (item.approvalStatus == Item.APPROVAL_PENDING) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PurpleAccent.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Pending", color = TechCardBg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else if (item.approvalStatus == Item.APPROVAL_REJECTED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ErrorColor.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Ditolak", color = TechCardBg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
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
                text = "${currencyFormat.format(item.price)}/hari",
                style = MaterialTheme.typography.bodySmall,
                color = Primary
            )
            if (item.stock > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stok: ${item.stock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
        }
    }
}
