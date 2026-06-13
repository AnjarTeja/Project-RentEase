package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyItemsActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val itemList = mutableListOf<Item>()
    private val originalItemList = mutableListOf<Item>() // For search filtering
    private lateinit var adapter: ItemAdapter

    private lateinit var backButton: ImageButton
    private lateinit var searchView: SearchView
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var addItemButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var tvEmptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_items)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.my_items_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadItemsFromFirestore()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.back_button)
        searchView = findViewById(R.id.search_view)
        itemsRecyclerView = findViewById(R.id.items_list_view)
        addItemButton = findViewById(R.id.add_item_button)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter(
            context = this,
            itemList = itemList,
            onEditClick = { item -> editSelectedItem(item) },
            onDeleteClick = { item -> confirmDelete(item) }
        )
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterItems(newText)
                return true
            }
        })

        // Add item button
        addItemButton.setOnClickListener {
            addNewItem()
        }
    }

    private fun loadItemsFromFirestore() {
        val uid = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        itemsRecyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE

        firestore.collection("items")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { documents ->
                itemList.clear()
                originalItemList.clear()
                
                val unsortedItems = mutableListOf<Item>()
                for (doc in documents) {
                    try {
                        unsortedItems.add(Item(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            ownerId = doc.getString("ownerId") ?: "",
                            status = doc.getString("status") ?: Item.STATUS_AVAILABLE,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        ))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val sortedItems = unsortedItems.sortedByDescending { it.createdAt }
                
                itemList.addAll(sortedItems)
                originalItemList.addAll(sortedItems)

                adapter.notifyDataSetChanged()
                
                progressBar.visibility = View.GONE
                if (itemList.isEmpty()) {
                    showEmptyState("Anda belum memiliki barang")
                } else {
                    itemsRecyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                showEmptyState("Gagal memuat barang: ${exception.localizedMessage}")
            }
    }

    private fun filterItems(query: String?) {
        if (query.isNullOrEmpty()) {
            adapter.updateData(originalItemList)
            if (originalItemList.isEmpty()) showEmptyState("Anda belum memiliki barang")
            else {
                emptyState.visibility = View.GONE
                itemsRecyclerView.visibility = View.VISIBLE
            }
            return
        }

        val filteredList = originalItemList.filter { 
            it.name.contains(query, ignoreCase = true) 
        }
        
        adapter.updateData(filteredList)
        
        if (filteredList.isEmpty()) {
            itemsRecyclerView.visibility = View.GONE
            showEmptyState("Tidak ditemukan barang \"$query\"")
        } else {
            emptyState.visibility = View.GONE
            itemsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(message: String) {
        emptyState.visibility = View.VISIBLE
        tvEmptyMessage.text = message
    }

    private fun addNewItem() {
        val intent = Intent(this, AddEditItemActivity::class.java).apply {
            putExtra("IS_USER", true)
        }
        startActivity(intent)
    }

    private fun editSelectedItem(item: Item) {
        val intent = Intent(this, AddEditItemActivity::class.java).apply {
            putExtra("ITEM_ID", item.id)
            putExtra("IS_USER", true)
        }
        startActivity(intent)
    }

    private fun confirmDelete(item: Item) {
        // Check for active or pending rentals before allowing deletion
        firestore.collection("rentals")
            .whereEqualTo("itemId", item.id)
            .whereIn("status", listOf("pending", "approved"))
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    showDeleteDialog(item)
                } else {
                    Toast.makeText(
                        this,
                        "Tidak bisa menghapus '${item.name}' karena masih ada ${snapshot.size()} transaksi aktif/pending.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                firestore.collection("rentals")
                    .whereEqualTo("itemId", item.id)
                    .get()
                    .addOnSuccessListener { allRentals ->
                        val activeCount = allRentals.documents.count {
                            val s = it.getString("status")
                            s == "pending" || s == "approved"
                        }
                        if (activeCount == 0) {
                            showDeleteDialog(item)
                        } else {
                            Toast.makeText(
                                this,
                                "Tidak bisa menghapus '${item.name}' karena masih ada $activeCount transaksi aktif/pending.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        showDeleteDialog(item)
                    }
            }
    }

    private fun showDeleteDialog(item: Item) {
        DialogUtils.showDangerDialog(
            activity = this,
            title = "Hapus Barang",
            message = "Apakah Anda yakin ingin menghapus '${item.name}'? Tindakan ini tidak dapat dibatalkan.",
            positiveButtonText = "Ya, Hapus"
        ) {
            deleteItem(item)
        }
    }

    private fun deleteItem(item: Item) {
        progressBar.visibility = View.VISIBLE
        
        firestore.collection("items").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadItemsFromFirestore() // Refresh list
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal menghapus barang", Toast.LENGTH_SHORT).show()
            }
    }
}
