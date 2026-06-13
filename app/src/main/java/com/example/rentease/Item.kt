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
    val stock: Int = 1,
    val category: String = CATEGORY_OTHER
) {
    companion object {
        const val STATUS_AVAILABLE = "available"
        const val STATUS_RENTED = "rented"
        const val STATUS_MAINTENANCE = "maintenance"
        
        const val APPROVAL_PENDING = "pending"
        const val APPROVAL_APPROVED = "approved"
        const val APPROVAL_REJECTED = "rejected"

        const val CATEGORY_ELECTRONICS = "Elektronik"
        const val CATEGORY_TOOLS = "Peralatan"
        const val CATEGORY_VEHICLES = "Kendaraan"
        const val CATEGORY_FURNITURE = "Furnitur"
        const val CATEGORY_CLOTHING = "Pakaian"
        const val CATEGORY_OTHER = "Lainnya"

        val CATEGORIES = listOf(
            CATEGORY_ELECTRONICS,
            CATEGORY_TOOLS,
            CATEGORY_VEHICLES,
            CATEGORY_FURNITURE,
            CATEGORY_CLOTHING,
            CATEGORY_OTHER
        )
    }
}
