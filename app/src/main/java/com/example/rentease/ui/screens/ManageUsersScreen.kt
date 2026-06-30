package com.example.rentease.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground
import com.example.rentease.ui.components.GlassCard
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.theme.ErrorColor
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.PurpleAccent
import com.example.rentease.ui.theme.TechCardBg
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManageUsersScreen(
    navController: NavHostController,
    onBack: () -> Unit = {},
    filterStaffOnly: Boolean = false
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    var allUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var filteredUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var accessError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    fun applyFilter() {
        val query = searchQuery.trim().lowercase()
        val result = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                val name = (user["name"] as? String ?: "").lowercase()
                val email = (user["email"] as? String ?: "").lowercase()
                name.contains(query) || email.contains(query)
            }
        }
        filteredUsers = result
    }

    LaunchedEffect(Unit) {
        val authManager = FirebaseAuthManager()
        authManager.getUserRole(
            onSuccess = { role ->
                if (role != "admin") {
                    accessError = "Anda tidak memiliki akses ke halaman ini"
                }
            },
            onFailure = { accessError = "Gagal memverifikasi akses" }
        )
    }

    LaunchedEffect(Unit) {
        isLoading = true
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Map<String, Any>>()
                for (doc in snapshot) {
                    val data = doc.data.toMutableMap()
                    val role = data["role"] as? String ?: "user"
                    if (filterStaffOnly && role == "user") continue
                    data["docId"] = doc.id
                    list.add(data)
                }
                list.sortWith(compareByDescending<Map<String, Any>> {
                    val r = it["role"] as? String ?: "user"
                    if (r != "user") 1 else 0
                }.thenBy { (it["name"] as? String ?: "").lowercase() })
                allUsers = list
                filteredUsers = list
                isLoading = false
            }
            .addOnFailureListener {
                errorMessage = "Gagal memuat data pengguna"
                isLoading = false
            }
    }

    LaunchedEffect(searchQuery) {
        applyFilter()
    }

    fun deleteUser() {
        val docId = userToDelete?.get("docId") as? String ?: return
        isDeleting = true
        db.collection("users").document(docId).delete()
            .addOnSuccessListener {
                isDeleting = false
                showDeleteDialog = false
                userToDelete = null
                allUsers = allUsers.filter { it["docId"] != docId }
                applyFilter()
            }
            .addOnFailureListener {
                isDeleting = false
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        if (accessError != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(title = "Akses Ditolak", onBackClick = onBack)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = accessError!!,
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(
                    title = if (filterStaffOnly) "Kelola Staff" else "Kelola Pengguna",
                    onBackClick = onBack
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama atau email...", color = TextHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextHint) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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

                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "${filteredUsers.size} dari ${allUsers.size} pengguna",
                        color = TextLight,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        text = "${allUsers.size} pengguna",
                        color = TextLight,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                        errorMessage != null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = errorMessage!!,
                                    color = ErrorColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        filteredUsers.isEmpty() -> {
                            val msg = if (searchQuery.isNotEmpty()) {
                                "Pengguna '$searchQuery' tidak ditemukan"
                            } else if (filterStaffOnly) {
                                "Belum ada data staff"
                            } else {
                                "Belum ada data pengguna"
                            }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = msg,
                                    color = TextHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                items(filteredUsers, key = { it["docId"] as? String ?: "" }) { user ->
                                    val name = user["name"] as? String ?: "Tanpa Nama"
                                    val email = user["email"] as? String ?: "-"
                                    val phone = user["phone"] as? String ?: ""
                                    val role = user["role"] as? String ?: "user"
                                    val createdAt = user["createdAt"] as? Long

                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = name, style = MaterialTheme.typography.titleSmall, color = TextDark)
                                                Text(text = email, style = MaterialTheme.typography.bodySmall, color = TextLight)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (phone.isNotEmpty()) {
                                                        Text(
                                                            text = phone,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextHint,
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                    }
                                                    if (createdAt != null) {
                                                        Text(
                                                            text = dateFormat.format(Date(createdAt)),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextHint
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            RoleBadge(role = role, textColor = if (role == "admin") PurpleAccent else Primary)
                                            IconButton(onClick = {
                                                userToDelete = user
                                                showDeleteDialog = true
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ErrorColor, modifier = Modifier.size(20.dp))
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

    if (showDeleteDialog && userToDelete != null) {
        val name = userToDelete!!["name"] as? String ?: "Pengguna"
        AlertDialog(
            onDismissRequest = { if (!isDeleting) { showDeleteDialog = false; userToDelete = null } },
            title = { Text("Hapus Pengguna", color = TextDark) },
            text = { Text("Apakah Anda yakin ingin menghapus '$name'? Tindakan ini tidak dapat dibatalkan.", color = TextLight) },
            confirmButton = {
                TextButton(onClick = { deleteUser() }, enabled = !isDeleting) {
                    Text(if (isDeleting) "Menghapus..." else "Ya, Hapus", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; userToDelete = null }) {
                    Text("Batal", color = TextHint)
                }
            },
            containerColor = TechCardBg
        )
    }
}
