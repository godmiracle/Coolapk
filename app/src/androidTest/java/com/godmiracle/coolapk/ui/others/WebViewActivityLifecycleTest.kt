package com.godmiracle.coolapk.ui.others

import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WebViewActivityLifecycleTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation = instrumentation.uiAutomation
    private val webViewProcessName = "${context.packageName}:webview"

    @Test
    fun backNavigationKeepsWebViewProcessAliveAndReusesItWhenReopened() {
        val hostPid = Process.myPid()
        val hostProcessName = currentProcessName()

        try {
            val originalWebViewPid = launchWebViewAndAwaitPid()
            assertProcessAlive(hostProcessName, hostPid)

            pressBack()
            waitUntil("WebViewActivity to be removed after back navigation") {
                !hasWebViewActivityRecord()
            }

            assertProcessPidStays(
                processName = webViewProcessName,
                expectedPid = originalWebViewPid,
                durationMillis = POST_DESTROY_OBSERVATION_MILLIS,
            )
            assertProcessAlive(hostProcessName, hostPid)

            val reopenedWebViewPid = launchWebViewAndAwaitPid()
            assertEquals(
                "Reopening WebViewActivity must reuse the process that survived onDestroy()",
                originalWebViewPid,
                reopenedWebViewPid,
            )
            assertProcessAlive(hostProcessName, hostPid)
        } finally {
            finishWebViewIfPresent()
        }
    }

    @Test
    fun configurationRecreationDoesNotReplaceWebViewProcess() {
        val hostPid = Process.myPid()
        val hostProcessName = currentProcessName()
        val originalRotation = currentRotation()
        val changedRotation = when (originalRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> UiAutomation.ROTATION_FREEZE_90
            else -> UiAutomation.ROTATION_FREEZE_0
        }

        try {
            val originalWebViewPid = launchWebViewAndAwaitPid()
            assertTrue(
                "Unable to request a configuration-changing display rotation",
                automation.setRotation(changedRotation),
            )
            waitUntil("display rotation to change") {
                currentRotation() == changedRotation
            }
            assertProcessPidStays(
                processName = webViewProcessName,
                expectedPid = originalWebViewPid,
                durationMillis = CONFIGURATION_RECREATION_OBSERVATION_MILLIS,
            )
            waitUntil("WebViewActivity to resume after configuration recreation") {
                hasWebViewActivityRecord()
            }

            assertEquals(
                "Configuration recreation must not terminate and replace the :webview process",
                originalWebViewPid,
                awaitSingleProcessPid(webViewProcessName),
            )
            assertProcessAlive(hostProcessName, hostPid)
        } finally {
            finishWebViewIfPresent()
            runCatching { automation.setRotation(originalRotation) }
            runCatching { automation.setRotation(UiAutomation.ROTATION_UNFREEZE) }
        }
    }

    private fun launchWebViewAndAwaitPid(): Int {
        context.startActivity(
            Intent(context, WebViewActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        waitUntil("WebViewActivity to appear in the activity stack") {
            hasWebViewActivityRecord()
        }
        return awaitSingleProcessPid(webViewProcessName)
    }

    private fun pressBack() {
        shell("input keyevent KEYCODE_BACK")
    }

    private fun finishWebViewIfPresent() {
        if (hasWebViewActivityRecord()) {
            pressBack()
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
    }

    private fun hasWebViewActivityRecord(): Boolean =
        shell("dumpsys activity activities")
            .lineSequence()
            .any(ACTIVITY_RECORD_PATTERN::containsMatchIn)

    private fun awaitSingleProcessPid(processName: String): Int {
        var observedPids = emptyList<Int>()
        waitUntil("exactly one $processName process to be running") {
            observedPids = processPids(processName)
            observedPids.size == 1
        }
        return observedPids.single()
    }

    private fun requireSingleProcessPid(processName: String): Int {
        val pids = processPids(processName)
        assertEquals("Expected one live $processName process, found $pids", 1, pids.size)
        return pids.single()
    }

    private fun assertProcessAlive(processName: String, expectedPid: Int) {
        assertEquals("The instrumentation host PID changed", expectedPid, Process.myPid())
        assertTrue(
            "Instrumentation host process $processName/$expectedPid is not reported alive",
            expectedPid in processPids(processName),
        )
    }

    private fun assertProcessPidStays(
        processName: String,
        expectedPid: Int,
        durationMillis: Long,
    ) {
        val deadline = SystemClock.elapsedRealtime() + durationMillis
        do {
            assertEquals(
                "Process $processName was terminated or replaced during lifecycle teardown",
                expectedPid,
                requireSingleProcessPid(processName),
            )
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        } while (SystemClock.elapsedRealtime() < deadline)
    }

    private fun processPids(processName: String): List<Int> =
        shell("pidof $processName")
            .trim()
            .split(Regex("\\s+"))
            .mapNotNull(String::toIntOrNull)

    private fun currentProcessName(): String =
        File("/proc/self/cmdline").readText().trimEnd('\u0000')

    @Suppress("DEPRECATION")
    private fun currentRotation(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay.rotation
    }

    private fun waitUntil(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) {
                return
            }
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError("Timed out waiting for $description")
    }

    private fun shell(command: String): String {
        val descriptor = automation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val POLL_INTERVAL_MILLIS = 100L
        const val POST_DESTROY_OBSERVATION_MILLIS = 500L
        const val CONFIGURATION_RECREATION_OBSERVATION_MILLIS = 1_500L
        val ACTIVITY_RECORD_PATTERN = Regex("Hist\\s+#\\d+:.*ActivityRecord\\{.*WebViewActivity")
    }
}
