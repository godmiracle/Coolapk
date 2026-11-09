package com.godmiracle.coolapk.ui.main

import android.view.View
import android.widget.ScrollView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.godmiracle.coolapk.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationInsetsInstrumentedTest {
    @Test
    fun floatingBarKeepsLastContentAboveItsBottomInset() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val content = View(activity).apply { setPadding(0, 0, 0, 7) }
                activity.applyFloatingNavigationContentInset(content)
                val systemBottom = 56
                val insets = WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, systemBottom))
                    .build()
                val bar = activity.resources.getDimensionPixelSize(R.dimen.miuix_floating_navigation_height)
                val gap = activity.resources.getDimensionPixelSize(R.dimen.miuix_floating_navigation_bottom_padding)

                ViewCompat.dispatchApplyWindowInsets(content, insets)
                assertEquals(7 + systemBottom + gap + bar, content.paddingBottom)
                ViewCompat.dispatchApplyWindowInsets(content, insets)
                assertEquals(7 + systemBottom + gap + bar, content.paddingBottom)

                val noInsets = WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                    .build()
                ViewCompat.dispatchApplyWindowInsets(content, noInsets)
                val fallbackGap = activity.resources.getDimensionPixelSize(R.dimen.ksu_navigation_bottom_without_insets)
                assertEquals(7 + fallbackGap + bar, content.paddingBottom)
            }

            scenario.onActivity { it.binding.viewPager.setCurrentItem(3, false) }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val fragment = requireNotNull(activity.supportFragmentManager.findFragmentByTag("f3"))
                val content = requireNotNull(fragment.requireView().findViewById<ScrollView>(R.id.content))
                val systemBottom = ViewCompat.getRootWindowInsets(content)
                    ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
                val original = (24f * activity.resources.displayMetrics.density).toInt()
                val gap = activity.resources.getDimensionPixelSize(
                    if (systemBottom > 0) R.dimen.miuix_floating_navigation_bottom_padding
                    else R.dimen.ksu_navigation_bottom_without_insets
                )
                val bar = activity.resources.getDimensionPixelSize(R.dimen.miuix_floating_navigation_height)
                assertEquals(original + systemBottom + gap + bar, content.paddingBottom)
            }
        }
    }
}
