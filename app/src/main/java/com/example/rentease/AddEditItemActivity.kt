package com.example.rentease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class AddEditItemActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var firebaseAuthManager: FirebaseAuthManager

    private var itemId: String? = null
    private var isUser: Boolean = false

    private var selectedImageUri: String = ""
    private var isUploading = false

    private lateinit var ivPhoto: ImageView
    private lateinit var ivPlaceholder: ImageView
    private lateinit var btnPickPhoto: TextView
    private lateinit var etName: EditText
    private lateinit var etDesc: EditText
    private lateinit var etPrice: EditText
    private lateinit var etStock: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvTitle: TextView
    private lateinit var btnSave: TextView
    private lateinit var backButton: ImageButton

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedImageUri = uri.toString()
                ivPhoto.setImageURI(uri)
                ivPlaceholder.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit_item)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_edit_item_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuthManager = FirebaseAuthManager()
        itemId = intent.getStringExtra("ITEM_ID")
        isUser = intent.getBooleanExtra("IS_USER", false)

        initializeViews()
        setupSpinner()
        setupCategorySpinner()
        setupListeners()

        if (isUser) {
            findViewById<TextView>(R.id.tv_status_label).visibility = View.GONE
            findViewById<View>(R.id.ll_status_container).visibility = View.GONE
        }

        if (itemId != null) {
            tvTitle.text = if (isUser) "Edit Pengajuan Barang" else "Edit Barang"
            loadItemData()
        } else {
            tvTitle.text = if (isUser) "Pengajuan Barang" else "Tambah Barang"
        }
    }

    private fun initializeViews() {
        tvTitle = findViewById(R.id.tv_activity_title)
        ivPhoto = findViewById(R.id.iv_item_photo)
        ivPlaceholder = findViewById(R.id.iv_photo_placeholder)
        btnPickPhoto = findViewById(R.id.btn_pick_photo)
        etName = findViewById(R.id.et_item_name)
        etDesc = findViewById(R.id.et_item_desc)
        etPrice = findViewById(R.id.et_item_price)
        etStock = findViewById(R.id.et_item_stock)
        spinnerStatus = findViewById(R.id.spinner_status)
        spinnerCategory = findViewById(R.id.spinner_category)
        btnSave = findViewById(R.id.btn_save_item)
        backButton = findViewById(R.id.back_button)
    }

    private fun setupSpinner() {
        val statuses = arrayOf("Tersedia", "Disewa", "Perbaikan")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter
    }

    private fun setupCategorySpinner() {
        val categories = Item.CATEGORIES.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveItemData() }
        btnPickPhoto.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }
    }

    private fun loadItemData() {
        if (itemId == null) return

        btnSave.isEnabled = false
        btnSave.text = "Memuat data..."

        firestore.collection("items").document(itemId!!)
            .get()
            .addOnSuccessListener { document ->
                btnSave.isEnabled = true
                btnSave.text = "Simpan Perubahan"

                if (document.exists()) {
                    etName.setText(document.getString("name"))
                    etDesc.setText(document.getString("description"))

                    val price = document.getDouble("price")
                    if (price != null) etPrice.setText(price.toInt().toString())

                    val stock = document.getLong("stock")
                    etStock.setText((stock?.toInt() ?: 1).toString())

                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        selectedImageUri = imageUrl
                        try {
                            ivPhoto.setImageURI(Uri.parse(imageUrl))
                            ivPlaceholder.visibility = View.GONE
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    val status = document.getString("status")
                    spinnerStatus.setSelection(when (status) {
                        Item.STATUS_AVAILABLE -> 0
                        Item.STATUS_RENTED -> 1
                        Item.STATUS_MAINTENANCE -> 2
                        else -> 0
                    })

                    val category = document.getString("category") ?: Item.CATEGORY_OTHER
                    spinnerCategory.setSelection(Item.CATEGORIES.indexOf(category).coerceAtLeast(0))
                } else {
                    Toast.makeText(this, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                btnSave.text = "Simpan Perubahan"
                Toast.makeText(this, "Gagal memuat data barang", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveItemData() {
        if (isUploading) return

        val name = etName.text.toString().trim()
        val desc = etDesc.text.toString().trim()
        val priceStr = etPrice.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Nama dan Harga tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(this, "Harga tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val stockStr = etStock.text.toString().trim()
        val stock = stockStr.toIntOrNull() ?: 1

        val statusValue = if (isUser) {
            Item.STATUS_AVAILABLE
        } else {
            when (spinnerStatus.selectedItemPosition) {
                0 -> Item.STATUS_AVAILABLE
                1 -> Item.STATUS_RENTED
                2 -> Item.STATUS_MAINTENANCE
                else -> Item.STATUS_AVAILABLE
            }
        }

        val approvalStatus = if (isUser) Item.APPROVAL_PENDING else Item.APPROVAL_APPROVED
        val selectedCategory = Item.CATEGORIES[spinnerCategory.selectedItemPosition]

        btnSave.isEnabled = false
        btnSave.text = "Menyimpan..."

        // If image is a local URI, upload to Firebase Storage first
        if (selectedImageUri.isNotEmpty() && !selectedImageUri.startsWith("https://firebasestorage")) {
            isUploading = true
            btnSave.text = "Mengupload gambar..."

            ImageUploadHelper.uploadImage(
                context = this,
                imageUri = Uri.parse(selectedImageUri),
                folder = "items",
                onSuccess = { downloadUrl ->
                    isUploading = false
                    saveToFirestore(name, desc, price, stock, statusValue, approvalStatus, selectedCategory, downloadUrl)
                },
                onFailure = { error ->
                    isUploading = false
                    // Fallback: save with original URI
                    saveToFirestore(name, desc, price, stock, statusValue, approvalStatus, selectedCategory, selectedImageUri)
                    Toast.makeText(this, "Gambar akan diproses nanti: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            saveToFirestore(name, desc, price, stock, statusValue, approvalStatus, selectedCategory, selectedImageUri)
        }
    }

    private fun saveToFirestore(
        name: String, desc: String, price: Double, stock: Int,
        statusValue: String, approvalStatus: String, category: String, imageUrl: String
    ) {
        val itemData = hashMapOf<String, Any>(
            "name" to name,
            "description" to desc,
            "price" to price,
            "stock" to stock,
            "status" to statusValue,
            "imageUrl" to imageUrl,
            "approvalStatus" to approvalStatus,
            "category" to category,
            "updatedAt" to System.currentTimeMillis()
        )

        val ownerId = firebaseAuthManager.getCurrentUserUID() ?: ""

        if (itemId == null) {
            itemData["createdAt"] = System.currentTimeMillis()
            itemData["ownerId"] = ownerId

            firestore.collection("items").add(itemData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Barang berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true; btnSave.text = "Simpan Data Barang"
                    Toast.makeText(this, "Gagal menambahkan barang", Toast.LENGTH_SHORT).show()
                }
        } else {
            firestore.collection("items").document(itemId!!).update(itemData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Barang berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true; btnSave.text = "Simpan Perubahan"
                    Toast.makeText(this, "Gagal memperbarui barang", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
