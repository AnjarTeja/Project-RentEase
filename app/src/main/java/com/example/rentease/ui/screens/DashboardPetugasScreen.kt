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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ui.components.ExitConfirmDialog
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.MenuGridItem
import com.example.rentease.ui.components.NebulaHeader
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.components.StatCard
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PrimaryLight
import com.example.rentease.ui.theme.PurpleAccent
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.example.rentease.ui.theme.WarningColor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private data class PetugasMenuItem(val label: String, val icon: ImageVector, val route: String, val tint: Color = Primary)

@Composable
fun DashboardPetugasScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val authManager = remember { FirebaseAuthManager() }
    val db = remember { FirebaseFirestore.getInstance() }
    var userName by remember { mutableStateOf("Petugas") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var statsPending by remember { mutableStateOf("-") }
    var statsApproved by remember { mutableStateOf("-") }
    var statsItems by remember { mutableStateOf("-") }
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
            PetugasMenuItem("Semua Barang", Icons.Default.Search, Screen.AllItems.route, Primary),
            PetugasMenuItem("Tambah Barang", Icons.Default.Add, Screen.AddEditItem.createRoute(fromUser = false), Primary),
            PetugasMenuItem("Verif. Rental", Icons.Default.CheckCircle, Screen.VerifyRental.route, Primary),
            PetugasMenuItem("Kelola Barang", Icons.Default.Inventory, Screen.ManageItems.route, WarningColor),
            PetugasMenuItem("Verif. Barang", Icons.Default.Verified, Screen.VerifyUserItems.route, SuccessColor),
            PetugasMenuItem("Pengembalian", Icons.Default.Restore, Screen.ManageReturns.route, Primary),
            PetugasMenuItem("Laporan", Icons.Default.RateReview, Screen.ViewReports.route, PurpleAccent),
            PetugasMenuItem("Layanan", Icons.AutoMirrored.Filled.Help, Screen.CustomerService.route, Primary)
        )
    }

    LaunchedEffect(Unit) {
        authManager.getUserData(
            onSuccess = { data -> 
                userName = (data["name"] as? String) ?: "Petugas" 
                profileImageUrl = data["profileImageUrl"] as? String
            },
            onFailure = { userName = "Petugas" }
        )
        try {
            val rentals = db.collection("rentals").get().await()
            val statuses = rentals.documents.mapNotNull { it.getString("status") }
            statsPending = statuses.count { it == "pending" }.toString()
            statsApproved = statuses.count { it == "approved" }.toString()
            statsItems = db.collection("items").get().await().size().toString()
        } catch (_: Exception) {}
    }

    GalaxyBackground(starAlpha = 0.4f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                            .background(TechCardBg)
                            .clickable { navController.navigate(Screen.ProfilePetugas.route) },
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
                        modifier = Modifier.weight(1f).clickable { navController.navigate(Screen.ProfilePetugas.route) }
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RoleBadge(role = "Petugas", textColor = Primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    icon = Icons.Default.CheckCircle,
                    value = statsApproved,
                    label = "Disetujui",
                    iconTint = SuccessColor,
                    valueColor = SuccessColor
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Inventory,
                    value = statsItems,
                    label = "Barang",
                    iconTint = Primary,
                    valueColor = Primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Menu Petugas",
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
    }
}
