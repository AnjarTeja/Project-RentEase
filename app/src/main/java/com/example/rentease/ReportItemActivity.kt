package com.example.rentease

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportItemActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rgReason: RadioGroup
    private lateinit var etDescription: EditText
    private lateinit var btnSubmit: MaterialButton

    private var itemId: String = ""
    private var itemName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report_item)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.report_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        itemId = intent.getStringExtra("ITEM_ID") ?: ""
        itemName = intent.getStringExtra("ITEM_NAME") ?: "Barang"

        if (itemId.isEmpty()) {
            Toast.makeText(this, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rgReason = findViewById(R.id.rg_reason)
        etDescription = findViewById(R.id.et_description)
        btnSubmit = findViewById(R.id.btn_submit_report)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        btnSubmit.setOnClickListener { submitReport() }
    }

    private fun submitReport() {
        val selectedReasonId = rgReason.checkedRadioButtonId
        if (selectedReasonId == -1) {
            Toast.makeText(this, "Pilih alasan pelaporan", Toast.LENGTH_SHORT).show()
            return
        }

        val reason = findViewById<RadioButton>(selectedReasonId).text.toString()
        val description = etDescription.text.toString().trim()
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = "Mengirim..."

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val reporterName = doc.getString("name") ?: "Pengguna"

                val report = hashMapOf(
                    "itemId" to itemId,
                    "itemName" to itemName,
                    "reporterId" to uid,
                    "reporterName" to reporterName,
                    "reason" to reason,
                    "description" to description,
                    "status" to ItemReport.STATUS_PENDING,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("item_reports").add(report)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Laporan berhasil dikirim! Admin akan meninjau.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener {
                        btnSubmit.isEnabled = true
                        btnSubmit.text = "Kirim Laporan"
                        Toast.makeText(this, "Gagal mengirim laporan", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Kirim Laporan"
                Toast.makeText(this, "Gagal mengambil data profil", Toast.LENGTH_SHORT).show()
            }
    }
}
