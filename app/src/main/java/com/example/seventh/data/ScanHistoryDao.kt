package com.example.seventh.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanHistory(): Flow<List<ScanHistory>>
    
    @Insert
    suspend fun insertScanHistory(scanHistory: ScanHistory)
    
    @Delete
    suspend fun deleteScanHistory(scanHistory: ScanHistory)
    
    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanHistoryById(id: Long): ScanHistory?
} 