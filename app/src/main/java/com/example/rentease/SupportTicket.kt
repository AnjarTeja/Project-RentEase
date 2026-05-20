package com.example.rentease

/**
 * Data class representing a support ticket / help request in Firestore.
 * 
 * Firestore collection: "support_tickets"
 */
data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = STATUS_OPEN, // "open", "in_progress", "resolved"
    val createdAt: Long = 0L,
    val replyMessage: String = "",
    val repliedAt: Long = 0L,
    val repliedBy: String = "" // Petugas UID
) {
    companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_RESOLVED = "resolved"
    }
}
