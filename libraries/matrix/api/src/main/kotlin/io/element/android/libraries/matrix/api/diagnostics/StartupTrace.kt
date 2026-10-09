package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context
import android.os.SystemClock
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Bounded, privacy-minimal startup telemetry. No credentials, IDs, network bodies, or messages.
 * The disk writer is never invoked on the UI thread.
 */
object StartupTrace {
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "syncme-startup-trace").apply { isDaemon = true }
    }
    @Volatile private var target: File? = null
    private val processStart = SystemClock.elapsedRealtime()
    private val processMarker = System.currentTimeMillis().toString(36)

    fun initialize(context: Context) {
        val file = File(context.filesDir, "syncme-startup-trace.txt")
        target = file
        mark("application_init")
    }

    fun mark(event: String) {
        val file = target ?: return
        val safe = event.replace(Regex("[^a-zA-Z0-9_.:-]"), "_").take(96)
        val elapsed = SystemClock.elapsedRealtime() - processStart
        val row = "${Instant.now()} pid=${android.os.Process.myPid()} run=$processMarker ms=$elapsed thread=${Thread.currentThread().name.take(32)} event=$safe\n"
        worker.execute {
            runCatching {
                if (file.length() > 150_000L) {
                    val tail = file.readText().takeLast(75_000)
                    file.writeText("--- older diagnostics truncated ---\n" + tail)
                }
                file.appendText(row)
            }
        }
    }
}
