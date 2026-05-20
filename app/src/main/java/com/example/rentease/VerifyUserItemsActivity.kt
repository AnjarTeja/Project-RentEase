package com.example.rentease

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class VerifyUserItemsActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "VerifyUserItems"

    private lateinit var rvItems: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: LinearLayout
    private lateinit var backButton: ImageButton

    private lateinit var adapter: VerifyUserItemAdapter
    private val itemList = mutableListOf<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_user_items)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verify_user_items_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadPendingItems()
    }

    private fun initializeViews() {
        rvItems = findViewById(R.id.rv_verify_items)
        progressBar = findViewById(R.id.progress_bar)
        emptyStateView = findViewById(R.id.empty_state_view)
        backButton = findViewById(R.id.back_button)
    }

    private fun setupRecyclerView() {
        adapter = VerifyUserItemAdapter(
            itemList = itemList,
            onApproveClick = { item -> showApproveConfirmDialog(item) },
            onRejectClick = { item -> showRejectConfirmDialog(item) }
        )
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = adapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
    }

    private fun loadPendingItems() {
        progressBar.visibility = View.VISIBLE
        rvItems.visibility = View.GONE
        emptyStateView.visibility = View.GONE

        firestore.collection("items")
            .whereEqualTo("approvalStatus", Item.APPROVAL_PENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                itemList.clear()
                for (doc in documents) {
                    val item = doc.toObject(Item::class.java).copy(id = doc.id)
                    itemList.add(item)
                }

                progressBar.visibility = View.GONE
                if (itemList.isEmpty()) {
                    emptyStateView.visibility = View.VISIBLE
                } else {
                    rvItems.visibility = View.VISIBLE
                }
                adapter.updateData(itemList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading items: ", e)
                progressBar.visibility = View.GONE
                emptyStateView.visibility = View.VISIBLE
                
                // Fallback if index is missing for orderBy
                if (e.message?.contains("FAILED_PRECONDITION") == true) {
                    fetchWithoutOrderFallback()
                } else {
                    Toast.makeText(this, "Gagal memuat data pengajuan", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchWithoutOrderFallback() {
        firestore.collection("items")
            .whereEqualTo("approvalStatus", Item.APPROVAL_PENDING)
            .get()
            .addOnSuccessListener { documents ->
                itemList.clear()
                for (doc in documents) {
                    val item = doc.toObject(Item::class.java).copy(id = doc.id)
                    itemList.add(item)
                }
                
                // Sort locally
                itemList.sortByDescending { it.createdAt }

                if (itemList.isEmpty()) {
                    emptyStateView.visibility = View.VISIBLE
                } else {
                    rvItems.visibility = View.VISIBLE
                }
                adapter.updateData(itemList)
            }
    }

    private fun showApproveConfirmDialog(item: Item) {
        DialogUtils.showDangerDialog(
            activity = this,
            title = "Setujui Barang",
            message = "Barang '${item.name}' akan ditambahkan ke katalog publik dan bisa dilihat oleh semua penyewa. Lanjutkan?",
            positiveButtonText = "Setujui",
            iconRes = R.drawable.ic_check,
            iconBgColorRes = R.drawable.bg_icon_circle_green
        ) {
            updateItemStatus(item, Item.APPROVAL_APPROVED)
        }
    }

    private fun showRejectConfirmDialog(item: Item) {
        DialogUtils.showDangerDialog(
            activity = this,
            title = "Tolak Barang",
            message = "Apakah Anda yakin ingin menolak pengajuan barang '${item.name}'? Data barang akan dihapus dari sistem pengajuan.",
            positiveButtonText = "Tolak",
            iconRes = R.drawable.ic_close,
            iconBgColorRes = R.drawable.bg_icon_circle_orange
        ) {
            updateItemStatus(item, Item.APPROVAL_REJECTED)
        }
    }

    private fun updateItemStatus(item: Item, newStatus: String) {
        progressBar.visibility = View.VISIBLE
        
        if (newStatus == Item.APPROVAL_REJECTED) {
            // Delete the item completely if rejected
            firestore.collection("items").document(item.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Pengajuan barang ditolak", Toast.LENGTH_SHORT).show()
                    loadPendingItems()
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Gagal menolak barang", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Update to approved
            firestore.collection("items").document(item.id)
                .update("approvalStatus", newStatus)
                .addOnSuccessListener {
                    Toast.makeText(this, "Barang berhasil disetujui (Live)", Toast.LENGTH_SHORT).show()
                    loadPendingItems()
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Gagal menyetujui barang", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
