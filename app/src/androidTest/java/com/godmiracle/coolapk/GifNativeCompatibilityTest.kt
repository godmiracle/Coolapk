package com.godmiracle.coolapk

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pl.droidsonroids.gif.GifDrawable

@RunWith(AndroidJUnit4::class)
class GifNativeCompatibilityTest {
    @Test
    fun gifDrawableLoadsIn16KbEnvironment() {
        val gifBytes = Base64.decode(
            "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
            Base64.DEFAULT
        )
        val drawable = GifDrawable(gifBytes)
        try {
            assertTrue(drawable.numberOfFrames > 0)
            assertFalse(drawable.isRecycled)
        } finally {
            drawable.recycle()
        }
    }
}
