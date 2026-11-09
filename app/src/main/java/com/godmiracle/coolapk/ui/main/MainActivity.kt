package com.godmiracle.coolapk.ui.main

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.ActivityMainBinding
import com.godmiracle.coolapk.ui.base.BaseActivity
import com.godmiracle.coolapk.ui.discover.DiscoverFragment
import com.godmiracle.coolapk.ui.follow.FollowPagerFragment
import com.godmiracle.coolapk.ui.home.HomeFragment
import com.godmiracle.coolapk.ui.my.MyFragment
import com.godmiracle.coolapk.ui.search.SearchActivity
import com.godmiracle.coolapk.util.ActivityCollector
import com.godmiracle.coolapk.util.IntentUtil
import com.godmiracle.coolapk.view.ksu.KsuBottomNavigation
import com.godmiracle.coolapk.view.ksu.ViewBackdrop
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.R as MaterialR

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), IOnBottomClickContainer {

    override var controller: IOnBottomClickListener? = null
    private var navView: NavigationBarView? = null
    private var glassBackdrop: ViewBackdrop? = null
    private var selectedTabIndex by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCollector.addActivity(this)

        navView = binding.bottomNav as? NavigationBarView

        // 竖屏使用 KernelSU 的 Compose 玻璃底栏；横屏继续使用原有 NavigationRailView。
        binding.floatingNavigation?.let { composeView ->
            val backdrop = ViewBackdrop(
                source = binding.viewPager,
                backgroundColor = Color(
                    MaterialColors.getColor(this, MaterialR.attr.colorSurface, android.graphics.Color.BLACK)
                ),
            )
            glassBackdrop = backdrop
            backdrop.attach()
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            composeView.setContent {
                KsuBottomNavigation(
                    selectedIndex = selectedTabIndex,
                    backdrop = backdrop,
                    onSelected = ::selectTab,
                    onReselected = { index -> if (index == 0) controller?.onReturnTop() },
                    onSearch = { IntentUtil.startActivity<SearchActivity>(this@MainActivity) {} },
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        binding.viewPager.apply {
            offscreenPageLimit = 3
            adapter = object : FragmentStateAdapter(this@MainActivity) {
                override fun getItemCount() = 4
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> HomeFragment()
                        1 -> DiscoverFragment()
                        2 -> FollowPagerFragment.newInstance(
                            uid = "",
                            type = "follow",
                            embedded = true
                        )
                        else -> MyFragment()
                    }
                }
            }

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    glassBackdrop?.refresh()
                }

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    selectedTabIndex = when (position) {
                        1 -> 2
                        2 -> 1
                        else -> position
                    }
                    glassBackdrop?.refresh()
                    when (position) {
                        0 -> onBackPressedCallback.isEnabled = false
                        else -> onBackPressedCallback.isEnabled = true
                    }
                }
            })
            isUserInputEnabled = false
        }

        navView?.apply {
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.navigation_home -> {
                        if (binding.viewPager.currentItem == 0) controller?.onReturnTop()
                        else selectTab(0)
                        true
                    }

                    R.id.navigation_discover -> {
                        selectTab(2)
                        true
                    }

                    R.id.navigation_follow -> {
                        selectTab(1)
                        true
                    }

                    R.id.navigation_me -> {
                        selectTab(3)
                        true
                    }

                    R.id.navigation_search -> {
                        IntentUtil.startActivity<SearchActivity>(this@MainActivity) {}
                        false
                    }

                    else -> false
                }
            }
            setOnClickListener { /*Do nothing*/ }
        }

        if (binding.bottomNavContainer.layoutParams is CoordinatorLayout.LayoutParams) {
            fixNavigationContainerInsets(binding.bottomNavContainer)
        }
    }

    private fun selectTab(index: Int) {
        val position = when (index) {
            0 -> 0
            1 -> 2
            2 -> 1
            3 -> 3
            else -> return
        }
        selectedTabIndex = index
        if (binding.viewPager.currentItem != position) {
            binding.viewPager.setCurrentItem(position, true)
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (binding.viewPager.currentItem != 0) {
                this.isEnabled = false
                showNavigationView()
                val rail = navView
                if (rail != null) rail.selectedItemId = rail.menu.getItem(0).itemId
                else selectTab(0)
            }
        }
    }

    fun showNavigationView() {
        glassBackdrop?.refresh()
    }

    /**
     * 底栏始终显示；列表停止滚动时更新其背后的采样内容。
     */
    fun onContentScrollStateChanged(newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            glassBackdrop?.refresh()
        }
    }

    /** 为常驻浮动底栏留出列表尾部空间，内容滚动时仍可透过玻璃显示。 */
    fun applyFloatingNavigationContentInset(view: View) {
        if (binding.floatingNavigation == null) return
        val originalBottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val rootInsets = ViewCompat.getRootWindowInsets(target) ?: insets
            val systemBottom = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val gap = resources.getDimensionPixelSize(
                if (systemBottom > 0) R.dimen.miuix_floating_navigation_bottom_padding
                else R.dimen.ksu_navigation_bottom_without_insets
            )
            val barHeight = resources.getDimensionPixelSize(R.dimen.miuix_floating_navigation_height)
            target.updatePadding(bottom = originalBottom + systemBottom + gap + barHeight)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    // from LibChecker
    /**
     * 将系统导航栏 inset 只应用到浮岛容器的 bottom margin，避免 Material 导航内容
     * 再额外绘制一层背景或把系统 inset 变成可见的底部空白。
     */
    private fun fixNavigationContainerInsets(view: FrameLayout) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            // 这里不直接使用 windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // 因为它的结果可能受到 insets 传播链上层某环节的影响，出现了错误的 navigationBarsInsets
            val navigationBarsInsets =
                ViewCompat.getRootWindowInsets(view)
                    ?.getInsets(WindowInsetsCompat.Type.systemBars())
            val inset = navigationBarsInsets?.bottom ?: 0
            // 容器为放大动画留出 20dp；可见底栏保持 KSU 的系统栏上方 8dp 间距。
            val offsetDp = if (inset > 0) -12f else 8f
            val bottomMargin = (inset + offsetDp * resources.displayMetrics.density)
                .toInt().coerceAtLeast(0)
            (view.layoutParams as? CoordinatorLayout.LayoutParams)?.let { layoutParams ->
                if (layoutParams.bottomMargin != bottomMargin) {
                    layoutParams.bottomMargin = bottomMargin
                    view.layoutParams = layoutParams
                }
            }
            windowInsets
        }
    }

    override fun onDestroy() {
        glassBackdrop?.detach()
        glassBackdrop = null
        super.onDestroy()
        ActivityCollector.removeActivity(this)
    }

}
