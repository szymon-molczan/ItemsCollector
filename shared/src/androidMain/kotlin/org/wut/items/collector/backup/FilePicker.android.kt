package org.wut.items.collector.backup

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.io.FileOutputStream
import java.util.UUID













actual class FilePicker(
    private val activity: ComponentActivity
) {
    private val context: Context = activity.applicationContext
    private var pickLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<String?>? = null

    
    fun register() {
        pickLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            val deferred = pending
            pending = null
            if (uri == null) {
                deferred?.complete(null)
                return@registerForActivityResult
            }
            val copied = copyUriToCache(uri)
            deferred?.complete(copied?.absolutePath)
        }
    }

    actual suspend fun pickZip(): String? {
        val launcher = pickLauncher
            ?: error("FilePicker.register() musi byc wywolane w MainActivity.onCreate()")
        val deferred = CompletableDeferred<String?>()
        pending = deferred
        launcher.launch(arrayOf("application/zip"))
        return deferred.await()
    }

    private fun copyUriToCache(uri: Uri): File? {
        return try {
            val dir = File(context.cacheDir, "imports").apply { mkdirs() }
            val out = File(dir, "import_${UUID.randomUUID()}.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            if (out.exists() && out.length() > 0) out else null
        } catch (t: Throwable) {
            null
        }
    }
}
