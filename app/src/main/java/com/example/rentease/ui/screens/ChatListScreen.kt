package com.example.rentease.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rentease.Chat
import com.example.rentease.FirebaseAuthManager
import com.example.rentease.ui.components.AppToolbar
import com.example.rentease.ui.components.GalaxyBackground

import com.example.rentease.ui.components.GlowCard
import com.example.rentease.ui.components.RoleBadge
import com.example.rentease.ui.navigation.Screen
import com.example.rentease.ui.theme.Primary
import com.example.rentease.ui.theme.TextDark
import com.example.rentease.ui.theme.TextHint
import com.example.rentease.ui.theme.TextLight
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val authManager = remember { FirebaseAuthManager() }
    val chatList = remember { mutableStateListOf<Chat>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = authManager.getCurrentUserUID() ?: return@LaunchedEffect

        val allChats = mutableListOf<Chat>()
        var renterLoaded = false
        var ownerLoaded = false

        fun finishIfDone() {
            if (renterLoaded && ownerLoaded) {
                allChats.sortByDescending { it.lastMessageTime }
                chatList.clear()
                chatList.addAll(allChats)
                isLoading = false
            }
        }

        db.collection("chats")
            .whereEqualTo("renterId", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    allChats.add(
                        Chat(
                            id = doc.id,
                            rentalId = doc.getString("rentalId") ?: "",
                            itemId = doc.getString("itemId") ?: "",
                            itemName = doc.getString("itemName") ?: "",
                            renterId = doc.getString("renterId") ?: "",
                            renterName = doc.getString("renterName") ?: "",
                            ownerId = doc.getString("ownerId") ?: "",
                            ownerName = doc.getString("ownerName") ?: "",
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    )
                }
                renterLoaded = true
                finishIfDone()
            }
            .addOnFailureListener {
                renterLoaded = true
                finishIfDone()
            }

        db.collection("chats")
            .whereEqualTo("ownerId", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    val exists = allChats.any { it.id == doc.id }
                    if (!exists) {
                        allChats.add(
                            Chat(
                                id = doc.id,
                                rentalId = doc.getString("rentalId") ?: "",
                                itemId = doc.getString("itemId") ?: "",
                                itemName = doc.getString("itemName") ?: "",
                                renterId = doc.getString("renterId") ?: "",
                                renterName = doc.getString("renterName") ?: "",
                                ownerId = doc.getString("ownerId") ?: "",
                                ownerName = doc.getString("ownerName") ?: "",
                                lastMessage = doc.getString("lastMessage") ?: "",
                                lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        )
                    }
                }
                ownerLoaded = true
                finishIfDone()
            }
            .addOnFailureListener {
                ownerLoaded = true
                finishIfDone()
            }
    }

    GalaxyBackground(starAlpha = 0.3f) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(title = "Chat", onBackClick = onBack)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = TextLight)
                }
            } else if (chatList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada chat", color = TextLight)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatList) { chat ->
                        val uid = authManager.getCurrentUserUID() ?: ""
                        val contactName = if (chat.renterId == uid) chat.ownerName else chat.renterName

                        GlowCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.Chat.createRoute(chat.id))
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = contactName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextDark
                                        )
                                        Text(
                                            text = if (chat.lastMessageTime > 0) {
                                                SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(chat.lastMessageTime))
                                            } else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHint
                                        )
                                    }
                                    Text(
                                        text = chat.itemName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Primary
                                    )
                                    Text(
                                        text = chat.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextLight,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
