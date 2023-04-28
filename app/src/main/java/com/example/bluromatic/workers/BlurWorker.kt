package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
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

        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)

        // Check if Input URI is valid
        require(!resourceUri.isNullOrBlank()){
            val errorMessage = applicationContext.getString(R.string.invalid_input_uri)
            Log.e(TAG, "doWork: $errorMessage")
            errorMessage
        }

        // send notification once operation starts
        makeStatusNotification(
            message = applicationContext.getString(R.string.blurring_image),
            context = applicationContext
        )

        // Now this part is important
        // Since we are passing URI for the request
        // We need Content Resolver to read the object
        val resolver = applicationContext.contentResolver

        // By default, runs on Default... moving to IO
        return withContext(Dispatchers.IO) {

            // To simulate a longer running process
            delay(DELAY_TIME_MILLIS)

            try {
                // Load the picture bitmap from the URI with BitmapFactory
                // this is done using ContentResolver now
                // Reading a file with uri using content resolver
                val picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri))
                )

                // run blurBitmap method
                val output = blurBitmap(bitmap = picture, blurLevel)

                // get output Uri
                val outputUri = writeBitmapToFile(applicationContext, output)

                // send notification again once operation completes
                // Don't need this notification, as we are simply sending the result back
                /*makeStatusNotification(
                    message = "Output is $outputUri",
                    context = applicationContext
                )*/

                /**
                 * This is basically creating a map of (Key, Value) pairs and creates data object
                 * it is alternative way to create data object
                 */
                val outputData = workDataOf(
                    KEY_IMAGE_URI to outputUri.toString()
                )

                // Return Success
                Result.success(outputData)
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