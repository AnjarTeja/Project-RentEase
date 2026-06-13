package com.example.rentease

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class VerifyRentalActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val rentalList = mutableListOf<RentalRequest>()
    private lateinit var adapter: RentalRequestAdapter

    // Views
    private lateinit var recyclerRentals: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabApproved: TextView
    private lateinit var tabRejected: TextView

    // State
    private var currentTabStatus = RentalRequest.STATUS_PENDING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_rental)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verify_rental_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        setupTabs()
        setupListeners()
        
        // Initial load
        selectTab(RentalRequest.STATUS_PENDING)
    }

    private fun initializeViews() {
        recyclerRentals = findViewById(R.id.recycler_rentals)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
        tabPending = findViewById(R.id.tab_pending)
        tabApproved = findViewById(R.id.tab_approved)
        tabRejected = findViewById(R.id.tab_rejected)
    }

    private fun setupRecyclerView() {
        adapter = RentalRequestAdapter(
            context = this,
            rentalList = rentalList,
            onApproveClick = { rental -> confirmAction(rental, RentalRequest.STATUS_APPROVED) },
            onRejectClick = { rental -> confirmAction(rental, RentalRequest.STATUS_REJECTED) }
        )
        recyclerRentals.layoutManager = LinearLayoutManager(this)
        recyclerRentals.adapter = adapter
    }

    private fun setupTabs() {
        tabPending.setOnClickListener { selectTab(RentalRequest.STATUS_PENDING) }
        tabApproved.setOnClickListener { selectTab(RentalRequest.STATUS_APPROVED) }
        tabRejected.setOnClickListener { selectTab(RentalRequest.STATUS_REJECTED) }
    }

    private fun selectTab(status: String) {
        currentTabStatus = status
        
        // Reset tab styles
        val unselectedBg = R.drawable.bg_stat_card
        val selectedBg = R.drawable.bg_tab_selected
        
        tabPending.setBackgroundResource(if (status == RentalRequest.STATUS_PENDING) selectedBg else unselectedBg)
        tabApproved.setBackgroundResource(if (status == RentalRequest.STATUS_APPROVED) selectedBg else unselectedBg)
        tabRejected.setBackgroundResource(if (status == RentalRequest.STATUS_REJECTED) selectedBg else unselectedBg)

        // Text colors
        tabPending.setTextColor(ContextCompat.getColor(this, if (status == RentalRequest.STATUS_PENDING) R.color.white else R.color.text_light))
        tabApproved.setTextColor(ContextCompat.getColor(this, if (status == RentalRequest.STATUS_APPROVED) R.color.white else R.color.text_light))
        tabRejected.setTextColor(ContextCompat.getColor(this, if (status == RentalRequest.STATUS_REJECTED) R.color.white else R.color.text_light))

        loadRentalsFromFirestore()
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }
    }

    private fun loadRentalsFromFirestore() {
        progressBar.visibility = View.VISIBLE
        recyclerRentals.visibility = View.GONE
        emptyState.visibility = View.GONE

        firestore.collection("rentals")
            .whereEqualTo("status", currentTabStatus)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                rentalList.clear()
                
                for (doc in documents) {
                    try {
                        val request = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                        rentalList.add(request)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.notifyDataSetChanged()
                
                progressBar.visibility = View.GONE
                if (rentalList.isEmpty()) {
                    showEmptyState()
                } else {
                    recyclerRentals.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                
                // If the query fails due to missing index, fallback to getting all and filtering
                if (exception.message?.contains("index") == true) {
                    loadRentalsWithoutIndex()
                } else {
                    showEmptyState("Gagal memuat data: ${exception.localizedMessage}")
                }
            }
    }
    
    // Fallback method if Firestore index hasn't been created yet
    private fun loadRentalsWithoutIndex() {
        firestore.collection("rentals")
            .get()
            .addOnSuccessListener { documents ->
                rentalList.clear()
                
                val filteredDocs = documents.filter { it.getString("status") == currentTabStatus }
                val sortedDocs = filteredDocs.sortedByDescending { it.getLong("createdAt") ?: 0L }
                
                for (doc in sortedDocs) {
                    try {
                        val request = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                        rentalList.add(request)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.notifyDataSetChanged()
                
                progressBar.visibility = View.GONE
                if (rentalList.isEmpty()) {
                    showEmptyState()
                } else {
                    recyclerRentals.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                showEmptyState("Gagal memuat data")
            }
    }

    private fun showEmptyState(message: String? = null) {
        emptyState.visibility = View.VISIBLE
        val defaultMessage = when (currentTabStatus) {
            RentalRequest.STATUS_PENDING -> "Tidak ada pengajuan pending"
            RentalRequest.STATUS_APPROVED -> "Belum ada pengajuan yang disetujui"
            RentalRequest.STATUS_REJECTED -> "Belum ada pengajuan yang ditolak"
            else -> "Data kosong"
        }
        tvEmptyMessage.text = message ?: defaultMessage
    }

    private fun confirmAction(rental: RentalRequest, newStatus: String) {
        val actionText = if (newStatus == RentalRequest.STATUS_APPROVED) "menyetujui" else "menolak"
        val title = if (newStatus == RentalRequest.STATUS_APPROVED) "Setujui Penyewaan" else "Tolak Penyewaan"
        
        val icon = if (newStatus == RentalRequest.STATUS_APPROVED) R.drawable.ic_check else R.drawable.ic_close
        val iconBg = if (newStatus == RentalRequest.STATUS_APPROVED) R.drawable.bg_icon_circle_green else R.drawable.bg_icon_circle_orange
        
        DialogUtils.showDangerDialog(
            activity = this,
            title = title,
            message = "Apakah Anda yakin ingin $actionText penyewaan ${rental.itemName} oleh ${rental.renterName}?",
            positiveButtonText = if (newStatus == RentalRequest.STATUS_APPROVED) "Setujui" else "Tolak",
            iconRes = icon,
            iconBgColorRes = iconBg
        ) {
            updateRentalStatus(rental, newStatus)
        }
    }

    private fun updateRentalStatus(rental: RentalRequest, newStatus: String) {
        progressBar.visibility = View.VISIBLE
        
        if (newStatus == RentalRequest.STATUS_APPROVED && rental.itemId.isNotEmpty()) {
            firestore.collection("items").document(rental.itemId).get()
                .addOnSuccessListener { doc ->
                    val currentStock = doc.getLong("stock")?.toInt() ?: 1
                    
                    if (currentStock <= 0) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Stok habis! Tidak bisa menyetujui penyewaan.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }
                    
                    val newStock = currentStock - 1
                    
                    val batch = firestore.batch()
                    val rentalRef = firestore.collection("rentals").document(rental.id)
                    batch.update(rentalRef, "status", newStatus)
                    batch.update(rentalRef, "updatedAt", System.currentTimeMillis())
                    
                    val itemRef = firestore.collection("items").document(rental.itemId)
                    batch.update(itemRef, "stock", newStock)
                    if (newStock <= 0) {
                        batch.update(itemRef, "status", Item.STATUS_RENTED)
                    }
                    
                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Status berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            NotificationHelper.showRentalStatusNotification(this, rental.itemName, newStatus)
                            loadRentalsFromFirestore() // Refresh current list
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "Gagal memperbarui status", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Gagal mengecek stok barang", Toast.LENGTH_SHORT).show()
                }
        } else {
            val batch = firestore.batch()
            
            val rentalRef = firestore.collection("rentals").document(rental.id)
            batch.update(rentalRef, "status", newStatus)
            batch.update(rentalRef, "updatedAt", System.currentTimeMillis())

            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(this, "Status berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    NotificationHelper.showRentalStatusNotification(this, rental.itemName, newStatus)
                    loadRentalsFromFirestore() // Refresh current list
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Gagal memperbarui status", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
