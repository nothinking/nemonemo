package com.example.seventh.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanHistory(): Flow<List<ScanHistory>>
    
    @Query("SELECT * FROM scan_history WHERE " +
           "tags LIKE :tag || ',%' OR " +
           "tags LIKE '%, ' || :tag || ',%' OR " +
           "tags LIKE '%,' || :tag || ',%' OR " +
           "tags LIKE '%, ' || :tag || ', %' OR " +
           "tags LIKE '%,' || :tag || ', %' OR " +
           "tags LIKE '%, ' || :tag || ',%' OR " +
           "tags LIKE '%,' || :tag OR " +
           "tags LIKE '%, ' || :tag OR " +
           "tags = :tag OR " +
           "tags = ' ' || :tag OR " +
           "tags = :tag || ' ' OR " +
           "tags = ' ' || :tag || ' ' " +
           "ORDER BY timestamp DESC")
    fun getScanHistoryByTag(tag: String): Flow<List<ScanHistory>>
    
    @Query("SELECT tags FROM scan_history WHERE tags IS NOT NULL AND tags != ''")
    fun getAllTags(): Flow<List<String>>
    
    @Insert
    suspend fun insertScanHistory(scanHistory: ScanHistory)
    
    @Delete
    suspend fun deleteScanHistory(scanHistory: ScanHistory)
    
    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanHistoryById(id: Long): ScanHistory?
} 