/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluromatic.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.bluromatic.CHANNEL_ID
import com.example.bluromatic.NOTIFICATION_ID
import com.example.bluromatic.NOTIFICATION_TITLE
import com.example.bluromatic.OUTPUT_PATH
import com.example.bluromatic.R
import com.example.bluromatic.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
import com.example.bluromatic.VERBOSE_NOTIFICATION_CHANNEL_NAME
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.util.UUID

private const val TAG = "WorkerUtils"

/**
 * Create a Notification that is shown as a heads-up notification if possible.
 *
 * For this codelab, this is used to show a notification so that you know when different steps
 * of the background work chain are starting
 *
 * @param message Message shown on the notification
 * @param context Context needed to create Toast
 */
fun makeStatusNotification(
    message: String,
    context: Context
) {

    var canNotify = true


    // Make a channel if necessary
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
        val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description

        // Add the channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }

    // Create the notification
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(NOTIFICATION_TITLE)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVibrate(LongArray(0))

    // In case it is Android T or above, we have to check for Permission
    // maybe add a event here... which will call to request permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            canNotify = false
            Toast.makeText(context, "Permission required to send notification", Toast.LENGTH_SHORT)
                .show()
        }
    }

    if (canNotify) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

}

/**
 * Blurs the given Bitmap image
 * @param bitmap Image to blur
 * @param blurLevel Blur level input
 * @return Blurred bitmap image
 */
@WorkerThread
fun blurBitmap(bitmap: Bitmap, blurLevel: Int): Bitmap {
    val input = Bitmap.createScaledBitmap(
        bitmap,
        bitmap.width / (blurLevel * 5),
        bitmap.height / (blurLevel * 5),
        true
    )
    return Bitmap.createScaledBitmap(input, bitmap.width, bitmap.height, true)
}

/**
 * Writes bitmap to a temporary file and returns the Uri for the file
 * @param applicationContext Application context
 * @param bitmap Bitmap to write to temp file
 * @return Uri for temp file with bitmap
 * @throws FileNotFoundException Throws if bitmap file cannot be found
 */
@Throws(FileNotFoundException::class)
fun writeBitmapToFile(applicationContext: Context, bitmap: Bitmap): Uri {
    val name = String.format("blur-filter-output-%s.png", UUID.randomUUID().toString())
    val outputDir = File(applicationContext.filesDir, OUTPUT_PATH)
    if (!outputDir.exists()) {
        outputDir.mkdirs() // should succeed
    }
    val outputFile = File(outputDir, name)
    var out: FileOutputStream? = null
    try {
        out = FileOutputStream(outputFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, out)
    } finally {
        out?.let {
            try {
                it.close()
            } catch (e: IOException) {
                Log.e(TAG, e.message.toString())
            }
        }
    }
    return Uri.fromFile(outputFile)
}

/**
 * In android 10 and above
 * Store via MediaStore ContentProvider...
 * better than working with whole scoped storage thing
 * Here permission is only needed if reading image from another app
 */
fun saveImageToExternalDirAndroidQAndAbove(
    appContext: Context,
    displayName: String,
    mimeType: String,
    bitmap: Bitmap
): Uri? {

    val resolver = appContext.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        // If we don't provide RELATIVE_PATH -- it saves in PICTURES by default
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    println("MediaSore URI = ${MediaStore.Images.Media.EXTERNAL_CONTENT_URI} -- ${MediaStore.Images.Media.INTERNAL_CONTENT_URI} ")

    val savedImageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    println("Saved Image URI = $savedImageUri")


    try {
        savedImageUri?.let { resolver.openOutputStream(it) }?.use {
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                0,
                it
            )
        }

    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }

    return savedImageUri

}

/**
 * This will save data in Android 9 or lower
 * MediaStore though works Android 9 or lower.. it doesn't work 100% well
 * though need Write Permission here
 * also... need to get write permission approval
 */
fun saveImageToExternalDir(
    displayName: String,
    bitmap: Bitmap
): Uri {

    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), displayName)

    println("File Saved at ${file.toURI()}")

    try {
        file.outputStream()
            .use {
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    0,
                    it
                )
            }
    }catch (e: Exception){
        e.printStackTrace()
        throw e
    }

    return file.toUri()
}

fun checkWritePermissionToStorage(){

}