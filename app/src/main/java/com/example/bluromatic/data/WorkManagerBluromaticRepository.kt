/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.example.bluromatic.data

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.getImageUri
import com.example.bluromatic.workers.BlurWorker
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class WorkManagerBluromaticRepository(context: Context) : BluromaticRepository {

    // Get WorkManager Instance here
    private val workManager = WorkManager.getInstance(context)
    val imageUri = context.getImageUri()

    override val outputWorkInfo: Flow<WorkInfo?> = MutableStateFlow(null)

    /**
     * Create the WorkRequests to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    override fun applyBlur(blurLevel: Int) {

        // This is creating a one time request
        /**
         * In this code, we show an alternate way to create the OneTimeWorkRequest object.
         * Calling OneTimeWorkRequest.from(CleanupWorker::class.java) is the equivalent to calling
         * OneTimeWorkRequestBuilder<CleanupWorker>().build().
         * Class OneTimeWorkRequest comes from the AndroidX Work library while
         * OneTimeWorkRequestBuilder is a helper function provided by the WorkManager KTX extension.
         */
        var continuation = workManager.beginWith(OneTimeWorkRequest.Companion.from(CleanupWorker::class.java))

        // Now the BlurBuilder WorkRequest same as before
        val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

        // Adding BlurBuilder to WorkContinuation Object using .then
        continuation = continuation.then(
            blurBuilder
                .setInputData(createInputDataForWorkRequest(blurLevel, imageUri))
                .build()
        )

        // Now the Save WorkRequest
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>().build()

        continuation = continuation.then(save)

        // This will simply change OnetimeWorkRequest.Builder to WorkRequest and enque to WorkManager
        // Adding data object to this request - using setInputData method
        /*workManager.enqueue(
            blurBuilder
                .setInputData(createInputDataForWorkRequest(blurLevel, imageUri))
                .build()
        )*/

        // Adding enqueue to WorkContinuation object
        continuation.enqueue()
    }

    /**
     * Cancel any ongoing WorkRequests
     * */
    override fun cancelWork() {}

    /**
     * Creates the input data bundle which includes the blur level to
     * update the amount of blur to be applied and the Uri to operate on
     * @return Data which contains the Image Uri as a String and blur level as an Integer
     */
    private fun createInputDataForWorkRequest(blurLevel: Int, imageUri: Uri): Data {
        val builder = Data.Builder()
        println("Input Image URi = $imageUri")
        builder.putString(KEY_IMAGE_URI, imageUri.toString()).putInt(KEY_BLUR_LEVEL, blurLevel)
        return builder.build()
    }
}
