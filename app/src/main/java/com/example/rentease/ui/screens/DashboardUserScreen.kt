package com.example.rentease.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.BannerItem
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ImageUploadHelper
import com.example.rentease.Item
import com.example.rentease.NotificationHelper
import com.example.rentease.RentalRequest
import com.example.rentease.ui.components.BottomNavTab
import com.example.rentease.ui.components.ExitConfirmDialog
import com.example.rentease.ui.components.UserBottomNavBar
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.BlueDark
import com.example.rentease.ui.theme.BlueLight
import com.example.rentease.ui.theme.ErrorRed
import com.example.rentease.ui.theme.PrimaryBlue
import com.example.rentease.ui.theme.SuccessGreen
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextPrimary
import com.example.rentease.ui.theme.TextSecondary
import com.example.rentease.ui.theme.WarningOrange
import com.example.rentease.ui.theme.White
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

private data class UserMenu(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color = PrimaryBlue,
    val isChat: Boolean = false
)

@Composable
fun DashboardUserScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val authManager = remember { FirebaseAuthManager() }
    val db = remember { FirebaseFirestore.getInstance() }
    val myItems = remember { mutableStateListOf<Item>() }
    var itemsLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val banners = remember { mutableStateListOf<BannerItem>() }
    var bannersLoading by remember { mutableStateOf(true) }
    var incomingCount by remember { mutableStateOf(0) }

    val menus = remember {
        listOf(
            UserMenu("Tambah Barang", Icons.Default.Add, Screen.AddEditItem.createRoute(fromUser = true), SuccessGreen),
            UserMenu("Transaksi Saya", Icons.Default.ShoppingCart, Screen.MyTransactions.route, PrimaryBlue),
            UserMenu("Barang Saya", Icons.Default.Inventory, Screen.MyItems.route, WarningOrange),
            UserMenu("Penyewaan Masuk", Icons.Default.Inbox, Screen.IncomingRentals.route, PrimaryBlue),
            UserMenu("Favorite", Icons.Default.Favorite, Screen.Favorites.route, ErrorRed),
            UserMenu("Riwayat", Icons.Default.History, Screen.History.route, SuccessGreen),
            UserMenu("Bantuan", Icons.AutoMirrored.Filled.Help, Screen.Help.route, PrimaryBlue)
        )
    }

    BackHandler { showExitDialog = true }

    ExitConfirmDialog(
        show = showExitDialog,
        onDismiss = { showExitDialog = false },
        onConfirm = {
            showExitDialog = false
            (context as? android.app.Activity)?.finish()
        }
    )

    LaunchedEffect(Unit) {
        val uid = authManager.getCurrentUserUID() ?: return@LaunchedEffect
        try {
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

        try {
            val bannerSnap = db.collection("banners").whereEqualTo("isActive", true).get().await()
            banners.clear()
            banners.addAll(bannerSnap.documents.mapNotNull { doc ->
                try {
                    BannerItem(
                        id = doc.id,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        title = doc.getString("title") ?: "",
                        itemId = doc.getString("itemId"),
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                } catch (_: Exception) { null }
            })
        } catch (_: Exception) {}
        if (banners.isEmpty()) {
            val topItems = myItems.take(5)
            banners.addAll(topItems.mapIndexed { i, item ->
                BannerItem(
                    id = "item_banner_$i",
                    imageUrl = item.imageUrl,
                    title = item.name,
                    itemId = item.id,
                    isActive = true
                )
            })
        }
        bannersLoading = false
    }

    val incomingListener = remember { mutableStateOf<ListenerRegistration?>(null) }
    val knownRentalIds = remember { mutableStateOf(setOf<String>()) }
    DisposableEffect(Unit) {
        val uid = authManager.getCurrentUserUID()
        if (uid != null) {
            val reg = db.collection("rentals")
                .whereEqualTo("ownerId", uid)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    val pendingDocs = snapshots.documents.filter { doc ->
                        doc.getString("status") == RentalRequest.STATUS_PENDING
                    }
                    val currentIds = pendingDocs.map { it.id }.toSet()
                    val newIds = currentIds - knownRentalIds.value
                    if (newIds.isNotEmpty()) {
                        for (doc in snapshots.documents) {
                            if (doc.id in newIds) {
                                val renterName = doc.getString("renterName") ?: "Seseorang"
                                val itemName = doc.getString("itemName") ?: "barang"
                                val duration = (doc.getLong("duration") ?: 0).toInt()
                                NotificationHelper.showOwnerRentalRequestNotification(
                                    context = context,
                                    renterName = renterName,
                                    itemName = itemName,
                                    duration = duration
                                )
                            }
                        }
                    }
                    knownRentalIds.value = currentIds
                    incomingCount = currentIds.size
                }
            incomingListener.value = reg
        }
        onDispose {
            incomingListener.value?.remove()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchHeader(
            onSearchClick = { navController.navigate(Screen.BrowseItems.route) }
        )

        Column(
            modifier = Modifier.background(PrimaryBlue)
        ) {
            if (!bannersLoading && banners.isNotEmpty()) {
                BannerCarousel(banners = banners)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Layanan",
                style = MaterialTheme.typography.titleSmall,
                color = White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalMenuRow(
                menus = menus,
                incomingCount = incomingCount,
                onMenuClick = { menu ->
                    if (menu.isChat) {
                        navController.navigate(Screen.UserChat.route)
                    } else {
                        if (menu.label == "Penyewaan Masuk") incomingCount = 0
                        navController.navigate(menu.route)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .background(White)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            val categories = remember(myItems.toList()) {
                listOf(null as String?) + myItems.map { it.category }.distinct()
            }
            CategoryChipsRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (itemsLoading) {
                ItemsLoadingShimmer()
            } else if (myItems.isEmpty()) {
                EmptyItemsPlaceholder(
                    onAddItem = { navController.navigate(Screen.AddEditItem.createRoute(fromUser = true)) }
                )
            } else {
                val displayItems = if (selectedCategory == null) myItems
                    else myItems.filter { it.category == selectedCategory }

                if (displayItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada barang di kategori ini",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Semua Barang",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${displayItems.size} barang",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextHint,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(Color(0xFFEEEEEE))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    displayItems.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { item ->
                                ItemCard(
                                    item = item,
                                    onClick = { navController.navigate(Screen.ItemDetail.createRoute(item.id)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        UserBottomNavBar(
            selectedTab = BottomNavTab.HOME,
            onTabSelected = { tab ->
                when (tab) {
                    BottomNavTab.CHAT -> navController.navigate(Screen.UserChat.route)
                    BottomNavTab.PROFILE -> navController.navigate(Screen.ProfileUser.route)
                    BottomNavTab.HOME -> {}
                }
            }
        )
    }
}

@Composable
private fun SearchHeader(onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PrimaryBlue, BlueDark)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(28.dp),
                color = White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cari barang sewaan...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalMenuRow(
    menus: List<UserMenu>,
    incomingCount: Int,
    onMenuClick: (UserMenu) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(menus, key = { it.label }) { menu ->
            val badge = if (menu.label == "Penyewaan Masuk") incomingCount else 0
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clickable { onMenuClick(menu) }
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            menu.icon,
                            contentDescription = menu.label,
                            tint = menu.tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (badge > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ErrorRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (badge > 99) "99+" else badge.toString(),
                                color = White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = menu.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontSize = 11.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
private fun BannerCarousel(banners: List<BannerItem>) {
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (banners.size > 1) {
            while (true) {
                delay(4000)
                currentPage = (currentPage + 1) % banners.size
            }
        }
    }

    val bannerColors = remember {
        listOf(
            listOf(0xFF1565C0, 0xFF0D47A1),
            listOf(0xFF00897B, 0xFF004D40),
            listOf(0xFFE65100, 0xFFBF360C)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        val banner = banners[currentPage]
        val colorIndex = currentPage % bannerColors.size

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(bannerColors[colorIndex][0]),
                            Color(bannerColors[colorIndex][1])
                        )
                    )
                )
        ) {
            if (banner.imageUrl.isNotEmpty()) {
                val imageModel = remember(banner.imageUrl) {
                    ImageUploadHelper.imageModelFromUrl(banner.imageUrl)
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterEnd)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                ) {
                    Text(
                        text = banner.title.substringBefore(" - "),
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 2,
                        letterSpacing = 0.5.sp
                    )
                    if (banner.title.contains(" - ")) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = banner.title.substringAfter(" - "),
                            color = White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            if (banner.imageUrl.isNotEmpty() && banner.title.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = banner.title,
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }

        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                banners.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == currentPage) 22.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index == currentPage) White
                                else White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    categories: List<String?>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            val label = cat ?: "Semua"
            val isSelected = cat == selectedCategory
            Surface(
                modifier = Modifier
                    .clickable { onCategorySelected(cat) }
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PrimaryBlue else BlueLight,
                shadowElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) White else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = White,
        shadowElevation = 3.dp,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(BlueLight),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    val imageModel = remember(item.imageUrl) {
                        ImageUploadHelper.imageModelFromUrl(item.imageUrl)
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        tint = TextHint.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(WarningOrange.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.rentCount}x disewa",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemsLoadingShimmer() {
    val shimmerColors = listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    @Composable
    fun ShimmerBlock(
        modifier: Modifier,
        shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = shimmerColors,
                        start = androidx.compose.ui.geometry.Offset(translateAnim.value - 300, 0f),
                        end = androidx.compose.ui.geometry.Offset(translateAnim.value, 0f)
                    )
                )
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        ShimmerBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ShimmerBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyItemsPlaceholder(onAddItem: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    tint = PrimaryBlue.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Belum ada barang tersedia",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tambah barang Anda sekarang untuk mulai menyewakan",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                onClick = onAddItem,
                shape = RoundedCornerShape(24.dp),
                color = PrimaryBlue
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tambah Barang",
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
