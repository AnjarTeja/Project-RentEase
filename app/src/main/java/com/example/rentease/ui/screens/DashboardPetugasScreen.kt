package com.example.rentease.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ui.components.MenuGridItem
import com.example.rentease.ui.components.StatItem
import com.example.rentease.R
import com.example.rentease.ui.components.ExitConfirmDialog
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.theme.TechDarkBg
import com.example.rentease.ui.components.NebulaHeader
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PurpleAccent
import com.example.rentease.ui.theme.SuccessColor
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
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
            onSuccess = { data -> userName = (data["name"] as? String) ?: "Petugas" },
            onFailure = { userName = "Petugas" }
        )
        try {
            statsPending = db.collection("rentals").whereEqualTo("status", "pending").get().await().size().toString()
            statsApproved = db.collection("rentals").whereEqualTo("status", "approved").get().await().size().toString()
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TechCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextLight, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = userName, style = MaterialTheme.typography.titleLarge, color = TextDark)
                        RoleBadge(role = "Petugas", textColor = Primary)
                    }
                    Button(
                        onClick = { navController.navigate(Screen.ProfilePetugas.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Profil", color = TechDarkBg)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(value = statsPending, label = "Pending", color = WarningColor, modifier = Modifier.weight(1f))
                StatItem(value = statsApproved, label = "Disetujui", color = SuccessColor, modifier = Modifier.weight(1f))
                StatItem(value = statsItems, label = "Barang", color = Primary, modifier = Modifier.weight(1f))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(menus) { menu ->
                    MenuGridItem(
                        icon = menu.icon,
                        label = menu.label,
                        tint = menu.tint,
                        onClick = { navController.navigate(menu.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
