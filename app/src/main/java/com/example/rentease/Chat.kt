package com.example.rentease

data class Chat(
    val id: String = "",
    val rentalId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val renterId: String = "",
    val renterName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val createdAt: Long = 0L
)
