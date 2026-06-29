package com.example.rentease.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ui.components.ExitConfirmDialog
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.MenuGridItem
import com.example.rentease.ui.components.NebulaHeader
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.components.StatCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.PrimaryBlue
import com.example.rentease.ui.theme.BlueSoftBg
import com.example.rentease.ui.theme.PurpleAccent
import com.example.rentease.ui.theme.SuccessGreen
import com.example.rentease.ui.theme.TextPrimary
import com.example.rentease.ui.theme.TextSecondary
import com.example.rentease.ui.theme.WarningOrange
import com.example.rentease.ui.theme.White
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private data class AdminMenuItem(val label: String, val icon: ImageVector, val route: String, val tint: Color = PrimaryBlue)

@Composable
fun DashboardAdminScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val authManager = remember { FirebaseAuthManager() }
    val db = remember { FirebaseFirestore.getInstance() }
    var userName by remember { mutableStateOf("Administrator") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var statUsers by remember { mutableStateOf("-") }
    var statTransactions by remember { mutableStateOf("-") }
    var statItems by remember { mutableStateOf("-") }
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
            AdminMenuItem("Cari Barang", Icons.Default.Search, Screen.BrowseItems.route, PrimaryBlue),
            AdminMenuItem("Tambah Barang", Icons.Default.Add, Screen.AddEditItem.createRoute(fromUser = false), PrimaryBlue),
            AdminMenuItem("Kelola User", Icons.Default.Group, Screen.ManageUsers.route, PrimaryBlue),
            AdminMenuItem("Kelola Barang", Icons.Default.Inventory, Screen.ManageItems.route, WarningOrange),
            AdminMenuItem("Verif. Rental", Icons.Default.CheckCircle, Screen.VerifyRental.route, PrimaryBlue),
            AdminMenuItem("Verif. Barang", Icons.Default.Verified, Screen.VerifyUserItems.route, SuccessGreen),
            AdminMenuItem("Pengembalian", Icons.Default.Restore, Screen.ManageReturns.route, PrimaryBlue),
            AdminMenuItem("Laporan", Icons.Default.Report, Screen.ViewReports.route, PurpleAccent),
            AdminMenuItem("Komplain", Icons.Default.SupportAgent, Screen.UserComplaints.route, WarningOrange),
            AdminMenuItem("Layanan", Icons.AutoMirrored.Filled.Help, Screen.CustomerService.route, PrimaryBlue)
        )
    }

    LaunchedEffect(Unit) {
        authManager.getUserData(
            onSuccess = { data -> 
                userName = (data["name"] as? String) ?: "Administrator" 
                profileImageUrl = data["profileImageUrl"] as? String
            },
            onFailure = { userName = "Administrator" }
        )
        try {
            statUsers = db.collection("users").get().await().size().toString()
            statItems = db.collection("items").get().await().size().toString()
            statTransactions = db.collection("rentals").get().await().size().toString()
        } catch (_: Exception) {}
    }

    GalaxyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NebulaHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(White)
                            .clickable { navController.navigate(Screen.ProfileAdmin.route) },
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
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f).clickable { navController.navigate(Screen.ProfileAdmin.route) }
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleLarge,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RoleBadge(role = "Admin", textColor = PrimaryBlue)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(BlueSoftBg)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Person,
                        value = statUsers,
                        label = "Pengguna",
                        iconTint = PrimaryBlue,
                        valueColor = PrimaryBlue
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        value = statTransactions,
                        label = "Transaksi",
                        iconTint = SuccessGreen,
                        valueColor = SuccessGreen
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory,
                        value = statItems,
                        label = "Barang",
                        iconTint = WarningOrange,
                        valueColor = WarningOrange
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Menu Admin",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    color = PrimaryBlue.copy(alpha = 0.15f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
