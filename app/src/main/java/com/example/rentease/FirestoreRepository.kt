package com.example.rentease

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query

/**
 * Centralized repository for Firestore operations.
 * Reduces code duplication across Activities.
 */
object FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // ============ ITEMS ============

    fun getAvailableItems(onSuccess: (List<Item>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("items")
            .whereEqualTo("status", Item.STATUS_AVAILABLE)
            .get()
            .addOnSuccessListener { documents ->
                val items = mutableListOf<Item>()
                for (doc in documents) {
                    try {
                        val approval = doc.getString("approvalStatus")
                        if (approval == null || approval == Item.APPROVAL_APPROVED) {
                            items.add(parseItem(doc))
                        }
                    } catch (e: Exception) { /* skip malformed */ }
                }
                items.sortByDescending { it.rentCount }
                onSuccess(items)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getAllItems(onSuccess: (List<Item>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val items = documents.mapNotNull { doc ->
                    try { parseItem(doc) } catch (e: Exception) { null }
                }
                onSuccess(items)
            }
            .addOnFailureListener { exception ->
                // Fallback without index
                if (exception.message?.contains("index") == true) {
                    firestore.collection("items").get()
                        .addOnSuccessListener { docs ->
                            val items = docs.mapNotNull { doc ->
                                try { parseItem(doc) } catch (e: Exception) { null }
                            }.sortedByDescending { it.createdAt }
                            onSuccess(items)
                        }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(exception)
                }
            }
    }

    fun getItemsByOwner(ownerId: String, onSuccess: (List<Item>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("items")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { documents ->
                val items = documents.mapNotNull { doc ->
                    try { parseItem(doc) } catch (e: Exception) { null }
                }.sortedByDescending { it.createdAt }
                onSuccess(items)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteItem(itemId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("items").document(itemId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun hasActiveRentals(itemId: String, onSuccess: (Boolean) -> Unit, onFailure: () -> Unit) {
        firestore.collection("rentals")
            .whereEqualTo("itemId", itemId)
            .whereIn("status", listOf("pending", "approved"))
            .get()
            .addOnSuccessListener { onSuccess(!it.isEmpty) }
            .addOnFailureListener {
                // Fallback
                firestore.collection("rentals")
                    .whereEqualTo("itemId", itemId)
                    .get()
                    .addOnSuccessListener { docs ->
                        val hasActive = docs.documents.any {
                            val s = it.getString("status")
                            s == "pending" || s == "approved"
                        }
                        onSuccess(hasActive)
                    }
                    .addOnFailureListener { onFailure() }
            }
    }

    // ============ RENTALS ============

    fun getRentalsByStatus(status: String, onSuccess: (List<RentalRequest>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("rentals")
            .whereEqualTo("status", status)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val rentals = documents.map { it.toObject(RentalRequest::class.java).copy(id = it.id) }
                onSuccess(rentals)
            }
            .addOnFailureListener { exception ->
                if (exception.message?.contains("index") == true) {
                    firestore.collection("rentals")
                        .whereEqualTo("status", status)
                        .get()
                        .addOnSuccessListener { docs ->
                            val rentals = docs.map { it.toObject(RentalRequest::class.java).copy(id = it.id) }
                                .sortedByDescending { it.createdAt }
                            onSuccess(rentals)
                        }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(exception)
                }
            }
    }

    fun getRentalsByRenter(renterId: String, statuses: List<String>, onSuccess: (List<RentalRequest>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("rentals")
            .whereEqualTo("renterId", renterId)
            .whereIn("status", statuses)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val rentals = documents.map { it.toObject(RentalRequest::class.java).copy(id = it.id) }
                onSuccess(rentals)
            }
            .addOnFailureListener { exception ->
                if (exception.message?.contains("index") == true) {
                    firestore.collection("rentals")
                        .whereEqualTo("renterId", renterId)
                        .get()
                        .addOnSuccessListener { docs ->
                            val rentals = docs.map { it.toObject(RentalRequest::class.java).copy(id = it.id) }
                                .filter { it.status in statuses }
                                .sortedByDescending { it.createdAt }
                            onSuccess(rentals)
                        }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(exception)
                }
            }
    }

    // ============ USERS ============

    fun getUserCount(onSuccess: (Int) -> Unit) {
        firestore.collection("users").get()
            .addOnSuccessListener { onSuccess(it.size()) }
            .addOnFailureListener { onSuccess(0) }
    }

    fun getCollectionCount(collection: String, onSuccess: (Int) -> Unit) {
        firestore.collection(collection).get()
            .addOnSuccessListener { onSuccess(it.size()) }
            .addOnFailureListener { onSuccess(0) }
    }

    // ============ HELPERS ============

    private fun parseItem(doc: com.google.firebase.firestore.DocumentSnapshot): Item {
        return Item(
            id = doc.id,
            name = doc.getString("name") ?: "",
            description = doc.getString("description") ?: "",
            price = doc.getDouble("price") ?: 0.0,
            ownerId = doc.getString("ownerId") ?: "",
            status = doc.getString("status") ?: Item.STATUS_AVAILABLE,
            imageUrl = doc.getString("imageUrl") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L,
            approvalStatus = doc.getString("approvalStatus") ?: Item.APPROVAL_APPROVED,
            rentCount = (doc.getLong("rentCount") ?: 0L).toInt(),
            stock = (doc.getLong("stock") ?: 1L).toInt(),
            category = doc.getString("category") ?: Item.CATEGORY_CAMERA
        )
    }
}
