package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wingo_records")
data class WinGoRecord(
    @PrimaryKey
    val period: String,
    val number: Int,
    val bigSmall: String,
    val color: String,
    val gameType: String = "WinGo 1Min",
    val page: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
