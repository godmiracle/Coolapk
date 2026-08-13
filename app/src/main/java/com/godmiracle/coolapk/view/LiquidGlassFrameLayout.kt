package com.godmiracle.coolapk.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import kotlin.math.min

/**
 * XML/View 系统中的轻量液态玻璃容器。
 *
 * Android 12 / API 31 及以上会把指定背景 View 重新绘制到独立层并施加 RenderEffect
 * 模糊，前景子 View（文字、图标和按钮）保持锐利。应用最低支持 Android 12，
 * 因此这里直接使用平台模糊能力，不再维护旧系统的半透明回退路径。
 */
class LiquidGlassFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backdropView = BackdropView(context)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(0.75f)
    }
    private val borderRect = RectF()
    private var backdropSource: View? = null
    private var scrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null
    private var cornerRadiusPx = dp(30f)

    init {
        setWillNotDraw(false)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    view.height,
                    cornerRadiusPx.coerceAtMost(min(view.width, view.height) / 2f)
                )
            }
        }
        backdropView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        backdropView.isClickable = false
        addView(
            backdropView,
            0,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        updateSurfaceColors()
    }

    fun setBackdropSource(source: View?) {
        removeScrollListener()
        backdropSource = source
        backdropView.source = source
        if (source != null) {
            scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
                backdropView.invalidate()
            }
            source.viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
        }
        refreshBackdrop()
    }

    fun setBlurRadiusDp(radiusDp: Float) {
        backdropView.setBlurRadiusPx(dp(radiusDp))
    }

    fun setSurfaceAlpha(alpha: Float) {
        backdropView.surfaceAlpha = alpha.coerceIn(0f, 1f)
        backdropView.invalidate()
    }

    fun setCornerRadiusDp(radiusDp: Float) {
        cornerRadiusPx = dp(radiusDp).coerceAtLeast(0f)
        invalidateOutline()
        invalidate()
    }

    fun refreshBackdrop() {
        backdropView.invalidate()
        postInvalidateOnAnimation()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (width <= 0 || height <= 0) return

        val inset = borderPaint.strokeWidth / 2f
        borderRect.set(inset, inset, width - inset, height - inset)
        canvas.drawRoundRect(
            borderRect,
            cornerRadiusPx.coerceAtMost(min(width, height) / 2f),
            cornerRadiusPx.coerceAtMost(min(width, height) / 2f),
            borderPaint
        )

        // 顶部细高光是液态玻璃的边缘光，不使用阴影堆叠，避免内容产生浮空感。
        val highlightInset = inset + dp(0.8f)
        borderRect.set(
            highlightInset,
            highlightInset,
            width - highlightInset,
            height - highlightInset
        )
        canvas.drawRoundRect(
            borderRect,
            cornerRadiusPx.coerceAtMost(min(width, height) / 2f),
            cornerRadiusPx.coerceAtMost(min(width, height) / 2f),
            highlightPaint
        )
    }

    override fun onDetachedFromWindow() {
        removeScrollListener()
        super.onDetachedFromWindow()
    }

    private fun removeScrollListener() {
        val source = backdropSource
        val listener = scrollChangedListener
        if (source != null && listener != null && source.viewTreeObserver.isAlive) {
            source.viewTreeObserver.removeOnScrollChangedListener(listener)
        }
        scrollChangedListener = null
    }

    private fun updateSurfaceColors() {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val surface = resolveThemeColor(android.R.attr.colorBackground, Color.WHITE)
        val foreground = Color.WHITE
        borderPaint.color = withAlpha(foreground, if (night) 120 else 178)
        highlightPaint.color = withAlpha(foreground, if (night) 74 else 142)
        backdropView.surfaceColor = surface
    }

    private fun resolveThemeColor(attribute: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        if (!context.theme.resolveAttribute(attribute, typedValue, true)) return fallback
        return when {
            typedValue.resourceId != 0 -> runCatching {
                resources.getColor(typedValue.resourceId, context.theme)
            }.getOrDefault(fallback)

            else -> typedValue.data
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private class BackdropView(context: Context) : View(context) {
        var source: View? = null
        var surfaceColor: Int = Color.WHITE
            set(value) {
                field = value
                updatePaintColor()
            }
        var surfaceAlpha: Float = 0.28f
            set(value) {
                field = value
                updatePaintColor()
            }
        private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val sourceLocation = IntArray(2)
        private val backdropLocation = IntArray(2)
        private var blurRadiusPx = 0f

        init {
            setWillNotDraw(false)
            updatePaintColor()
        }

        fun setBlurRadiusPx(radiusPx: Float) {
            blurRadiusPx = radiusPx.coerceAtLeast(0f)
            setRenderEffect(
                if (blurRadiusPx > 0f) {
                    RenderEffect.createBlurEffect(
                        blurRadiusPx,
                        blurRadiusPx,
                        Shader.TileMode.CLAMP
                    )
                } else {
                    null
                }
            )
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val sourceView = source
            if (sourceView != null &&
                sourceView.width > 0 && sourceView.height > 0
            ) {
                sourceView.getLocationOnScreen(sourceLocation)
                getLocationOnScreen(backdropLocation)
                canvas.save()
                canvas.translate(
                    (sourceLocation[0] - backdropLocation[0]).toFloat(),
                    (sourceLocation[1] - backdropLocation[1]).toFloat()
                )
                sourceView.draw(canvas)
                canvas.restore()
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), surfacePaint)
            } else {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), surfacePaint)
            }
        }

        private fun updatePaintColor() {
            val alpha = surfaceAlpha
            surfacePaint.color = (surfaceColor and 0x00FFFFFF) or
                ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
        }
    }
}
