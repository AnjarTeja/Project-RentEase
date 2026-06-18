package com.example.rentease.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.Item
import com.example.rentease.ui.components.AppToolbar
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }
    var sortOption by remember { mutableIntStateOf(0) }
    var sortExpanded by remember { mutableStateOf(false) }
    val sortLabels = listOf("Populer", "Termurah", "Termahal", "Terbaru")

    fun applyFilters() {
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

    LaunchedEffect(Unit) {
        db.collection("items")
            .whereEqualTo("status", Item.STATUS_AVAILABLE)
            .get()
            .addOnSuccessListener { docs ->
                val items = mutableListOf<Item>()
                for (doc in docs) {
                    val approval = doc.getString("approvalStatus")
                    if (approval == null || approval == Item.APPROVAL_APPROVED) {
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
                                approvalStatus = approval ?: Item.APPROVAL_APPROVED,
                                rentCount = (doc.getLong("rentCount") ?: 0L).toInt(),
                                stock = (doc.getLong("stock") ?: 1L).toInt(),
                                category = doc.getString("category") ?: Item.CATEGORY_OTHER
                            )
                        )
                    }
                }
                allItems.clear()
                allItems.addAll(items.sortedByDescending { it.rentCount })
                isLoading = false
                applyFilters()
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Gagal memuat barang: ${e.message}"
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Cari Barang", onBackClick = onBack)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; applyFilters() },
                    placeholder = { Text("Cari barang...", color = TextHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextHint) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; applyFilters() }) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null; applyFilters() },
                        label = { Text("Semua", color = TextDark) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.3f))
                    )
                    Item.CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat; applyFilters() },
                            label = { Text(cat, color = TextDark) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.3f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceMin,
                        onValueChange = { priceMin = it; applyFilters() },
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
                        onValueChange = { priceMax = it; applyFilters() },
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

                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = it }
                ) {
                    OutlinedTextField(
                        value = sortLabels.getOrElse(sortOption) { "Populer" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Urutkan", color = TextHint) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint.copy(alpha = 0.3f),
                            cursorColor = Primary,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        sortLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { sortOption = index; sortExpanded = false; applyFilters() }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (errorMessage.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = ErrorColor)
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotEmpty()) "Barang '$searchQuery' tidak ditemukan"
                        else "Tidak ada barang di kategori ini",
                        color = TextLight
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { item ->
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        radius = 12.dp
    ) {
        Column(
            modifier = Modifier.clickable { onClick() }.padding(8.dp)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier.fillMaxWidth().height(120.dp),
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
        }
    }
}
