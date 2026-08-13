package com.godmiracle.coolapk.ui.main

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
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
import com.godmiracle.coolapk.ui.follow.FollowPagerFragment
import com.godmiracle.coolapk.ui.home.HomeFragment
import com.godmiracle.coolapk.ui.my.MyFragment
import com.godmiracle.coolapk.ui.search.SearchActivity
import com.godmiracle.coolapk.util.ActivityCollector
import com.godmiracle.coolapk.util.IntentUtil
import com.godmiracle.coolapk.util.PrefManager
import com.godmiracle.coolapk.view.LiquidGlassFrameLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), IOnBottomClickContainer {

    private val viewModel by viewModels<MainViewModel>()
    private val navViewBehavior by lazy { HideBottomViewOnScrollBehavior<FrameLayout>() }
    override var controller: IOnBottomClickListener? = null
    private lateinit var navView: NavigationBarView
    private val isLogin by lazy { PrefManager.isLogin }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCollector.addActivity(this)

        navView = binding.bottomNav as NavigationBarView

        // 竖屏布局提供玻璃底栏，横屏布局继续使用原有 NavigationRailView。
        val bottomNavSurface = binding.root.findViewById<LiquidGlassFrameLayout>(
            R.id.bottomNavSurface
        )
        val searchActionSurface = binding.root.findViewById<LiquidGlassFrameLayout>(
            R.id.searchActionSurface
        )
        bottomNavSurface?.apply {
            setBackdropSource(binding.viewPager)
            setBlurRadiusDp(18f)
            setSurfaceAlpha(0.28f)
            setCornerRadiusDp(30f)
        }
        searchActionSurface?.apply {
            setBackdropSource(binding.viewPager)
            setBlurRadiusDp(20f)
            setSurfaceAlpha(0.32f)
            setCornerRadiusDp(28f)
            setOnClickListener {
                IntentUtil.startActivity<SearchActivity>(this@MainActivity) {}
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        if (viewModel.isInit) {
            viewModel.isInit = false
            genData()
        }

        binding.viewPager.apply {
            offscreenPageLimit = 2
            adapter = object : FragmentStateAdapter(this@MainActivity) {
                override fun getItemCount() = 3
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> HomeFragment()
                        1 -> FollowPagerFragment.newInstance(
                            uid = PrefManager.uid,
                            type = "follow",
                            embedded = true
                        )
                        else -> MyFragment()
                    }
                }
            }

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    bottomNavSurface?.refreshBackdrop()
                    searchActionSurface?.refreshBackdrop()
                    when (position) {
                        0 -> onBackPressedCallback.isEnabled = false
                        1 -> onBackPressedCallback.isEnabled = true
                        2 -> onBackPressedCallback.isEnabled = true
                    }
                }
            })
            isUserInputEnabled = false
        }

        navView.apply {
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.navigation_home -> {
                        if (binding.viewPager.currentItem == 0)
                            controller?.onReturnTop()
                        else
                            binding.viewPager.setCurrentItem(0, true)
                        true
                    }

                    R.id.navigation_follow -> {
                        binding.viewPager.setCurrentItem(1, true)
                        true
                    }

                    R.id.navigation_me -> {
                        binding.viewPager.setCurrentItem(2, true)
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

        (binding.bottomNavContainer.layoutParams as? CoordinatorLayout.LayoutParams)?.let {
            it.behavior = navViewBehavior
            binding.bottomNavContainer.layoutParams = it
            fixNavigationContainerInsets(binding.bottomNavContainer)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, windowInsets ->
            if (view is BottomNavigationView)
                view.updatePadding(bottom = 0)
            windowInsets
        }

    }

    private fun genData() {
        viewModel.fetchAppInfo("com.coolapk.market")
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (binding.viewPager.currentItem != 0) {
                this.isEnabled = false
                showNavigationView()
                navView.selectedItemId = navView.menu.getItem(0).itemId
            }
        }
    }

    fun showNavigationView() {
        if (binding.bottomNavContainer.layoutParams is CoordinatorLayout.LayoutParams &&
            navViewBehavior.isScrolledDown
        )
            navViewBehavior.slideUp(binding.bottomNavContainer, true)
    }

    fun hideNavigationView() {
        if (binding.bottomNavContainer.layoutParams is CoordinatorLayout.LayoutParams &&
            navViewBehavior.isScrolledUp
        )
            navViewBehavior.slideDown(binding.bottomNavContainer, true)
    }

    /**
     * 滚动过程中隐藏浮动底栏，列表进入空闲状态后再展示，避免根据滑动方向产生不一致的状态。
     */
    fun onContentScrollStateChanged(newState: Int) {
        when (newState) {
            RecyclerView.SCROLL_STATE_DRAGGING,
            RecyclerView.SCROLL_STATE_SETTLING -> hideNavigationView()

            RecyclerView.SCROLL_STATE_IDLE -> showNavigationView()
        }
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
            view.updatePadding(bottom = 0)

            val bottomMargin =
                resources.getDimensionPixelSize(R.dimen.miuix_floating_navigation_bottom_padding) +
                    (navigationBarsInsets?.bottom ?: 0)
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
        super.onDestroy()
        ActivityCollector.removeActivity(this)
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isInit && isLogin) {
            with(System.currentTimeMillis()) {
                if (this - viewModel.lastCheck >= 5 * 60 * 1000) {
                    viewModel.lastCheck = this
                    viewModel.onCheckCount()
                }
            }
        }
    }

}
