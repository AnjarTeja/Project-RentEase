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

class ManageReturnsActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "ManageReturns"

    private lateinit var rvReturns: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: LinearLayout
    private lateinit var backButton: ImageButton

    private lateinit var adapter: ManageReturnAdapter
    private val rentalList = mutableListOf<RentalRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_returns)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manage_returns_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadApprovedRentals()
    }

    private fun initializeViews() {
        rvReturns = findViewById(R.id.rv_manage_returns)
        progressBar = findViewById(R.id.progress_bar)
        emptyStateView = findViewById(R.id.empty_state_view)
        backButton = findViewById(R.id.back_button)
    }

    private fun setupRecyclerView() {
        adapter = ManageReturnAdapter(
            rentalList = rentalList,
            onReturnClick = { rental -> showConfirmReturnDialog(rental) }
        )
        rvReturns.layoutManager = LinearLayoutManager(this)
        rvReturns.adapter = adapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
    }

    private fun loadApprovedRentals() {
        progressBar.visibility = View.VISIBLE
        rvReturns.visibility = View.GONE
        emptyStateView.visibility = View.GONE

        firestore.collection("rentals")
            .whereEqualTo("status", "approved")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                rentalList.clear()
                for (doc in documents) {
                    val rental = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                    rentalList.add(rental)
                }

                progressBar.visibility = View.GONE
                if (rentalList.isEmpty()) {
                    emptyStateView.visibility = View.VISIBLE
                } else {
                    rvReturns.visibility = View.VISIBLE
                }
                adapter.updateData(rentalList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading rentals: ", e)
                progressBar.visibility = View.GONE
                emptyStateView.visibility = View.VISIBLE
                
                // Fallback if index is missing
                if (e.message?.contains("FAILED_PRECONDITION") == true) {
                    fetchWithoutOrderFallback()
                } else {
                    Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchWithoutOrderFallback() {
        firestore.collection("rentals")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                rentalList.clear()
                for (doc in documents) {
                    val rental = doc.toObject(RentalRequest::class.java).copy(id = doc.id)
                    rentalList.add(rental)
                }
                
                rentalList.sortByDescending { it.createdAt }

                if (rentalList.isEmpty()) {
                    emptyStateView.visibility = View.VISIBLE
                } else {
                    rvReturns.visibility = View.VISIBLE
                }
                adapter.updateData(rentalList)
            }
    }

    private fun showConfirmReturnDialog(rental: RentalRequest) {
        DialogUtils.showDangerDialog(
            activity = this,
            title = "Konfirmasi Pengembalian",
            message = "Apakah barang '${rental.itemName}' sudah dikembalikan dengan kondisi baik oleh ${rental.renterName}?",
            positiveButtonText = "Sudah Kembali",
            iconRes = R.drawable.ic_check,
            iconBgColorRes = R.drawable.bg_icon_circle_green
        ) {
            processReturn(rental)
        }
    }

    private fun processReturn(rental: RentalRequest) {
        progressBar.visibility = View.VISIBLE
        
        // We need a transaction or batch write to ensure consistency
        val batch = firestore.batch()

        val rentalRef = firestore.collection("rentals").document(rental.id)
        batch.update(rentalRef, "status", "returned")

        if (rental.itemId.isNotEmpty()) {
            val itemRef = firestore.collection("items").document(rental.itemId)
            batch.update(itemRef, "status", Item.STATUS_AVAILABLE)
            // Increment the rentCount by 1 and restock by 1
            batch.update(itemRef, "rentCount", com.google.firebase.firestore.FieldValue.increment(1))
            batch.update(itemRef, "stock", com.google.firebase.firestore.FieldValue.increment(1))
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Barang berhasil dikembalikan!", Toast.LENGTH_SHORT).show()
                loadApprovedRentals()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error processing return: ", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memproses pengembalian", Toast.LENGTH_SHORT).show()
            }
    }
}
