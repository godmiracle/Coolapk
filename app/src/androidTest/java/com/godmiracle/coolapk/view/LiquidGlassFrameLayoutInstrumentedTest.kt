package com.godmiracle.coolapk.view

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.godmiracle.coolapk.test.R as TestR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiquidGlassFrameLayoutInstrumentedTest {

    @Test
    fun xmlInflationAndPublicApiPreserveForegroundSemantics() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        instrumentation.runOnMainSync {
            val root = LayoutInflater.from(instrumentation.context)
                .inflate(TestR.layout.fixture_liquid_glass_surface, null, false)
            val source = root.findViewById<View>(TestR.id.fixture_backdrop_source)
            val surface = root.findViewById<LiquidGlassFrameLayout>(TestR.id.fixture_glass_surface)
            val action = root.findViewById<Button>(TestR.id.fixture_foreground_action)

            surface.setBackdropSource(source)
            surface.refreshBackdrop()
            surface.setBackdropSource(null)
            surface.setBlurRadiusDp(12f)
            surface.refreshBackdrop()

            assertEquals("Glass action", action.contentDescription)
            assertTrue(action.isClickable)

            val backdrop = surface.getChildAt(0)
            assertFalse(backdrop.isClickable)
            assertEquals(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                backdrop.importantForAccessibility,
            )
        }
    }
}
