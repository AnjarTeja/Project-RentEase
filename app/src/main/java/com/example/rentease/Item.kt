package com.example.rentease

data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val ownerId: String = "",
    val status: String = STATUS_AVAILABLE, // "available", "rented", "maintenance"
    val imageUrl: String = "",
    val createdAt: Long = 0L,
    val approvalStatus: String = APPROVAL_APPROVED,
    val rentCount: Int = 0,
    val stock: Int = 1
) {
    companion object {
        const val STATUS_AVAILABLE = "available"
        const val STATUS_RENTED = "rented"
        const val STATUS_MAINTENANCE = "maintenance"
        
        const val APPROVAL_PENDING = "pending"
        const val APPROVAL_APPROVED = "approved"
        const val APPROVAL_REJECTED = "rejected"
    }
}
