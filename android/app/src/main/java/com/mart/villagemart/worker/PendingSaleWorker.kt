package com.mart.villagemart.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mart.villagemart.data.PendingSale
import com.mart.villagemart.data.PendingSaleDao
import com.mart.villagemart.data.VillageMartRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

@HiltWorker
class PendingSaleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pendingSaleDao: PendingSaleDao,
    private val repository: VillageMartRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            for (draft in pendingSaleDao.pending()) {
                try {
                    repository.retryPendingSale(draft)
                } catch (error: HttpException) {
                    if (error.code() >= 500 || error.code() == 408 || error.code() == 429) {
                        pendingSaleDao.updateError(draft.idempotencyKey, error.message())
                        return Result.retry()
                    }
                    pendingSaleDao.markFailed(draft.idempotencyKey, error.message())
                } catch (error: IOException) {
                    pendingSaleDao.updateError(
                        draft.idempotencyKey,
                        error.message ?: "Network unavailable"
                    )
                    return Result.retry()
                }
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to synchronize pending sale drafts", error)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "PendingSaleWorker"
    }
}
