package com.example.rentease

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BrowseViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    private val _allItems = MutableLiveData<List<Item>>()
    val allItems: LiveData<List<Item>> = _allItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var selectedCategory: String? = null
    private var searchQuery: String = ""
    private var priceMin: Double = 0.0
    private var priceMax: Double = Double.MAX_VALUE
    private var sortOption: Int = 0

    fun loadItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("items")
                        .whereEqualTo("status", Item.STATUS_AVAILABLE)
                        .get()
                        .await()
                }

                val parsedItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        val approval = doc.getString("approvalStatus")
                        if (approval == null || approval == Item.APPROVAL_APPROVED) {
                            Item(
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
                        } else null
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.rentCount }

                _allItems.value = parsedItems
                applyFilters()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat barang: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCategory(category: String?) {
        selectedCategory = category
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    fun setPriceRange(min: Double, max: Double) {
        priceMin = min
        priceMax = max
        applyFilters()
    }

    fun setSortOption(option: Int) {
        sortOption = option
        applyFilters()
    }

    private fun applyFilters() {
        val currentItems = _allItems.value ?: return

        val filtered = currentItems.filter { item ->
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() ||
                item.name.lowercase().contains(searchQuery) ||
                item.description.lowercase().contains(searchQuery)
            val matchesPrice = item.price >= priceMin && item.price <= priceMax
            matchesCategory && matchesSearch && matchesPrice
        }

        val sorted = when (sortOption) {
            1 -> filtered.sortedBy { it.price }
            2 -> filtered.sortedByDescending { it.price }
            3 -> filtered.sortedByDescending { it.createdAt }
            else -> filtered.sortedByDescending { it.rentCount }
        }

        _items.value = sorted
    }
}
