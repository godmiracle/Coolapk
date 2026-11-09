package com.godmiracle.coolapk.view.ksu

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import kotlin.math.round

/** Supplies the existing ViewPager content to the same Miuix Backdrop used by KernelSU. */
@Stable
internal class ViewBackdrop(
    private val source: View,
    private val backgroundColor: Color,
) : Backdrop {
    override val isCoordinatesDependent: Boolean = true
    override var offsetResidualX: Float = 0f
        private set
    override var offsetResidualY: Float = 0f
        private set

    private val sourceLocation = IntArray(2)
    private val inverseLayerScope = InverseLayerScope()
    private val revision = mutableIntStateOf(0)
    private var observer: ViewTreeObserver? = null
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { refresh() }
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { refresh() }
    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (source.isDirty) refresh()
        true
    }
    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            bindObserver()
            refresh()
        }

        override fun onViewDetachedFromWindow(view: View) {
            unbindObserver()
            refresh()
        }
    }

    fun attach() {
        source.addOnAttachStateChangeListener(attachListener)
        if (source.isAttachedToWindow) bindObserver()
    }

    fun detach() {
        unbindObserver()
        source.removeOnAttachStateChangeListener(attachListener)
    }

    fun refresh() {
        revision.intValue = if (revision.intValue == Int.MAX_VALUE) 0 else revision.intValue + 1
    }

    private fun bindObserver() {
        unbindObserver()
        source.viewTreeObserver.takeIf { it.isAlive }?.let {
            observer = it
            it.addOnScrollChangedListener(scrollListener)
            it.addOnGlobalLayoutListener(layoutListener)
            it.addOnPreDrawListener(preDrawListener)
        }
    }

    private fun unbindObserver() {
        observer?.takeIf { it.isAlive }?.let {
            it.removeOnScrollChangedListener(scrollListener)
            it.removeOnGlobalLayoutListener(layoutListener)
            it.removeOnPreDrawListener(preDrawListener)
        }
        observer = null
    }

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        // Reading snapshot state here invalidates Miuix's recorded backdrop on source changes.
        if (revision.intValue < 0) return
        drawRect(backgroundColor)
        if (coordinates == null || !source.isAttachedToWindow || source.width <= 0 || source.height <= 0) return

        source.getLocationInWindow(sourceLocation)
        val location = coordinates.positionInWindow()
        val offsetX = location.x - sourceLocation[0]
        val offsetY = location.y - sourceLocation[1]
        val consumerSize = (density as? BackdropEffectScope)?.size ?: size

        withTransform({
            if (layerBlock != null) {
                inverseLayerScope.reset()
                with(inverseLayerScope) { inverseTransform(density, consumerSize, layerBlock) }
            }
            if (downscaleFactor > 1) {
                val inverse = 1f / downscaleFactor
                val scaledX = offsetX * inverse
                val scaledY = offsetY * inverse
                val roundedX = round(scaledX * 0.5f).toInt().toFloat() * 2f
                val roundedY = round(scaledY * 0.5f).toInt().toFloat() * 2f
                offsetResidualX = (scaledX - roundedX) * downscaleFactor
                offsetResidualY = (scaledY - roundedY) * downscaleFactor
                translate(-roundedX, -roundedY)
                scale(inverse, inverse, Offset.Zero)
            } else {
                offsetResidualX = 0f
                offsetResidualY = 0f
                translate(-offsetX, -offsetY)
            }
        }) {
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val checkpoint = nativeCanvas.save()
                try {
                    source.draw(nativeCanvas)
                } finally {
                    nativeCanvas.restoreToCount(checkpoint)
                }
            }
        }
    }
}
