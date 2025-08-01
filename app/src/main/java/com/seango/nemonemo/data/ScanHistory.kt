package com.seango.nemonemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val tags: String,
    val isFromGallery: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) 