package com.example.bluromatic.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.OUTPUT_PATH
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class CleanupWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        makeStatusNotification(
            message = applicationContext.getString(R.string.cleaning_up_files),
            applicationContext
        )

        return withContext(Dispatchers.IO){

            delay(DELAY_TIME_MILLIS)

            try {
                val outputDir = File(applicationContext.filesDir, OUTPUT_PATH)
                if (outputDir.exists()) {
                    val entries = outputDir.listFiles()
                    if (entries != null) {
                        for (entry in entries) {
                            val name = entry.name
                            if (name.isNotEmpty() && name.endsWith(".png")) {
                                val delete = entry.delete()
                                Log.i(TAG, "Old File deleted - $name - $delete")
                            }
                        }
                    }
                }
                Result.success()
            }
            catch (e: Exception){
                e.printStackTrace()
                Result.failure()
            }
        }

    }

}

private const val TAG = "ClenupWorker"