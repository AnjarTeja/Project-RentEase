package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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

class FavoritesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvFavorites: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout

    private val favoriteItems = mutableListOf<Item>()
    private val favoriteIds = mutableListOf<String>()
    private lateinit var adapter: FavoritesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.favorites_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvFavorites = findViewById(R.id.rv_favorites)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        adapter = FavoritesAdapter(
            items = favoriteItems,
            onItemClick = { item ->
                val intent = Intent(this, ItemDetailActivity::class.java)
                intent.putExtra("ITEM_ID", item.id)
                startActivity(intent)
            },
            onRemoveFavorite = { item ->
                removeFavorite(item.id)
            }
        )

        rvFavorites.layoutManager = LinearLayoutManager(this)
        rvFavorites.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        val uid = auth.currentUser?.uid ?: return

        progressBar.visibility = android.view.View.VISIBLE
        rvFavorites.visibility = android.view.View.GONE
        emptyState.visibility = android.view.View.GONE

        db.collection("favorites")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                favoriteIds.clear()
                val itemIds = mutableListOf<String>()
                for (doc in snapshot) {
                    val itemId = doc.getString("itemId") ?: continue
                    itemIds.add(itemId)
                    favoriteIds.add(doc.id)
                }

                if (itemIds.isEmpty()) {
                    showEmpty()
                    return@addOnSuccessListener
                }

                loadFavoriteItems(itemIds)
            }
            .addOnFailureListener {
                loadFavoritesFallback(uid)
            }
    }

    private fun loadFavoritesFallback(uid: String) {
        db.collection("favorites")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                favoriteIds.clear()
                val itemIds = mutableListOf<String>()
                val sorted = snapshot.sortedByDescending { it.getLong("createdAt") ?: 0L }
                for (doc in sorted) {
                    val itemId = doc.getString("itemId") ?: continue
                    itemIds.add(itemId)
                    favoriteIds.add(doc.id)
                }
                if (itemIds.isEmpty()) showEmpty()
                else loadFavoriteItems(itemIds)
            }
            .addOnFailureListener { showEmpty() }
    }

    private fun loadFavoriteItems(itemIds: List<String>) {
        favoriteItems.clear()

        if (itemIds.isEmpty()) {
            showEmpty()
            return
        }

        var loaded = 0
        for (itemId in itemIds) {
            db.collection("items").document(itemId).get()
                .addOnSuccessListener { doc ->
                    loaded++
                    if (doc.exists()) {
                        try {
                            val item = Item(
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
                                category = doc.getString("category") ?: Item.CATEGORY_OTHER
                            )
                            favoriteItems.add(item)
                        } catch (e: Exception) { /* skip */ }
                    }
                    if (loaded >= itemIds.size) finishLoading()
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded >= itemIds.size) finishLoading()
                }
        }
    }

    private fun finishLoading() {
        progressBar.visibility = android.view.View.GONE
        adapter.updateData(favoriteItems)

        if (favoriteItems.isEmpty()) {
            showEmpty()
        } else {
            rvFavorites.visibility = android.view.View.VISIBLE
            emptyState.visibility = android.view.View.GONE
        }
    }

    private fun showEmpty() {
        progressBar.visibility = android.view.View.GONE
        rvFavorites.visibility = android.view.View.GONE
        emptyState.visibility = android.view.View.VISIBLE
    }

    private fun removeFavorite(favId: String) {
        db.collection("favorites")
            .whereEqualTo("itemId", favId)
            .whereEqualTo("userId", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    db.collection("favorites").document(doc.id).delete()
                }
                Toast.makeText(this, "Dihapus dari favorit", Toast.LENGTH_SHORT).show()
                loadFavorites()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus favorit", Toast.LENGTH_SHORT).show()
            }
    }
}
