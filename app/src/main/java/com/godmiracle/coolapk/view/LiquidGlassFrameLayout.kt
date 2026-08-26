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
import androidx.annotation.ColorInt
import com.google.android.material.R as MaterialR
import com.godmiracle.coolapk.R
import kotlin.math.min

/**
 * XML/View 系统中的共享 GlassSurface 实现。
 *
 * Android 12 / API 31 及以上会把指定内容 sibling 重新绘制到独立层并施加
 * RenderEffect 模糊，前景子 View（文字、图标和按钮）保持锐利。source 不可用时
 * 仍绘制主题着色表面，保证 XML inflation 和页面切换阶段不会出现黑块或崩溃。
 */
class LiquidGlassFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backdropView = BackdropView(context)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val borderRect = RectF()
    private val attributeSet = attrs
    private var backdropSource: View? = null
    private var xmlBackdropSourceId = View.NO_ID
    private var scrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null
    private var sourceLayoutChangeListener: View.OnLayoutChangeListener? = null
    private var sourceAttachStateChangeListener: View.OnAttachStateChangeListener? = null
    private var cornerRadiusPx = dp(24f)
    private var borderWidthPx = dp(1f)
    private var highlightWidthPx = dp(0.75f)
    @ColorInt
    private var surfaceColor = resolveThemeColor(
        MaterialR.attr.colorSurface,
        resolveResourceColor(R.color.glass_surface_fallback, Color.WHITE)
    )
    private var surfaceAlpha = 0.24f
    @ColorInt
    private var borderColor = resolveThemeColor(
        MaterialR.attr.colorOnSurface,
        resolveResourceColor(R.color.glass_border_fallback, Color.WHITE)
    )
    @ColorInt
    private var highlightColor = resolveThemeColor(
        MaterialR.attr.colorOnSurface,
        resolveResourceColor(R.color.glass_highlight_fallback, Color.WHITE)
    )
    private var blurRadiusPx = dp(16f)
    private var surfaceColorOverridden = false
    private var surfaceAlphaOverridden = false
    private var blurRadiusOverridden = false
    private var cornerRadiusOverridden = false
    private var borderColorOverridden = false
    private var borderWidthOverridden = false
    private var highlightColorOverridden = false
    private var highlightWidthOverridden = false

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
        applyStyledAttributes()
        applySurfaceState()
    }

    fun setBackdropSource(source: View?) {
        removeSourceListeners()
        backdropSource = source?.takeUnless(::isUnsafeBackdropSource)
        backdropView.source = backdropSource
        bindSourceListeners()
        refreshBackdrop()
    }

    fun setBlurRadiusDp(radiusDp: Float) {
        blurRadiusOverridden = true
        blurRadiusPx = dp(radiusDp)
        backdropView.setBlurRadiusPx(blurRadiusPx)
    }

    fun setSurfaceAlpha(alpha: Float) {
        surfaceAlphaOverridden = true
        surfaceAlpha = alpha.coerceIn(0f, 1f)
        backdropView.surfaceAlpha = surfaceAlpha
        backdropView.invalidate()
    }

    fun setCornerRadiusDp(radiusDp: Float) {
        cornerRadiusOverridden = true
        cornerRadiusPx = dp(radiusDp).coerceAtLeast(0f)
        invalidateOutline()
        invalidate()
    }

    fun setSurfaceColor(@ColorInt color: Int) {
        surfaceColorOverridden = true
        surfaceColor = color
        backdropView.surfaceColor = color
        refreshBackdrop()
    }

    fun refreshBackdrop() {
        backdropView.invalidate()
        postInvalidateOnAnimation()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (width <= 0 || height <= 0) return

        val radius = cornerRadiusPx.coerceAtMost(min(width, height) / 2f)
        if (borderWidthPx > 0f) {
            val inset = borderWidthPx / 2f
            borderRect.set(inset, inset, width - inset, height - inset)
            canvas.drawRoundRect(borderRect, radius, radius, borderPaint)
        }

        // 顶部细高光是液态玻璃的边缘光，不使用阴影堆叠，避免内容产生浮空感。
        if (highlightWidthPx > 0f) {
            val highlightInset = borderWidthPx + highlightWidthPx / 2f
            borderRect.set(highlightInset, highlightInset, width - highlightInset, height - highlightInset)
            val highlightRadius = (cornerRadiusPx - highlightInset)
                .coerceAtLeast(0f)
                .coerceAtMost(min(width, height) / 2f)
            canvas.drawRoundRect(borderRect, highlightRadius, highlightRadius, highlightPaint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (backdropSource == null && xmlBackdropSourceId != View.NO_ID) {
            rootView.findViewById<View>(xmlBackdropSourceId)?.let {
                setBackdropSource(it)
                return
            }
        }
        bindSourceListeners()
        refreshBackdrop()
    }

    override fun onDetachedFromWindow() {
        removeSourceListeners()
        super.onDetachedFromWindow()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyStyledAttributes()
        applySurfaceState()
        refreshBackdrop()
    }

    private fun applyStyledAttributes() {
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.GlassSurface,
            0,
            R.style.Widget_Coolapk_GlassSurface
        )
        try {
            if (!surfaceColorOverridden) {
                surfaceColor = typedArray.getColor(
                    R.styleable.GlassSurface_glassSurfaceColor,
                    surfaceColor
                )
            }
            if (!surfaceAlphaOverridden) {
                surfaceAlpha = typedArray.getFloat(
                    R.styleable.GlassSurface_glassSurfaceAlpha,
                    surfaceAlpha
                ).coerceIn(0f, 1f)
            }
            if (!blurRadiusOverridden) {
                blurRadiusPx = typedArray.getDimension(
                    R.styleable.GlassSurface_glassBlurRadius,
                    blurRadiusPx
                ).coerceAtLeast(0f)
            }
            if (!cornerRadiusOverridden) {
                cornerRadiusPx = typedArray.getDimension(
                    R.styleable.GlassSurface_glassCornerRadius,
                    cornerRadiusPx
                ).coerceAtLeast(0f)
            }
            if (!borderColorOverridden) {
                borderColor = typedArray.getColor(
                    R.styleable.GlassSurface_glassBorderColor,
                    borderColor
                )
            }
            if (!borderWidthOverridden) {
                borderWidthPx = typedArray.getDimension(
                    R.styleable.GlassSurface_glassBorderWidth,
                    borderWidthPx
                ).coerceAtLeast(0f)
            }
            if (!highlightColorOverridden) {
                highlightColor = typedArray.getColor(
                    R.styleable.GlassSurface_glassHighlightColor,
                    highlightColor
                )
            }
            if (!highlightWidthOverridden) {
                highlightWidthPx = typedArray.getDimension(
                    R.styleable.GlassSurface_glassHighlightWidth,
                    highlightWidthPx
                ).coerceAtLeast(0f)
            }
            if (xmlBackdropSourceId == View.NO_ID) {
                xmlBackdropSourceId = typedArray.getResourceId(
                    R.styleable.GlassSurface_glassBackdropSource,
                    View.NO_ID
                )
            }
        } finally {
            typedArray.recycle()
        }
    }

    private fun applySurfaceState() {
        borderPaint.strokeWidth = borderWidthPx
        highlightPaint.strokeWidth = highlightWidthPx
        borderPaint.color = colorWithDefaultAlpha(borderColor, if (isNightMode()) 120 else 178)
        highlightPaint.color = colorWithDefaultAlpha(highlightColor, if (isNightMode()) 74 else 142)
        backdropView.surfaceColor = surfaceColor
        backdropView.surfaceAlpha = surfaceAlpha
        backdropView.setBlurRadiusPx(blurRadiusPx)
        invalidateOutline()
        invalidate()
    }

    private fun bindSourceListeners() {
        val source = backdropSource ?: return
        if (!isAttachedToWindow) return

        val scrollListener = ViewTreeObserver.OnScrollChangedListener {
            refreshBackdrop()
        }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            refreshBackdrop()
        }
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.viewTreeObserver.addOnScrollChangedListener(scrollListener)
                refreshBackdrop()
            }

            override fun onViewDetachedFromWindow(view: View) {
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
                }
                refreshBackdrop()
            }
        }

        scrollChangedListener = scrollListener
        sourceLayoutChangeListener = layoutListener
        sourceAttachStateChangeListener = attachListener
        source.addOnLayoutChangeListener(layoutListener)
        source.addOnAttachStateChangeListener(attachListener)
        if (source.isAttachedToWindow) {
            source.viewTreeObserver.addOnScrollChangedListener(scrollListener)
        }
    }

    private fun removeSourceListeners() {
        val source = backdropSource
        val listener = scrollChangedListener
        if (source != null && listener != null && source.viewTreeObserver.isAlive) {
            source.viewTreeObserver.removeOnScrollChangedListener(listener)
        }
        source?.let { currentSource ->
            sourceLayoutChangeListener?.let(currentSource::removeOnLayoutChangeListener)
            sourceAttachStateChangeListener?.let(currentSource::removeOnAttachStateChangeListener)
        }
        scrollChangedListener = null
        sourceLayoutChangeListener = null
        sourceAttachStateChangeListener = null
    }

    private fun isUnsafeBackdropSource(source: View): Boolean {
        if (source === this) return true

        var current: View? = parent as? View
        while (current != null) {
            if (current === source) return true
            current = current.parent as? View
        }

        current = source
        while (current != null) {
            if (current === this) return true
            current = current.parent as? View
        }
        return false
    }

    private fun isNightMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    @ColorInt
    private fun colorWithDefaultAlpha(@ColorInt color: Int, defaultAlpha: Int): Int =
        if (Color.alpha(color) == 255) withAlpha(color, defaultAlpha) else color

    @ColorInt
    private fun resolveResourceColor(resourceId: Int, @ColorInt fallback: Int): Int =
        runCatching { resources.getColor(resourceId, context.theme) }.getOrDefault(fallback)

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
                sourceView.width > 0 && sourceView.height > 0 &&
                (!isAttachedToWindow || sourceView.isAttachedToWindow)
            ) {
                val drawn = runCatching {
                    sourceView.getLocationOnScreen(sourceLocation)
                    getLocationOnScreen(backdropLocation)
                    canvas.save()
                    canvas.translate(
                        (sourceLocation[0] - backdropLocation[0]).toFloat(),
                        (sourceLocation[1] - backdropLocation[1]).toFloat()
                    )
                    sourceView.draw(canvas)
                    canvas.restore()
                }.isSuccess
                if (drawn) {
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), surfacePaint)
                    return
                }
            } else {
                drawFallback(canvas)
                return
            }
            drawFallback(canvas)
        }

        private fun drawFallback(canvas: Canvas) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), surfacePaint)
        }

        private fun updatePaintColor() {
            val alpha = surfaceAlpha
            surfacePaint.color = (surfaceColor and 0x00FFFFFF) or
                ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
        }
    }
}
