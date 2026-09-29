package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.data.db.WinGoRecord
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

class FirestoreWinGoRepository(context: Context) {
    private val tag = "FirestoreWinGoRepo"
    private val collectionName = "records"

    val firestore: FirebaseFirestore? = try {
        val apps = FirebaseApp.getApps(context)
        val app = if (apps.isEmpty()) {
            FirebaseApp.initializeApp(context)
        } else {
            FirebaseApp.getInstance()
        }
        if (app != null) {
            FirebaseFirestore.getInstance(app)
        } else {
            null
        }
    } catch (t: Throwable) {
        Log.w(tag, "Firebase Firestore initialization notice (cloud sync will fallback to local Room DB): ${t.message}")
        null
    }

    val isAvailable: Boolean
        get() = firestore != null

    /**
     * Real-time retrieval of WinGo game records ordered by period DESC.
     * Listens for live snapshot changes and emits updated lists.
     */
    fun getRealtimeRecordsFlow(limit: Long = 500): Flow<List<WinGoRecord>> {
        val db = firestore ?: return emptyFlow()

        return callbackFlow {
            val listenerRegistration = db.collection(collectionName)
                .orderBy("period", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Firestore snapshot listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val records = snapshot.documents.mapNotNull { doc ->
                            try {
                                val period = doc.getString("period") ?: doc.id
                                val number = doc.getLong("number")?.toInt() ?: 0
                                val bigSmall = doc.getString("bigSmall") ?: if (number >= 5) "Big" else "Small"
                                val color = doc.getString("color") ?: "Red"
                                val gameType = doc.getString("gameType") ?: "WinGo 1Min"
                                val page = doc.getLong("page")?.toInt() ?: 1
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                WinGoRecord(
                                    period = period,
                                    number = number,
                                    bigSmall = bigSmall,
                                    color = color,
                                    gameType = gameType,
                                    page = page,
                                    timestamp = timestamp
                                )
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to parse Firestore doc ${doc.id}: ${e.message}")
                                null
                            }
                        }
                        trySend(records)
                    }
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Store a single WinGo game record in Firestore.
     */
    suspend fun saveRecord(record: WinGoRecord): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore is not initialized"))
        return try {
            val data = hashMapOf(
                "period" to record.period,
                "number" to record.number,
                "bigSmall" to record.bigSmall,
                "color" to record.color,
                "gameType" to record.gameType,
                "page" to record.page,
                "timestamp" to record.timestamp
            )

            db.collection(collectionName)
                .document(record.period)
                .set(data, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving record ${record.period} to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Store a batch of WinGo game records in Firestore (e.g. from scraping pages).
     */
    suspend fun saveBatchRecords(records: List<WinGoRecord>): Result<Int> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore is not initialized"))
        return try {
            // Firestore write batches support up to 500 operations per batch
            val batches = records.chunked(450)
            var totalWritten = 0

            for (chunk in batches) {
                val batch = db.batch()
                for (record in chunk) {
                    val docRef = db.collection(collectionName).document(record.period)
                    val data = hashMapOf(
                        "period" to record.period,
                        "number" to record.number,
                        "bigSmall" to record.bigSmall,
                        "color" to record.color,
                        "gameType" to record.gameType,
                        "page" to record.page,
                        "timestamp" to record.timestamp
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
                totalWritten += chunk.size
            }

            Result.success(totalWritten)
        } catch (e: Exception) {
            Log.e(tag, "Error batch writing to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Clear all records from Firestore.
     */
    suspend fun clearAllRecords(): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore is not initialized"))
        return try {
            val snapshot = db.collection(collectionName).get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error clearing Firestore records: ${e.message}")
            Result.failure(e)
        }
    }
}
