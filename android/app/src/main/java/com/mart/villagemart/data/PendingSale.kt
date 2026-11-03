package com.mart.villagemart.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_sales")
data class PendingSale(
    @PrimaryKey val idempotencyKey: String,
    val requestJson: String,
    val createdAtEpochMillis: Long,
    val status: String = STATUS_PENDING,
    val saleId: Long? = null,
    val invoiceNumber: String? = null,
    val lastError: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_FAILED = "FAILED"
    }
}

@Dao
interface PendingSaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(sale: PendingSale)

    @Query("SELECT * FROM pending_sales WHERE idempotencyKey = :key")
    suspend fun find(key: String): PendingSale?

    @Query("SELECT * FROM pending_sales WHERE status = 'PENDING' ORDER BY createdAtEpochMillis")
    suspend fun pending(): List<PendingSale>

    @Query("SELECT * FROM pending_sales ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<PendingSale>>

    @Query("UPDATE pending_sales SET status = 'CONFIRMED', saleId = :saleId, invoiceNumber = :invoiceNumber, lastError = NULL WHERE idempotencyKey = :key")
    suspend fun markConfirmed(key: String, saleId: Long, invoiceNumber: String)

    @Query("UPDATE pending_sales SET status = 'FAILED', lastError = :message WHERE idempotencyKey = :key")
    suspend fun markFailed(key: String, message: String)

    @Query("UPDATE pending_sales SET lastError = :message WHERE idempotencyKey = :key")
    suspend fun updateError(key: String, message: String)

    @Query("DELETE FROM pending_sales WHERE idempotencyKey = :key")
    suspend fun delete(key: String)
}

@Database(entities = [PendingSale::class], version = 1, exportSchema = true)
abstract class VillageMartDatabase : RoomDatabase() {
    abstract fun pendingSaleDao(): PendingSaleDao
}
