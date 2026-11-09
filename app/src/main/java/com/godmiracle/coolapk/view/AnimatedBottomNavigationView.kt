package com.godmiracle.coolapk.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * XML 版的 BiliPai 风格浮动导航栏。
 *
 * BiliPai 的 Compose FloatingBottomBar 将导航项和选中胶囊分成独立图层，
 * 让胶囊在槽位之间连续移动，并在落位时做轻微的横纵向回弹。这里保持
 * Material BottomNavigationView 的菜单、无障碍和点击语义，只把静态选中
 * 背景替换为同一交互模型的 View 版本。
 */
class AnimatedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val indicatorBounds = RectF()
    private val animationStartBounds = RectF()
    private val animationTargetBounds = RectF()
    private val itemBounds = Rect()
    private var glassIndicatorSurface: LiquidGlassFrameLayout? = null
    private var glassProgressAnimator: ValueAnimator? = null
    private var glassProgress = 0f
    private var hasIndicatorBounds = false
    private var indicatorAnimator: ValueAnimator? = null
    private var settleAnimator: ValueAnimator? = null
    private var indicatorScaleX = 1f
    private var indicatorScaleY = 1f

    // BiliPai 的槽位移动使用临界阻尼弹簧；PathInterpolator 是 View 动画中的等价实现。
    private val indicatorMotionInterpolator: Interpolator =
        PathInterpolator(0.22f, 0.92f, 0.28f, 1f)

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        // Material NavigationBarView replaces even a transparent XML background with its
        // default MaterialShapeDrawable. The outer LiquidGlassFrameLayout is the only
        // background layer we want, otherwise it leaves a white rectangular surface behind
        // the menu and breaks the floating-bar chrome used by BiliPai.
        background = null
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            post { synchronizeSelectionWithoutAnimation() }
        }
    }

    /** Connects the separate glass pill drawn between the backdrop and navigation items. */
    fun setGlassIndicatorSurface(surface: LiquidGlassFrameLayout?) {
        glassIndicatorSurface = surface
        surface?.setLiquidGlassProgress(glassProgress)
        surface?.visibility = View.INVISIBLE
        post { synchronizeSelectionWithoutAnimation() }
    }

    /** 同步当前选中项，不触发首次进入时的回弹。 */
    fun synchronizeSelectionWithoutAnimation() {
        val itemId = selectedItemIdOrFirstItem()
        val target = findIndicatorBounds(itemId) ?: return
        indicatorAnimator?.cancel()
        settleAnimator?.cancel()
        glassProgressAnimator?.cancel()
        glassProgress = 0f
        glassIndicatorSurface?.setLiquidGlassProgress(glassProgress)
        indicatorBounds.set(target)
        hasIndicatorBounds = true
        indicatorScaleX = 1f
        indicatorScaleY = 1f
        updateGlassIndicatorSurface()
        invalidate()
    }

    /**
     * 触发一次选中胶囊的位移动画和落位回弹。
     * 调用方可以在 NavigationBarView 的选择回调中调用，导航状态仍由 Material 控件维护。
     */
    fun animateSelection(itemId: Int) {
        post {
            val target = findIndicatorBounds(itemId) ?: return@post
            if (!hasIndicatorBounds) {
                indicatorBounds.set(target)
                hasIndicatorBounds = true
                invalidate()
                return@post
            }

            animationStartBounds.set(indicatorBounds)
            animationTargetBounds.set(target)
            indicatorAnimator?.cancel()
            animateGlassProgress(1f, GLASS_PROGRESS_IN_DURATION_MS)
            indicatorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = INDICATOR_MOVE_DURATION_MS
                interpolator = indicatorMotionInterpolator
                addUpdateListener { animator ->
                    val progress = animator.animatedValue as Float
                    interpolateBounds(
                        from = animationStartBounds,
                        to = animationTargetBounds,
                        progress = progress
                    )
                    updateGlassIndicatorSurface()
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (indicatorAnimator === animation) {
                            animateGlassProgress(0f, GLASS_PROGRESS_OUT_DURATION_MS)
                        }
                    }
                })
                start()
            }
            animateSettleRebound()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        compactLabeledItemContent()
    }

    override fun onDetachedFromWindow() {
        indicatorAnimator?.cancel()
        settleAnimator?.cancel()
        glassProgressAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun animateGlassProgress(target: Float, durationMs: Long) {
        glassProgressAnimator?.cancel()
        glassProgressAnimator = ValueAnimator.ofFloat(glassProgress, target).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                glassProgress = animator.animatedValue as Float
                glassIndicatorSurface?.setLiquidGlassProgress(glassProgress)
            }
            start()
        }
    }

    private fun animateSettleRebound() {
        settleAnimator?.cancel()
        settleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SETTLE_REBOUND_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val transform = resolveSettleTransform(progress)
                indicatorScaleX = transform.first
                indicatorScaleY = transform.second
                updateGlassIndicatorSurface()
                invalidate()
            }
            start()
        }
    }

    private fun resolveSettleTransform(progress: Float): Pair<Float, Float> {
        val clamped = progress.coerceIn(0f, 1f)
        val compressionEnd = 0.2f
        if (clamped <= compressionEnd) {
            val compressionProgress = (clamped / compressionEnd).coerceIn(0f, 1f)
            val eased = 1f - (1f - compressionProgress) * (1f - compressionProgress) *
                (1f - compressionProgress)
            return (1f - 0.035f * eased) to (1f + 0.028f * eased)
        }

        // 与 BiliPai 的落位波形保持同一组阻尼/回弹比例，避免胶囊突然停止。
        val releaseProgress = ((clamped - compressionEnd) / (1f - compressionEnd))
            .coerceIn(0f, 1f)
        val damping = ((1f - releaseProgress) * exp(-3.2 * releaseProgress)).toFloat()
        val reboundWave = damping * sin(PI * releaseProgress).toFloat()
        return (1f + 0.085f * reboundWave) to (1f + 0.075f * reboundWave)
    }

    private fun interpolateBounds(from: RectF, to: RectF, progress: Float) {
        indicatorBounds.set(
            lerp(from.left, to.left, progress),
            lerp(from.top, to.top, progress),
            lerp(from.right, to.right, progress),
            lerp(from.bottom, to.bottom, progress)
        )
    }

    private fun updateGlassIndicatorSurface() {
        val surface = glassIndicatorSurface ?: return
        if (!hasIndicatorBounds || indicatorBounds.isEmpty) {
            surface.visibility = View.INVISIBLE
            return
        }

        val width = indicatorBounds.width().roundToInt().coerceAtLeast(1)
        val height = indicatorBounds.height().roundToInt().coerceAtLeast(1)
        surface.layoutParams?.let { params ->
            if (params.width != width || params.height != height) {
                params.width = width
                params.height = height
                surface.layoutParams = params
            }
        }
        surface.translationX = indicatorBounds.left
        surface.translationY = indicatorBounds.top
        surface.scaleX = indicatorScaleX
        surface.scaleY = indicatorScaleY
        surface.visibility = View.VISIBLE
    }

    private fun findIndicatorBounds(itemId: Int): RectF? {
        val itemView = findItemView(this, itemId) ?: return null
        itemBounds.set(0, 0, itemView.width, itemView.height)
        offsetDescendantRectToMyCoords(itemView, itemBounds)

        val horizontalInset = dp(4f)
        val verticalInset = dp(4f)
        val left = itemBounds.left + horizontalInset
        val top = itemBounds.top + verticalInset
        val right = itemBounds.right - horizontalInset
        val bottom = itemBounds.bottom - verticalInset
        if (right <= left || bottom <= top) return null
        return RectF(left, top, right, bottom)
    }

    private fun findItemView(parent: ViewGroup, itemId: Int): View? {
        val menuIndex = (0 until menu.size()).firstOrNull {
            menu.getItem(it).itemId == itemId
        } ?: return null
        val menuView = findMenuView(parent) ?: return null
        return menuView.getChildAt(menuIndex)
    }

    private fun findMenuView(parent: ViewGroup): ViewGroup? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is ViewGroup && child.childCount == menu.size()) return child
            if (child is ViewGroup) {
                val result = findMenuView(child)
                if (result != null) return result
            }
        }
        return null
    }

    /**
     * Material's labeled item pins the icon to the top and the label to the bottom. BiliPai
     * instead centers one Column with a 1dp gap, so translate both Material subtrees as a pair.
     * The item view itself remains untouched and keeps the original touch/accessibility bounds.
     */
    private fun compactLabeledItemContent() {
        val menuView = findMenuView(this) ?: return
        val gap = dp(1f)
        for (index in 0 until minOf(menuView.childCount, menu.size())) {
            val itemView = menuView.getChildAt(index) as? ViewGroup ?: continue
            val iconContainer = itemView.findViewById<View>(
                com.google.android.material.R.id.navigation_bar_item_icon_container
            ) ?: continue
            val labelGroup = itemView.findViewById<View>(
                com.google.android.material.R.id.navigation_bar_item_labels_group
            ) ?: continue
            if (labelGroup.visibility != View.VISIBLE || itemView.height <= 0) continue

            val contentHeight = iconContainer.height + gap + labelGroup.height
            val contentTop = (itemView.height - contentHeight) / 2f
            iconContainer.translationY = contentTop - iconContainer.top
            labelGroup.translationY =
                contentTop + iconContainer.height + gap - labelGroup.top
        }
    }

    private fun selectedItemIdOrFirstItem(): Int {
        if (selectedItemId != View.NO_ID) return selectedItemId
        return if (menu.size() > 0) menu.getItem(0).itemId else View.NO_ID
    }

    private fun lerp(start: Float, stop: Float, progress: Float): Float =
        start + (stop - start) * progress

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        const val INDICATOR_MOVE_DURATION_MS = 320L
        const val SETTLE_REBOUND_DURATION_MS = 260L
        const val GLASS_PROGRESS_IN_DURATION_MS = 90L
        const val GLASS_PROGRESS_OUT_DURATION_MS = 220L
    }
}
