package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * This class basically is responsible for starting/scheduling an asynchronous background work
 * this will happen even if we close the app
 */
class BlurWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        // send notification once operation starts
        makeStatusNotification(
            message = applicationContext.getString(R.string.blurring_image),
            context = applicationContext
        )

        // By default, runs on Default... moving to IO
        return withContext(Dispatchers.IO) {

            // To simulate a longer running process
            delay(DELAY_TIME_MILLIS)

            try {
                // Load the picture bitmap from the resources with BitmapFactory
                val picture = BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.android_cupcake
                )

                // run blurBitmap method
                val output = blurBitmap(bitmap = picture, 1)

                // get output Uri
                val outputUri = writeBitmapToFile(applicationContext, output)

                // send notification again once operation completes
                makeStatusNotification(
                    message = "Output is $outputUri",
                    context = applicationContext
                )

                // Return Success
                Result.success()
            } catch (throwable: Throwable) {
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_applying_blur),
                    throwable
                )
                // Return Failure
                Result.failure()
            }
        }
    }

}

private const val TAG = "BlurWorker"