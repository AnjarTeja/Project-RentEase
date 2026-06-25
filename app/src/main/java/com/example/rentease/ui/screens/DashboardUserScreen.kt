package com.example.rentease.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.Item
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.CategoryFilterChips
import com.example.rentease.ui.components.ExitConfirmDialog
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.MenuGridItem
import com.example.rentease.ui.components.NebulaHeader
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.components.StatCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PrimaryLight
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TechDarkBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

private data class UserMenuItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color = Primary
)

@Composable
fun DashboardUserScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val authManager = remember { FirebaseAuthManager() }
    val db = remember { FirebaseFirestore.getInstance() }
    var userName by remember { mutableStateOf("Pengguna") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var statsActive by remember { mutableStateOf("-") }
    var statsPending by remember { mutableStateOf("-") }
    var statsCompleted by remember { mutableStateOf("-") }
    val myItems = remember { mutableStateListOf<Item>() }
    var itemsLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler { showExitDialog = true }

    ExitConfirmDialog(
        show = showExitDialog,
        onDismiss = { showExitDialog = false },
        onConfirm = {
            showExitDialog = false
            (context as? android.app.Activity)?.finish()
        }
    )

    val menus = remember {
        listOf(
            UserMenuItem("Cari Barang", Icons.Default.Search, Screen.BrowseItems.route, Primary),
            UserMenuItem("Tambah Barang", Icons.Default.Add, Screen.AddEditItem.createRoute(fromUser = true), SuccessColor),
            UserMenuItem("Transaksi Saya", Icons.Default.ShoppingCart, Screen.MyTransactions.route, Primary),
            UserMenuItem("Barang Saya", Icons.Default.Inventory, Screen.MyItems.route, WarningColor),
            UserMenuItem("Penyewaan Masuk", Icons.Default.Inbox, Screen.IncomingRentals.route, Primary),
            UserMenuItem("Favorite", Icons.Default.Favorite, Screen.Favorites.route, ErrorColor),
            UserMenuItem("Chat", Icons.AutoMirrored.Filled.Chat, Screen.ChatList.route, Primary),
            UserMenuItem("Riwayat", Icons.Default.History, Screen.History.route, SuccessColor),
            UserMenuItem("Bantuan", Icons.AutoMirrored.Filled.Help, Screen.Help.route, Primary)
        )
    }

    LaunchedEffect(Unit) {
        authManager.getUserData(
            onSuccess = { data -> 
                userName = (data["name"] as? String) ?: "Pengguna" 
                profileImageUrl = data["profileImageUrl"] as? String
            },
            onFailure = { userName = "Pengguna" }
        )
        val uid = authManager.getCurrentUserUID() ?: return@LaunchedEffect
        try {
            val rentals = db.collection("rentals").whereEqualTo("renterId", uid).get().await()
            val all = rentals.documents.mapNotNull {
                it.getString("status")
            }
            statsPending = all.count { it == RentalRequest.STATUS_PENDING }.toString()
            statsActive = all.count { it == RentalRequest.STATUS_APPROVED || it == RentalRequest.STATUS_RETURN_PENDING }.toString()
            statsCompleted = all.count { it == RentalRequest.STATUS_RETURNED || it == RentalRequest.STATUS_REJECTED }.toString()

            val snap = db.collection("items").limit(50).get().await()
            myItems.clear()
            myItems.addAll(snap.documents.mapNotNull { doc ->
                val st = doc.getString("status") ?: ""
                val app = doc.getString("approvalStatus") ?: ""
                if (st != Item.STATUS_AVAILABLE || app != Item.APPROVAL_APPROVED) return@mapNotNull null
                try {
                    Item(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        ownerId = doc.getString("ownerId") ?: "",
                        status = st,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        approvalStatus = app,
                        rentCount = (doc.getLong("rentCount") ?: 0).toInt(),
                        stock = (doc.getLong("stock") ?: 1).toInt(),
                        category = doc.getString("category") ?: Item.CATEGORY_CAMERA
                    )
                } catch (_: Exception) { null }
            })
            myItems.sortByDescending { it.rentCount }
        } catch (_: Exception) {}
        itemsLoading = false
    }

    GalaxyBackground(starAlpha = 0.4f) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ====== FIXED TOP SECTION ======
            Column {
                NebulaHeader(bottomRadius = 24.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.dp, Primary.copy(alpha = 0.5f), CircleShape)
                                .background(TechCardBg)
                                .clickable { navController.navigate(Screen.ProfileUser.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profileImageUrl.isNullOrBlank()) {
                                val imageModel = androidx.compose.runtime.remember(profileImageUrl) {
                                    val url = profileImageUrl
                                    if (url != null && url.startsWith("data:image") && url.contains("base64,")) {
                                        try {
                                            val base64String = url.substringAfter("base64,")
                                            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                        } catch (e: Exception) { url }
                                    } else { url }
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Foto Profil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = androidx.compose.ui.res.painterResource(com.example.rentease.R.drawable.ic_launcher_foreground),
                                    error = androidx.compose.ui.res.painterResource(com.example.rentease.R.drawable.ic_launcher_foreground)
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f).clickable { navController.navigate(Screen.ProfileUser.route) }
                        ) {
                            Text(
                                text = "Hai, $userName",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            RoleBadge(role = "Pengguna")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        value = statsActive,
                        label = "Aktif",
                        iconTint = Primary,
                        valueColor = Primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.History,
                        value = statsPending,
                        label = "Pending",
                        iconTint = WarningColor,
                        valueColor = WarningColor
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Check,
                        value = statsCompleted,
                        label = "Selesai",
                        iconTint = SuccessColor,
                        valueColor = SuccessColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                menus.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { menu ->
                            MenuGridItem(
                                icon = menu.icon,
                                label = menu.label,
                                tint = menu.tint,
                                onClick = { navController.navigate(menu.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = Primary.copy(alpha = 0.15f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ====== SCROLLABLE BOTTOM SECTION ======
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barang Tersedia",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (!itemsLoading) {
                            val itemCount = if (selectedCategory == null) myItems.size
                                else myItems.count { it.category == selectedCategory }
                            Text(
                                text = "$itemCount item",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CategoryFilterChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (itemsLoading) {
                        // Loading shimmer
                        repeat(3) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                                radius = 12.dp
                            ) {
                                Row(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 80.dp, height = 80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TechCardBg.copy(alpha = 0.5f))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .width(120.dp)
                                                .height(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(TechCardBg.copy(alpha = 0.5f))
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(80.dp)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(TechCardBg.copy(alpha = 0.5f))
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(60.dp)
                                                .height(22.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(TechCardBg.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    } else if (myItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextHint,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Belum ada barang tersedia",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Belum ada barang yang bisa disewa saat ini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextHint,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 48.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                GlassCard(
                                    modifier = Modifier.clickable { navController.navigate(Screen.AddEditItem.createRoute(fromUser = true)) },
                                    radius = 12.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tambah Barang Sewaan", color = Primary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    } else {
                        val displayItems = if (selectedCategory == null) myItems
                            else myItems.filter { it.category == selectedCategory }
                        displayItems.forEach { item ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 10.dp)
                                    .clickable { navController.navigate(Screen.ItemDetail.createRoute(item.id)) },
                                radius = 12.dp
                            ) {
                                Row(modifier = Modifier.padding(8.dp)) {
                                    if (item.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = item.name,
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 72.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 72.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(TechCardBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Inventory, contentDescription = null, tint = TextHint, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextDark,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.price)} /hari",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.rentCount}x disewa",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHint
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        RoleBadge(
                                            role = when (item.status) {
                                                Item.STATUS_AVAILABLE -> "Tersedia"
                                                Item.STATUS_RENTED -> "Disewa"
                                                Item.STATUS_MAINTENANCE -> "Perbaikan"
                                                else -> item.status
                                            },
                                            textColor = when (item.status) {
                                                Item.STATUS_AVAILABLE -> SuccessColor
                                                Item.STATUS_RENTED -> WarningColor
                                                Item.STATUS_MAINTENANCE -> ErrorColor
                                                else -> TextLight
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
