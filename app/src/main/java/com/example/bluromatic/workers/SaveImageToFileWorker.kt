package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Locale

class SaveImageToFileWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:MM:SS z",
        Locale.getDefault()
    )

    override suspend fun doWork(): Result {

        makeStatusNotification(
            message = applicationContext.getString(R.string.saving_image),
            applicationContext
        )

        return withContext(Dispatchers.IO){
            delay(DELAY_TIME_MILLIS)

            val resolver = applicationContext.contentResolver

            try{
                // Get the input image URI from Data sent to this worker
                val resourceUri = inputData.getString(KEY_IMAGE_URI)

                // Read the input image url into a bitmap
                val bitmap = BitmapFactory.decodeStream(
                    resolver.openInputStream(
                        Uri.parse(resourceUri)
                    )
                )

                // This is where it gets awesome
                /*val imageUrl = MediaStore.Images.Media.insertImage(
                    resolver,
                    bitmap,
                    title,
                    dateFormatter.format(Date())
                )*/
                val imageUrl = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageToExternalDirAndroidQAndAbove(
                        applicationContext,
                        title,
                        "image/png",
                        bitmap
                    )?.toString()
                }else {
                    saveImageToExternalDir(
                        "$title.png",
                        bitmap
                    ).toString()
                }

                println("Image URL = $imageUrl")

                if(!imageUrl.isNullOrEmpty()){
                    val output = workDataOf(
                        KEY_IMAGE_URI to imageUrl
                    )
                    Result.success(output)
                }else{
                    Log.e(TAG, applicationContext.resources.getString(R.string.writing_to_mediaStore_failed))
                    Result.failure()
                }

            }catch (e: Exception){
                Log.e(TAG, applicationContext.resources.getString(R.string.error_saving_image), e)
                Result.failure()
            }

        }

    }

}

private const val TAG = "SaveImageToFileWorker"