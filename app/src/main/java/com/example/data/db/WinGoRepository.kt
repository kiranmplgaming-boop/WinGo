package com.example.data.db

import kotlinx.coroutines.flow.Flow

class WinGoRepository(private val dao: WinGoRecordDao) {
    val records: Flow<List<WinGoRecord>> = dao.getAllRecords()
    val recordCount: Flow<Int> = dao.getRecordCount()
    val latestRecord: Flow<WinGoRecord?> = dao.getLatestRecord()

    suspend fun getRecordCountDirect(): Int = dao.getRecordCountDirect()

    suspend fun getRecordByPeriod(period: String): WinGoRecord? = dao.getRecordByPeriod(period)

    suspend fun insertRecords(records: List<WinGoRecord>) {
        dao.insertRecords(records)
    }

    suspend fun insertRecord(record: WinGoRecord) {
        dao.insertRecord(record)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun deleteInvalidLengthRecords() {
        dao.deleteInvalidLengthRecords()
    }
}
