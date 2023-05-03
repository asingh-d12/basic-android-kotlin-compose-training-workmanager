import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.workers.BlurWorker
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class WorkInstrumentationTest {
    private lateinit var context: Context

    private val mockUriInput =
        KEY_IMAGE_URI to "android.resource://com.example.bluromatic/drawable/android_cupcake"


    @Before
    fun setUp() {
        // To get Context in Android Tests♠
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun cleanupWorker_doWork_resultSuccess() {
        // This basically gives direct access to the worker
        val worker = TestListenableWorkerBuilder<CleanupWorker>(context).build()

        // Which we can run here directly
        runBlocking {
            val result = worker.doWork()
            MatcherAssert.assertThat(result, `is`(ListenableWorker.Result.success()))
        }
    }

    @Test
    fun blurWorker_doWork_resultSuccessReturnsUri() {

        val data = workDataOf(
            KEY_IMAGE_URI to "android.resource://com.example.bluromatic/drawable/android_cupcake"
        )

        val worker = TestListenableWorkerBuilder<BlurWorker>(
            context = context,
            inputData = data
        ).build()

        runBlocking {
            val result = worker.doWork()
            val resultUri = result.outputData.getString(KEY_IMAGE_URI)
            Assert.assertTrue(
                resultUri?.startsWith(
                    "file:///data/user/0/com.example.bluromatic/files/blur_filter_outputs/blur-filter-output-"
                ) ?: false
            )
        }

    }

    @Test
    fun saveImageToFileWorker_doWork_resultSuccessReturnsUri() {

        val data = workDataOf(mockUriInput)

        val worker = TestListenableWorkerBuilder<SaveImageToFileWorker>(
            context = context,
            inputData = data
        ).build()

        runBlocking {
            val result = worker.doWork()
            val resultUri = result.outputData.getString(KEY_IMAGE_URI)

            val verifyPrefix = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "content://media/external/images/media/"
            } else {
                "file:///storage/emulated/0/Pictures/SavedImages/"
            }

            Assert.assertTrue(resultUri?.startsWith(verifyPrefix) ?: false)
        }

    }


}