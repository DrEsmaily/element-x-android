package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
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
    @Volatile private var directory: File? = null
    private const val MAX_RUNS = 20
    private const val RUN_LIMIT_BYTES = 2_000_000L
    private val processStart = SystemClock.elapsedRealtime()
    private val watchdogStarted = AtomicBoolean(false)
    private val processMarker = System.currentTimeMillis().toString(36)

    fun initialize(context: Context) {
        val folder = File(context.filesDir, "syncme-startup-runs")
        folder.mkdirs()
        directory = folder
        target = File(folder, "run-" + System.currentTimeMillis().toString().padStart(13, '0') +
            "-" + android.os.Process.myPid() + ".txt")
        worker.execute { prune(folder) }
        mark("application_init")
        startWatchdog()
    }

    private fun startWatchdog() {
        if (!watchdogStarted.compareAndSet(false, true)) return
        val handler = Handler(Looper.getMainLooper())
        Thread({
            while (true) {
                val acknowledged = AtomicBoolean(false)
                val postedAt = SystemClock.elapsedRealtime()
                handler.post { acknowledged.set(true) }
                Thread.sleep(350L)
                if (!acknowledged.get()) {
                    mark("main_thread_stall_detected_ms_" + (SystemClock.elapsedRealtime() - postedAt))
                    var samples = 0
                    while (!acknowledged.get()) {
                        // Always retain the full Java call chain, including the application SDK
                        // frames BELOW Socket.connect; 12 frames were insufficient previously.
                        if (samples < 8 || samples % 5 == 0) {
                            val main = Looper.getMainLooper().thread
                            mark("stall_sample_" + samples + "_state_" + main.state)
                            main.stackTrace.take(72).forEachIndexed { index, frame ->
                                mark("stack_" + samples + "_" + index + "_" + frame.className + "." +
                                    frame.methodName + ":" + frame.lineNumber)
                            }
                        }
                        samples++
                        Thread.sleep(400L)
                    }
                    mark("main_thread_resumed_after_ms_" + (SystemClock.elapsedRealtime() - postedAt))
                }
                Thread.sleep(150L)
            }
        }, "syncme-startup-watchdog").apply { isDaemon = true }.start()
    }

    private fun prune(folder: File) {
        val files = folder.listFiles { f -> f.isFile && f.name.startsWith("run-") && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.name } ?: return
        files.drop(MAX_RUNS).forEach { runCatching { it.delete() } }
    }

    /** Call only from a background dispatcher; includes up to ten completed/current runs. */
    fun readRecentReports(): String {
        val folder = directory ?: return "Diagnostics unavailable."
        val files = folder.listFiles { f -> f.isFile && f.name.startsWith("run-") && f.name.endsWith(".txt") }
            ?.sortedBy { it.name }?.takeLast(MAX_RUNS).orEmpty()
        return if (files.isEmpty()) "No startup data." else files.joinToString("\n\n") { file ->
            "===== " + file.name + " =====\n" +
                runCatching { file.readText() }.getOrDefault("Read error")
        }
    }

    /**
     * Clear retained diagnostic runs without touching Matrix caches, sessions or app data.
     * Must be called from a background dispatcher. Serialize with the writer so old
     * queued append operations cannot recreate deleted reports.
     */
    fun clearReports(): Boolean {
        val folder = directory ?: return false
        return runCatching {
            worker.submit<Boolean> {
                val active = target
                val files = folder.listFiles { f ->
                    f.isFile && f.name.startsWith("run-") && f.name.endsWith(".txt")
                }.orEmpty()
                var success = true
                files.forEach { file ->
                    val cleared = if (file == active) {
                        runCatching { file.writeText("") }.isSuccess
                    } else {
                        file.delete()
                    }
                    if (!cleared) success = false
                }
                success
            }.get()
        }.getOrDefault(false)
    }

    fun recordFailure(label: String, error: Throwable) {
        mark(label + "_failure")
        var cause: Throwable? = error
        var depth = 0
        while (cause != null && depth < 12) {
            val current = cause
            mark(label + "_cause_" + depth + "_" + current.javaClass.name)
            current.message.orEmpty().chunked(120).take(20).forEachIndexed { i, part ->
                mark(label + "_message_" + depth + "_" + i + "_" + part)
            }
            current.stackTrace.take(20).forEachIndexed { i, frame ->
                mark(label + "_frame_" + depth + "_" + i + "_" + frame.className + "." + frame.methodName + ":" + frame.lineNumber)
            }
            cause = current.cause
            depth++
        }
    }

    fun mark(event: String) {
        val file = target ?: return
        val safe = event.replace(Regex("[^a-zA-Z0-9_.:-]"), "_").take(240)
        val elapsed = SystemClock.elapsedRealtime() - processStart
        val row = "${Instant.now()} pid=${android.os.Process.myPid()} run=$processMarker ms=$elapsed thread=${Thread.currentThread().name.take(32)} event=$safe\n"
        worker.execute {
            runCatching {
                if (file.length() >= RUN_LIMIT_BYTES) return@runCatching
                file.appendText(row)
            }
        }
    }
}
