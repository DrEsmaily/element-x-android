package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors
import android.app.ActivityManager
import android.os.Debug
import java.util.concurrent.TimeUnit

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
    private val watchdogStarted = AtomicBoolean(false)
    private val processMarker = System.currentTimeMillis().toString(36)

    fun initialize(context: Context) {
        val file = File(context.filesDir, "syncme-startup-trace.txt")
        target = file
        mark("application_init")
        mark("device_api_" + android.os.Build.VERSION.SDK_INT + "_cores_" + Runtime.getRuntime().availableProcessors())
        mark("heap_max_mb_" + Runtime.getRuntime().maxMemory() / 1048576)
        startMetrics()
        startWatchdog()
    }

    private fun startMetrics() {
        Thread({
            val runtime = Runtime.getRuntime()
            var lastGc = -1L
            val start = SystemClock.elapsedRealtime()
            while (SystemClock.elapsedRealtime() - start < 90_000L) {
                Thread.sleep(1000L)
                val used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576
                val native = Debug.getNativeHeapAllocatedSize() / 1048576
                mark("memory_java_mb_" + used + "_native_mb_" + native)
                val gcCount = Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull() ?: -1L
                if (gcCount >= 0 && lastGc >= 0 && gcCount != lastGc) {
                    mark("gc_count_delta_" + (gcCount - lastGc))
                }
                lastGc = gcCount
            }
        }, "syncme-memory-sampler").apply { isDaemon = true }.start()
    }

    private fun startWatchdog() {
        if (!watchdogStarted.compareAndSet(false, true)) return
        val handler = Handler(Looper.getMainLooper())
        Thread({
            val start = SystemClock.elapsedRealtime()
            while (SystemClock.elapsedRealtime() - start < 120_000L) {
                val postedAt = SystemClock.elapsedRealtime()
                val acknowledged = AtomicBoolean(false)
                handler.post {
                    val latency = SystemClock.elapsedRealtime() - postedAt
                    if (latency > 100L) mark("main_queue_latency_ms_" + latency)
                    acknowledged.set(true)
                }
                Thread.sleep(250L)
                val wait = SystemClock.elapsedRealtime() - postedAt
                if (!acknowledged.get() && wait >= 250) {
                    mark("main_queue_blocked_ms_" + wait)
                    Looper.getMainLooper().thread.stackTrace.take(16).forEachIndexed { index, frame ->
                        mark("main_stack_" + index + "_" + frame.className + "." + frame.methodName + ":" + frame.lineNumber)
                    }
                    var loops = 0
                    while (!acknowledged.get() && loops++ < 80) Thread.sleep(100L)
                    if (acknowledged.get()) mark("main_queue_recovered")
                }
                Thread.sleep(100L)
            }
        }, "syncme-startup-watchdog").apply { isDaemon = true }.start()
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
