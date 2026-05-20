package com.example.rentease

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

class ViewReportsActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val reportList = mutableListOf<ReportItem>()
    private lateinit var adapter: ReportAdapter

    private lateinit var backButton: ImageButton
    private lateinit var exportButton: ImageButton
    private lateinit var reportTypeSpinner: Spinner
    
    private lateinit var tvStatVal1: TextView
    private lateinit var tvStatLabel1: TextView
    private lateinit var tvStatVal2: TextView
    private lateinit var tvStatLabel2: TextView
    
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyMessage: TextView
    private lateinit var recyclerView: RecyclerView

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val pdfFileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // ActivityResultLauncher for saving the PDF
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        if (uri != null) {
            generateAndSavePdf(uri)
        } else {
            Toast.makeText(this, "Pembuatan laporan dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_reports)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.view_reports_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupSpinner()
        setupRecyclerView()
        setupListeners()
        setupRoleTheme()
    }

    private fun setupRoleTheme() {
        val topBar = findViewById<LinearLayout>(R.id.top_bar)
        val firebaseAuthManager = FirebaseAuthManager()
        
        firebaseAuthManager.getUserData(
            onSuccess = { userData ->
                val role = userData["role"] as? String ?: "petugas"
                if (role == "admin") {
                    topBar.setBackgroundResource(R.drawable.bg_header_admin)
                } else {
                    topBar.setBackgroundResource(R.drawable.bg_header_petugas)
                }
            },
            onFailure = {
                topBar.setBackgroundResource(R.drawable.bg_header_petugas)
            }
        )
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.back_button)
        exportButton = findViewById(R.id.btn_export)
        reportTypeSpinner = findViewById(R.id.report_type_spinner)
        
        tvStatVal1 = findViewById(R.id.tv_stat_val_1)
        tvStatLabel1 = findViewById(R.id.tv_stat_label_1)
        tvStatVal2 = findViewById(R.id.tv_stat_val_2)
        tvStatLabel2 = findViewById(R.id.tv_stat_label_2)
        
        progressBar = findViewById(R.id.progress_bar)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
        recyclerView = findViewById(R.id.report_details_view)
    }

    private fun setupSpinner() {
        val categories = arrayOf("Penyewaan (Rentals)", "Barang (Items)", "Pengguna (Users)")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        reportTypeSpinner.adapter = spinnerAdapter
    }

    private fun setupRecyclerView() {
        adapter = ReportAdapter(reportList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        exportButton.setOnClickListener {
            if (reportList.isEmpty()) {
                Toast.makeText(this, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val category = reportTypeSpinner.selectedItem.toString().substringBefore(" ").lowercase()
            val fileName = "RentEase_Laporan_${category}_${pdfFileNameFormat.format(Date())}.pdf"
            createDocumentLauncher.launch(fileName)
        }

        reportTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadRentalReports()
                    1 -> loadItemReports()
                    2 -> loadUserReports()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ==========================================
    // DATA LOADING METHODS
    // ==========================================

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmptyMessage.visibility = View.GONE
        tvStatVal1.text = "-"
        tvStatVal2.text = "-"
    }

    private fun showData(totalCount: Int) {
        progressBar.visibility = View.GONE
        if (totalCount == 0) {
            tvEmptyMessage.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
        }
        adapter.updateData(reportList)
    }

    private fun loadRentalReports() {
        showLoading()
        tvStatLabel1.text = "Total Sewa"
        tvStatLabel2.text = "Disetujui"

        firestore.collection("rentals")
            .get()
            .addOnSuccessListener { documents ->
                reportList.clear()
                var approvedCount = 0
                
                for (doc in documents) {
                    val status = doc.getString("status") ?: "pending"
                    if (status == "approved") approvedCount++
                    
                    val itemName = doc.getString("itemName") ?: "Barang"
                    val renterName = doc.getString("renterName") ?: "Penyewa"
                    val time = doc.getLong("createdAt") ?: 0L
                    
                    val title = "Sewa: $itemName"
                    val subtitle = "Penyewa: $renterName • Status: ${status.uppercase()}"
                    
                    reportList.add(ReportItem(title, subtitle, dateFormat.format(Date(time)), time))
                }
                
                tvStatVal1.text = documents.size().toString()
                tvStatVal2.text = approvedCount.toString()
                
                // Sort descending
                reportList.sortByDescending { it.timestamp }
                showData(reportList.size)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat laporan penyewaan", Toast.LENGTH_SHORT).show()
                showData(0)
            }
    }

    private fun loadItemReports() {
        showLoading()
        tvStatLabel1.text = "Total Barang"
        tvStatLabel2.text = "Tersedia"

        firestore.collection("items")
            .get()
            .addOnSuccessListener { documents ->
                reportList.clear()
                var availableCount = 0
                
                for (doc in documents) {
                    val status = doc.getString("status") ?: "available"
                    if (status == "available") availableCount++
                    
                    val name = doc.getString("name") ?: "Barang"
                    val price = doc.getDouble("price") ?: 0.0
                    val time = doc.getLong("createdAt") ?: 0L
                    
                    val title = "Barang: $name"
                    val subtitle = "Harga: Rp${price.toInt()} • Status: ${status.uppercase()}"
                    
                    reportList.add(ReportItem(title, subtitle, dateFormat.format(Date(time)), time))
                }
                
                tvStatVal1.text = documents.size().toString()
                tvStatVal2.text = availableCount.toString()
                
                reportList.sortByDescending { it.timestamp }
                showData(reportList.size)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat laporan barang", Toast.LENGTH_SHORT).show()
                showData(0)
            }
    }

    private fun loadUserReports() {
        showLoading()
        tvStatLabel1.text = "Total User"
        tvStatLabel2.text = "Total Staff"

        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                reportList.clear()
                var regularUserCount = 0
                var staffCount = 0
                
                val emailMap = mutableMapOf<String, String>() // email -> docId
                val duplicatesToDelete = mutableListOf<String>()
                
                for (doc in documents) {
                    val email = doc.getString("email") ?: ""
                    val role = doc.getString("role") ?: "user"
                    val name = doc.getString("name") ?: "Tanpa Nama"
                    
                    if (email.isNotEmpty()) {
                        if (emailMap.containsKey(email)) {
                            // Duplikat terdeteksi!
                            // Hapus dokumen ini jika ID-nya bukan UID saat ini (simpelnya hapus yang ke-2)
                            duplicatesToDelete.add(doc.id)
                            continue 
                        } else {
                            emailMap[email] = doc.id
                        }
                    }

                    if (role == "user") {
                        regularUserCount++
                    } else {
                        staffCount++
                    }
                    
                    val title = "User: $name"
                    val subtitle = "Email: $email • Role: ${role.uppercase()}"
                    val time = doc.getLong("createdAt") ?: System.currentTimeMillis() 
                    
                    reportList.add(ReportItem(title, subtitle, dateFormat.format(Date(time)), time))
                }
                
                // Cleanup duplicates in background
                if (duplicatesToDelete.isNotEmpty()) {
                    for (id in duplicatesToDelete) {
                        firestore.collection("users").document(id).delete()
                    }
                    Toast.makeText(this, "Membersihkan ${duplicatesToDelete.size} data duplikat...", Toast.LENGTH_SHORT).show()
                }
                
                tvStatVal1.text = regularUserCount.toString()
                tvStatVal2.text = staffCount.toString()
                
                reportList.sortByDescending { it.timestamp }
                showData(reportList.size)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat laporan pengguna", Toast.LENGTH_SHORT).show()
                showData(0)
            }
    }

    // ==========================================
    // PDF GENERATION METHOD
    // ==========================================

    private fun generateAndSavePdf(uri: Uri) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size roughly
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        // Paints
        val titlePaint = Paint().apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(0, 100, 200) // RentEase Blueish
        }
        val headerPaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }
        val textPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
        }
        val lightTextPaint = Paint().apply {
            textSize = 10f
            color = Color.DKGRAY
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val margin = 50f
        var yPos = 60f

        // 1. Draw Title
        canvas.drawText("Laporan Sistem RentEase", margin, yPos, titlePaint)
        yPos += 30f

        // 2. Draw Report Info
        val category = reportTypeSpinner.selectedItem.toString()
        val dateGenerated = "Dicetak pada: " + dateFormat.format(Date())
        canvas.drawText("Kategori Laporan: $category", margin, yPos, textPaint)
        yPos += 20f
        canvas.drawText(dateGenerated, margin, yPos, textPaint)
        yPos += 20f

        // 3. Draw Summary Stats
        val stat1 = "${tvStatLabel1.text}: ${tvStatVal1.text}"
        val stat2 = "${tvStatLabel2.text}: ${tvStatVal2.text}"
        canvas.drawText("Ringkasan -> $stat1 | $stat2", margin, yPos, headerPaint)
        yPos += 20f
        
        // Draw separator line
        canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
        yPos += 30f

        // 4. Draw Table Headers
        canvas.drawText("No.", margin, yPos, headerPaint)
        canvas.drawText("Informasi Utama", margin + 40f, yPos, headerPaint)
        canvas.drawText("Tanggal", pageInfo.pageWidth - margin - 100f, yPos, headerPaint)
        yPos += 10f
        canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
        yPos += 20f

        // 5. Loop Items
        for ((index, item) in reportList.withIndex()) {
            // Pagination Check
            if (yPos > pageInfo.pageHeight - 60f) {
                pdfDocument.finishPage(currentPage)
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                yPos = 60f // Reset Y for new page
                
                // Redraw table headers on new page
                canvas.drawText("No.", margin, yPos, headerPaint)
                canvas.drawText("Informasi Utama", margin + 40f, yPos, headerPaint)
                canvas.drawText("Tanggal", pageInfo.pageWidth - margin - 100f, yPos, headerPaint)
                yPos += 10f
                canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
                yPos += 20f
            }

            // Draw Row Data
            canvas.drawText("${index + 1}.", margin, yPos, textPaint)
            canvas.drawText(item.title, margin + 40f, yPos, textPaint)
            canvas.drawText(item.dateStr, pageInfo.pageWidth - margin - 100f, yPos, textPaint)
            
            yPos += 15f
            
            // Draw subtitle slightly smaller and lighter
            canvas.drawText(item.subtitle, margin + 40f, yPos, lightTextPaint)
            
            yPos += 20f
            // Row separator
            canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
            yPos += 15f
        }

        // Finish last page
        pdfDocument.finishPage(currentPage)

        // 6. Write to Output Stream
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Toast.makeText(this, "Laporan berhasil disimpan", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
