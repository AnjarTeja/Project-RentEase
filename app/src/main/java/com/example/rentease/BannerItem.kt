package com.example.rentease

data class BannerItem(
    val id: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val itemId: String? = null,
    val isActive: Boolean = true
)
