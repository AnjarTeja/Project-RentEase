package com.example.rentease

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * MVVM version of BrowseItemsActivity.
 * Demonstrates proper architecture with ViewModel + LiveData.
 */
class BrowseItemsMvvmActivity : AppCompatActivity() {

    private lateinit var viewModel: BrowseViewModel
    private lateinit var rvItems: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: BrowseItemAdapter
    private lateinit var etSearch: EditText
    private lateinit var categoryChipsContainer: LinearLayout
    private lateinit var etPriceMin: EditText
    private lateinit var etPriceMax: EditText
    private lateinit var spinnerSort: Spinner

    private val categoryChips = mutableListOf<TextView>()
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_browse_items)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.browse_items_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this)[BrowseViewModel::class.java]

        initializeViews()
        setupRecyclerView()
        setupCategoryChips()
        setupSortSpinner()
        setupListeners()
        observeViewModel()

        viewModel.loadItems()
    }

    private fun initializeViews() {
        rvItems = findViewById(R.id.rv_browse_items)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        etSearch = findViewById(R.id.et_search_items)
        categoryChipsContainer = findViewById(R.id.category_chips_container)
        etPriceMin = findViewById(R.id.et_price_min)
        etPriceMax = findViewById(R.id.et_price_max)
        spinnerSort = findViewById(R.id.spinner_sort)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_clear_search).setOnClickListener {
            etSearch.setText("")
            etSearch.clearFocus()
        }
    }

    private fun setupRecyclerView() {
        adapter = BrowseItemAdapter(emptyList()) { item ->
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
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
        }
        chip.setOnClickListener {
            selectedCategory = category
            selectChip(chip, category)
            viewModel.setCategory(category)
        }
        return chip
    }

    private fun selectChip(selected: TextView, category: String?) {
        for (chip in categoryChips) {
            chip.setBackgroundResource(
                if (chip == selected) R.drawable.bg_chip_selected
                else R.drawable.bg_chip_unselected
            )
            chip.setTextColor(
                if (chip == selected) Color.WHITE
                else ContextCompat.getColor(this, R.color.text_hint)
            )
        }
    }

    private fun dp(value: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Populer", "Termurah", "Termahal", "Terbaru")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                viewModel.setSortOption(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString().trim().lowercase())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val priceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val min = etPriceMin.text.toString().trim().toDoubleOrNull() ?: 0.0
                val max = etPriceMax.text.toString().trim().toDoubleOrNull() ?: Double.MAX_VALUE
                viewModel.setPriceRange(min, max)
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etPriceMin.addTextChangedListener(priceWatcher)
        etPriceMax.addTextChangedListener(priceWatcher)
    }

    private fun observeViewModel() {
        viewModel.items.observe(this) { itemList ->
            adapter.updateData(itemList)
            if (itemList.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                rvItems.visibility = View.GONE
                findViewById<TextView>(R.id.tv_empty_message)?.text =
                    if (etSearch.text.toString().isNotEmpty()) "Barang '${etSearch.text}' tidak ditemukan"
                    else "Tidak ada barang di kategori ini"
            } else {
                emptyState.visibility = View.GONE
                rvItems.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                viewModel.errorMessage.value = null
            }
        }
    }
}
