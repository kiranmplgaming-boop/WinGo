package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WinGoRecordDao {
    @Query("SELECT * FROM wingo_records ORDER BY period DESC")
    fun getAllRecords(): Flow<List<WinGoRecord>>

    @Query("SELECT COUNT(*) FROM wingo_records")
    fun getRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM wingo_records")
    suspend fun getRecordCountDirect(): Int

    @Query("SELECT * FROM wingo_records WHERE period = :period LIMIT 1")
    suspend fun getRecordByPeriod(period: String): WinGoRecord?

    @Query("SELECT * FROM wingo_records ORDER BY period DESC LIMIT 1")
    fun getLatestRecord(): Flow<WinGoRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WinGoRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<WinGoRecord>)

    @Query("DELETE FROM wingo_records WHERE length(period) != 17")
    suspend fun deleteInvalidLengthRecords()

    @Query("DELETE FROM wingo_records")
    suspend fun clearAll()
}
