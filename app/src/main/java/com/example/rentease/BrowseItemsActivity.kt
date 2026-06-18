package com.example.rentease

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var categoryChipsContainer: LinearLayout
    private lateinit var etPriceMin: EditText
    private lateinit var etPriceMax: EditText
    private lateinit var spinnerSort: Spinner

    private val allItemList = mutableListOf<Item>()
    private val itemList = mutableListOf<Item>()

    private var selectedCategory: String? = null
    private val categoryChips = mutableListOf<TextView>()
    private var sortOption = 0

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
        setupCategoryChips()
        setupSortSpinner()
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
        categoryChipsContainer = findViewById(R.id.category_chips_container)
        etPriceMin = findViewById(R.id.et_price_min)
        etPriceMax = findViewById(R.id.et_price_max)
        spinnerSort = findViewById(R.id.spinner_sort)
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

    private fun setupCategoryChips() {
        categoryChipsContainer.removeAllViews()
        categoryChips.clear()

        val allChip = createChip("Semua", null)
        categoryChipsContainer.addView(allChip)
        categoryChips.add(allChip)

        for (category in Item.CATEGORIES) {
            val chip = createChip(category, category)
            categoryChipsContainer.addView(chip)
            categoryChips.add(chip)
        }

        selectChip(allChip, null)
    }

    private fun createChip(label: String, category: String?): TextView {
        val chip = TextView(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            layoutParams = params
        }

        chip.setOnClickListener {
            selectedCategory = category
            selectChip(chip, category)
            applyFilters()
        }

        return chip
    }

    private fun selectChip(selectedChip: TextView, category: String?) {
        for (chip in categoryChips) {
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_hint))
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Populer", "Termurah", "Termahal", "Terbaru")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortOption = position
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.toString().trim().isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            etSearch.clearFocus()
        }

        val priceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyFilters() }
            override fun afterTextChanged(s: Editable?) {}
        }
        etPriceMin.addTextChangedListener(priceWatcher)
        etPriceMax.addTextChangedListener(priceWatcher)
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().trim().lowercase()
        val priceMin = etPriceMin.text.toString().trim().toDoubleOrNull() ?: 0.0
        val priceMax = etPriceMax.text.toString().trim().toDoubleOrNull() ?: Double.MAX_VALUE

        itemList.clear()

        for (item in allItemList) {
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesSearch = query.isEmpty() ||
                item.name.lowercase().contains(query) ||
                item.description.lowercase().contains(query)
            val matchesPrice = item.price >= priceMin && item.price <= priceMax

            if (matchesCategory && matchesSearch && matchesPrice) {
                itemList.add(item)
            }
        }

        when (sortOption) {
            0 -> itemList.sortByDescending { it.rentCount } // Populer
            1 -> itemList.sortBy { it.price } // Termurah
            2 -> itemList.sortByDescending { it.price } // Termahal
            3 -> itemList.sortByDescending { it.createdAt } // Terbaru
        }

        adapter.updateData(itemList)

        if (itemList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_empty_message)?.text =
                if (query.isNotEmpty()) "Barang '$query' tidak ditemukan"
                else "Tidak ada barang di kategori ini"
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
                                stock = (doc.getLong("stock") ?: 1L).toInt(),
                                category = doc.getString("category") ?: Item.CATEGORY_OTHER
                            )
                            allItemList.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing item: ${e.message}")
                    }
                }

                allItemList.sortByDescending { it.rentCount }
                applyFilters()

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
