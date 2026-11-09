package com.godmiracle.coolapk.view.ksu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.godmiracle.coolapk.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.blur.Backdrop

@RunWith(AndroidJUnit4::class)
class KsuBottomNavigationInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val backdrop = object : Backdrop {
        override val isCoordinatesDependent = false
        override fun DrawScope.drawBackdrop(
            density: Density,
            coordinates: LayoutCoordinates?,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
            downscaleFactor: Int,
        ) = drawRect(Color(0xFF6688AA))
    }

    @Test
    fun tapDragReselectAndSearchKeepNavigationSemantics() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val selections = mutableListOf<Int>()
        var homeReselections = 0
        var searches = 0
        composeRule.setContent {
            var selected by remember { mutableIntStateOf(0) }
            KsuBottomNavigation(
                selectedIndex = selected,
                backdrop = backdrop,
                onSelected = { selected = it; selections += it },
                onReselected = { if (it == 0) homeReselections++ },
                onSearch = { searches++ },
            )
        }

        val follow = context.getString(R.string.tab_follow)
        val home = context.getString(R.string.tab_home)
        val me = context.getString(R.string.tab_me)
        composeRule.onNodeWithText(follow).performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(1, selections.last()) }
        composeRule.onNodeWithText(follow).assertIsSelected()

        composeRule.onNodeWithText(follow).performTouchInput {
            down(center)
            moveBy(Offset(width * 2f, 0f))
            up()
        }
        composeRule.runOnIdle { assertEquals(3, selections.last()) }
        composeRule.onNodeWithText(me).assertIsSelected()

        composeRule.onNodeWithText(home).performTouchInput { click() }
        composeRule.onNodeWithText(home).performTouchInput { click() }
        composeRule.onNodeWithContentDescription(context.getString(R.string.search)).performTouchInput { click() }
        val tree = composeRule.onRoot().printToString()
        composeRule.runOnIdle {
            assertEquals(0, selections.last())
            assertEquals(1, homeReselections)
            assertEquals(tree, 1, searches)
        }
    }
}
