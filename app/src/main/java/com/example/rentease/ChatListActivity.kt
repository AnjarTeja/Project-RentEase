package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvChats: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout

    private val chatList = mutableListOf<Chat>()
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_list_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvChats = findViewById(R.id.rv_chats)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        adapter = ChatListAdapter(chatList) { chat ->
            val uid = auth.currentUser?.uid ?: return@ChatListAdapter
            val contactName = if (chat.renterId == uid) chat.ownerName else chat.renterName

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHAT_ID", chat.id)
            intent.putExtra("ITEM_NAME", chat.itemName)
            intent.putExtra("CONTACT_NAME", contactName)
            startActivity(intent)
        }

        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = adapter

        loadChats()
    }

    private fun loadChats() {
        val uid = auth.currentUser?.uid ?: return

        progressBar.visibility = android.view.View.VISIBLE
        rvChats.visibility = android.view.View.GONE
        emptyState.visibility = android.view.View.GONE

        db.collection("chats")
            .whereEqualTo("renterId", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                chatList.clear()
                for (doc in snapshots) {
                    val chat = Chat(
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
                    chatList.add(chat)
                }
                loadOwnerChats(uid)
            }
            .addOnFailureListener {
                loadOwnerChats(uid)
            }
    }

    private fun loadOwnerChats(uid: String) {
        db.collection("chats")
            .whereEqualTo("ownerId", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    val existing = chatList.any { it.id == doc.id }
                    if (!existing) {
                        val chat = Chat(
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
                        chatList.add(chat)
                    }
                }
                chatList.sortByDescending { it.lastMessageTime }
                finishLoading()
            }
            .addOnFailureListener {
                finishLoading()
            }
    }

    private fun finishLoading() {
        progressBar.visibility = android.view.View.GONE
        adapter.updateData(chatList)

        if (chatList.isEmpty()) {
            emptyState.visibility = android.view.View.VISIBLE
            rvChats.visibility = android.view.View.GONE
        } else {
            emptyState.visibility = android.view.View.GONE
            rvChats.visibility = android.view.View.VISIBLE
        }
    }
}
