package com.example.rentease

data class ItemReport(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val description: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = 0L
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_DISMISSED = "dismissed"
    }
}
