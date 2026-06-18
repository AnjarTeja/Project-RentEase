package com.example.rentease

import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageReturnsActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "ManageReturns"

    private lateinit var rvReturns: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: LinearLayout
    private lateinit var backButton: ImageButton
    private lateinit var tvOverdueCount: TextView
    private lateinit var tvTotalFine: TextView

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
        tvOverdueCount = findViewById(R.id.tv_overdue_count)
        tvTotalFine = findViewById(R.id.tv_total_fine)

        val fineHeader = findViewById<LinearLayout>(R.id.ll_fine_summary)
        if (fineHeader != null) fineHeader.visibility = View.VISIBLE
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
                processOverdueCheck()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading rentals: ", e)
                if (e.message?.contains("FAILED_PRECONDITION") == true) {
                    fetchWithoutOrderFallback()
                } else {
                    progressBar.visibility = View.GONE
                    emptyStateView.visibility = View.VISIBLE
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
                processOverdueCheck()
            }
    }

    private fun processOverdueCheck() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val todayDate = sdf.parse(sdf.format(Date()))
        var overdueCount = 0
        var totalFine = 0.0

        for (rental in rentalList) {
            try {
                val endDate = sdf.parse(rental.endDate)
                if (endDate != null && todayDate != null && endDate.before(todayDate)) {
                    val diffMs = todayDate.time - endDate.time
                    val overdueDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                    val fine = overdueDays * rental.pricePerDay * 0.1 // 10% per day fine

                    rental.overdueDays = overdueDays
                    rental.fineAmount = fine
                    rental.isOverdue = true

                    overdueCount++
                    totalFine += fine
                }
            } catch (e: Exception) { /* skip */ }
        }

        tvOverdueCount.text = "$overdueCount Rental Terlambat"
        tvTotalFine.text = "Total Denda: Rp ${String.format("%,.0f", totalFine)}"

        progressBar.visibility = View.GONE
        if (rentalList.isEmpty()) {
            emptyStateView.visibility = View.VISIBLE
        } else {
            rvReturns.visibility = View.VISIBLE
        }
        adapter.updateData(rentalList)
    }

    private fun showConfirmReturnDialog(rental: RentalRequest) {
        var message = "Apakah barang '${rental.itemName}' sudah dikembalikan dengan kondisi baik oleh ${rental.renterName}?"
        if (rental.isOverdue && rental.fineAmount > 0) {
            message += "\n\n⚠️ Rental ini terlambat ${rental.overdueDays} hari!\nDenda: Rp ${String.format("%,.0f", rental.fineAmount)}"
        }

        DialogUtils.showDangerDialog(
            activity = this,
            title = "Konfirmasi Pengembalian",
            message = message,
            positiveButtonText = "Sudah Kembali",
            iconRes = R.drawable.ic_check,
            iconBgColorRes = R.drawable.bg_icon_circle_green
        ) {
            processReturn(rental)
        }
    }

    private fun processReturn(rental: RentalRequest) {
        progressBar.visibility = View.VISIBLE

        val batch = firestore.batch()

        val rentalRef = firestore.collection("rentals").document(rental.id)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        val updates = hashMapOf<String, Any>(
            "status" to "returned",
            "actualReturnDate" to sdf.format(Date()),
            "isOverdue" to rental.isOverdue
        )
        if (rental.isOverdue) {
            updates["overdueDays"] = rental.overdueDays
            updates["fineAmount"] = rental.fineAmount
        }
        batch.update(rentalRef, updates)

        if (rental.itemId.isNotEmpty()) {
            val itemRef = firestore.collection("items").document(rental.itemId)
            batch.update(itemRef, "status", Item.STATUS_AVAILABLE)
            batch.update(itemRef, "rentCount", com.google.firebase.firestore.FieldValue.increment(1))
            batch.update(itemRef, "stock", com.google.firebase.firestore.FieldValue.increment(1))
        }

        batch.commit()
            .addOnSuccessListener {
                val fineMsg = if (rental.isOverdue && rental.fineAmount > 0) {
                    " Denda: Rp ${String.format("%,.0f", rental.fineAmount)}"
                } else ""
                Toast.makeText(this, "Barang berhasil dikembalikan!$fineMsg", Toast.LENGTH_LONG).show()

                if (rental.isOverdue) {
                    NotificationHelper.showOverdueNotification(
                        this, rental.itemName, rental.renterName, rental.overdueDays
                    )
                }
                loadApprovedRentals()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error processing return: ", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memproses pengembalian", Toast.LENGTH_SHORT).show()
            }
    }
}
