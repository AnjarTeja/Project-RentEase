package com.example.rentease

/**
 * Data class representing a rental request in Firestore.
 *
 * Firestore collection: "rentals"
 * Each document contains the fields below.
 */
data class RentalRequest(
    val id: String = "",
    val itemName: String = "",
    val itemId: String = "",
    val renterName: String = "",
    val renterEmail: String = "",
    val renterId: String = "",
    val ownerId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val duration: Int = 0,
    val status: String = STATUS_PENDING,  // "pending", "approved", "rejected"
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val note: String = "",
    val pricePerDay: Double = 0.0,
    val itemImageUrl: String = "",
    val rating: Int = 0  // 0 = not rated, 1-5 = star rating
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_RETURNED = "returned"
    }
}
