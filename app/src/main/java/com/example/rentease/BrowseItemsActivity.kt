package com.example.rentease

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class BrowseItemsActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "BrowseItemsActivity"

    private lateinit var rvItems: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: BrowseItemAdapter
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton

    private val allItemList = mutableListOf<Item>()
    private val itemList = mutableListOf<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_browse_items)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.browse_items_root)) { v, insets ->
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
        loadAvailableItems()
    }

    private fun initializeViews() {
        rvItems = findViewById(R.id.rv_browse_items)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        etSearch = findViewById(R.id.et_search_items)
        btnClearSearch = findViewById(R.id.btn_clear_search)
    }

    private fun setupRecyclerView() {
        adapter = BrowseItemAdapter(itemList) { item ->
            val intent = Intent(this, ItemDetailActivity::class.java)
            intent.putExtra("ITEM_ID", item.id)
            startActivity(intent)
        }

        rvItems.layoutManager = GridLayoutManager(this, 2)
        rvItems.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
        
        // Search functionality
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    btnClearSearch.visibility = View.VISIBLE
                } else {
                    btnClearSearch.visibility = View.GONE
                }
                filterItems(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            etSearch.clearFocus()
        }
    }

    private fun filterItems(query: String) {
        itemList.clear()
        if (query.isEmpty()) {
            itemList.addAll(allItemList)
        } else {
            val lowerCaseQuery = query.lowercase()
            for (item in allItemList) {
                if (item.name.lowercase().contains(lowerCaseQuery) || 
                    item.description.lowercase().contains(lowerCaseQuery)) {
                    itemList.add(item)
                }
            }
        }
        adapter.updateData(itemList)
        
        if (itemList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_empty_message)?.text = "Barang '$query' tidak ditemukan"
        } else {
            emptyState.visibility = View.GONE
        }
    }

    private fun loadAvailableItems() {
        progressBar.visibility = View.VISIBLE
        rvItems.visibility = View.GONE
        emptyState.visibility = View.GONE

        firestore.collection("items")
            .whereEqualTo("status", Item.STATUS_AVAILABLE)
            .get()
            .addOnSuccessListener { documents ->
                allItemList.clear()
                itemList.clear()

                for (doc in documents) {
                    try {
                        val approval = doc.getString("approvalStatus")
                        
                        // Show if approved OR if the field is missing (for legacy data)
                        if (approval == null || approval == Item.APPROVAL_APPROVED) {
                            val item = Item(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                ownerId = doc.getString("ownerId") ?: "",
                                status = doc.getString("status") ?: Item.STATUS_AVAILABLE,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                approvalStatus = approval ?: Item.APPROVAL_APPROVED,
                                rentCount = (doc.getLong("rentCount") ?: 0L).toInt(),
                                stock = (doc.getLong("stock") ?: 1L).toInt()
                            )
                            allItemList.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing item: ${e.message}")
                    }
                }

                // Sort by most popular first
                allItemList.sortByDescending { it.rentCount }
                
                itemList.addAll(allItemList)

                adapter.updateData(itemList)
                progressBar.visibility = View.GONE

                if (itemList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                } else {
                    rvItems.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat barang", Toast.LENGTH_SHORT).show()
                emptyState.visibility = View.VISIBLE
            }
    }
}
