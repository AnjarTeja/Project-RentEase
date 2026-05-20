package com.example.rentease

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ItemDetailActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val authManager = FirebaseAuthManager()

    private lateinit var itemId: String
    private lateinit var itemName: String
    private lateinit var itemOwnerId: String
    private var itemPrice: Double = 0.0
    private var itemImageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_item_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.item_detail_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Don't pad top because of CollapsingToolbar
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        itemId = intent.getStringExtra("ITEM_ID") ?: ""

        if (itemId.isEmpty()) {
            Toast.makeText(this, "Item tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupBackButton()
        loadItemDetails()
        setupRentButton()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun loadItemDetails() {
        firestore.collection("items").document(itemId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    itemName = doc.getString("name") ?: "Barang"
                    itemOwnerId = doc.getString("ownerId") ?: ""
                    
                    val description = doc.getString("description") ?: ""
                    itemPrice = doc.getDouble("price") ?: 0.0
                    val stock = doc.getLong("stock")?.toInt() ?: 1
                    itemImageUrl = doc.getString("imageUrl") ?: ""
                    val status = doc.getString("status") ?: Item.STATUS_AVAILABLE

                    findViewById<TextView>(R.id.tv_detail_name).text = itemName
                    findViewById<TextView>(R.id.tv_detail_description).text = description
                    findViewById<TextView>(R.id.tv_detail_stock).text = "Sisa Stok: $stock"
                    
                    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    findViewById<TextView>(R.id.tv_detail_price).text = "${formatter.format(itemPrice)} / hari"

                    val tvStatus = findViewById<TextView>(R.id.tv_detail_status)
                    val btnRentNow = findViewById<MaterialButton>(R.id.btn_rent_now)
                    val currentUid = authManager.getCurrentUserUID()
                    val formContainer = findViewById<View>(R.id.ll_rental_form)

                    if (currentUid == itemOwnerId) {
                        tvStatus.text = "Milik Anda"
                        tvStatus.setTextColor(getColor(R.color.primary_color))
                        formContainer.visibility = View.GONE
                        btnRentNow.isEnabled = false
                        btnRentNow.text = "Barang Milik Anda"
                    } else if (status == Item.STATUS_AVAILABLE && stock > 0) {
                        tvStatus.text = "Tersedia"
                        tvStatus.setTextColor(getColor(R.color.primary_color))
                        btnRentNow.isEnabled = true
                        btnRentNow.text = "Ajukan Sewa Barang"
                    } else {
                        tvStatus.text = if (stock <= 0) "Stok Habis" else "Tidak Tersedia"
                        tvStatus.setTextColor(getColor(R.color.error_color))
                        btnRentNow.isEnabled = false
                        btnRentNow.text = if (stock <= 0) "Stok Habis" else "Barang Sedang Disewa"
                    }

                    val ivImage = findViewById<ImageView>(R.id.iv_detail_image)
                    if (itemImageUrl.isNotEmpty()) {
                        try {
                            ivImage.setImageURI(Uri.parse(itemImageUrl))
                        } catch (e: Exception) {
                            ivImage.setImageResource(R.drawable.bg_card_dark)
                        }
                    }
                } else {
                    Toast.makeText(this, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat detail barang", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRentButton() {
        val btnRentNow = findViewById<MaterialButton>(R.id.btn_rent_now)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        btnRentNow.setOnClickListener {
            val durationStr = findViewById<EditText>(R.id.et_duration).text.toString().trim()
            val note = findViewById<EditText>(R.id.et_note).text.toString().trim()

            if (durationStr.isEmpty()) {
                Toast.makeText(this, "Masukkan durasi sewa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationStr.toIntOrNull()
            if (duration == null || duration <= 0) {
                Toast.makeText(this, "Durasi sewa harus berupa angka lebih dari 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRentNow.text = ""
            progressBar.visibility = View.VISIBLE
            btnRentNow.isEnabled = false

            submitRentalRequest(duration, note, btnRentNow, progressBar)
        }
    }

    private fun submitRentalRequest(duration: Int, note: String, btnRentNow: MaterialButton, progressBar: ProgressBar) {
        authManager.getUserData(
            onSuccess = { userData ->
                val renterName = userData["name"] as? String ?: "Pengguna"
                val renterEmail = userData["email"] as? String ?: authManager.getCurrentUserEmail() ?: ""
                val renterId = authManager.getCurrentUserUID() ?: ""

                val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                val startDate = sdf.format(Date())

                val rentalRequest = RentalRequest(
                    itemName = itemName,
                    itemId = itemId,
                    renterName = renterName,
                    renterEmail = renterEmail,
                    renterId = renterId,
                    ownerId = itemOwnerId,
                    startDate = startDate,
                    duration = duration,
                    note = note,
                    status = RentalRequest.STATUS_PENDING,
                    createdAt = System.currentTimeMillis(),
                    pricePerDay = itemPrice,
                    itemImageUrl = itemImageUrl
                )

                firestore.collection("rentals")
                    .add(rentalRequest)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Berhasil mengajukan sewa! Menunggu persetujuan petugas.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengajukan sewa.", Toast.LENGTH_SHORT).show()
                        resetButtonState(btnRentNow, progressBar)
                    }
            },
            onFailure = {
                Toast.makeText(this, "Gagal mengambil data profil Anda", Toast.LENGTH_SHORT).show()
                resetButtonState(btnRentNow, progressBar)
            }
        )
    }

    private fun resetButtonState(btnRentNow: MaterialButton, progressBar: ProgressBar) {
        btnRentNow.text = "Ajukan Sewa Barang"
        progressBar.visibility = View.GONE
        btnRentNow.isEnabled = true
    }
}
