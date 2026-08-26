package com.godmiracle.coolapk.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseTablayoutViewpagerBinding
import com.godmiracle.coolapk.ui.home.IOnTabClickContainer
import com.godmiracle.coolapk.ui.home.IOnTabClickListener
import com.godmiracle.coolapk.view.LiquidGlassFrameLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// Toolbar + TabLayout + ViewPager2
abstract class BasePagerFragment : Fragment(), IOnTabClickContainer {

    private var _binding: BaseTablayoutViewpagerBinding? = null
    val binding get() = _binding!!
    override var tabController: IOnTabClickListener? = null
    lateinit var tabList: List<String>
    private var toolbarGlassSurface: LiquidGlassFrameLayout? = null
    private var tabGlassSurface: LiquidGlassFrameLayout? = null
    private var toolbarBackdropSource: View? = null
    private var tabBackdropSource: View? = null
    private var glassSurfacesInitialized = false
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var appBarOffsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null
    private var glassScrollObserver: ViewTreeObserver? = null
    private var glassRootAttachStateChangeListener: View.OnAttachStateChangeListener? = null
    private val glassContentScrollChangedListener =
        ViewTreeObserver.OnScrollChangedListener { refreshGlassSurfaces() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BaseTablayoutViewpagerBinding.inflate(inflater, container, false)
        initGlassSurfaces()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBar.setLiftable(true)

        initTabList()
        initBar()
        initView()
    }

    fun initView() {
        binding.viewPager.offscreenPageLimit =
            with(tabList.size) {
                if (this < 1) ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
                else this
            }
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int) = getFragment(position)
            override fun getItemCount() = tabList.size
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabList[position]
        }.attach()
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                iOnTabSelected(tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                tabController?.onReturnTop(null)
                onTabReselectedExtra()
            }
        })
    }

    private fun initGlassSurfaces() {
        if (glassSurfacesInitialized) {
            refreshGlassSurfaces()
            return
        }
        glassSurfacesInitialized = true
        toolbarGlassSurface = binding.toolBar.parent as? LiquidGlassFrameLayout
        tabGlassSurface = (binding.tabLayout.parent as? View)?.parent as? LiquidGlassFrameLayout

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                refreshGlassSurfaces()
            }

            override fun onPageSelected(position: Int) {
                refreshGlassSurfaces()
            }
        }.also(binding.viewPager::registerOnPageChangeCallback)

        appBarOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, _ ->
            refreshGlassSurfaces()
        }.also(binding.appBar::addOnOffsetChangedListener)

        glassRootAttachStateChangeListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                registerGlassContentScrollListener(view)
                refreshGlassSurfaces()
                view.post {
                    if (_binding?.root === view) {
                        refreshGlassSurfaces()
                    }
                }
            }

            override fun onViewDetachedFromWindow(view: View) {
                unregisterGlassContentScrollListener()
                clearGlassBackdropSources()
            }
        }.also(binding.root::addOnAttachStateChangeListener)

        binding.root.also { root ->
            registerGlassContentScrollListener(root)
            root.post {
                if (_binding?.root === root) {
                    refreshGlassSurfaces()
                }
            }
        }
        refreshGlassSurfaces()
    }

    private fun registerGlassContentScrollListener(root: View) {
        val observer = root.viewTreeObserver
        if (glassScrollObserver === observer) return
        unregisterGlassContentScrollListener()
        if (observer.isAlive) {
            observer.addOnScrollChangedListener(glassContentScrollChangedListener)
            glassScrollObserver = observer
        }
    }

    private fun unregisterGlassContentScrollListener() {
        glassScrollObserver?.takeIf { it.isAlive }
            ?.removeOnScrollChangedListener(glassContentScrollChangedListener)
        glassScrollObserver = null
    }

    private fun refreshGlassSurfaces() {
        val currentBinding = _binding ?: return
        val toolbarSource = currentBinding.collapsingToolbar.run {
            (0 until childCount)
                .map(::getChildAt)
                .filter { child ->
                    child !== toolbarGlassSurface &&
                            child.visibility == View.VISIBLE &&
                            child.isAttachedToWindow
                }
                .let { candidates ->
                    candidates.firstOrNull { it !== currentBinding.view }
                        ?: candidates.firstOrNull()
                }
        }
        val tabSource = currentBinding.viewPager.takeIf {
            it.visibility == View.VISIBLE && it.isAttachedToWindow
        }

        if (toolbarBackdropSource !== toolbarSource) {
            toolbarBackdropSource = toolbarSource
            toolbarGlassSurface?.setBackdropSource(toolbarSource)
        }
        if (tabBackdropSource !== tabSource) {
            tabBackdropSource = tabSource
            tabGlassSurface?.setBackdropSource(tabSource)
        }
        toolbarGlassSurface?.refreshBackdrop()
        tabGlassSurface?.refreshBackdrop()
    }

    private fun clearGlassBackdropSources() {
        toolbarBackdropSource = null
        tabBackdropSource = null
        toolbarGlassSurface?.setBackdropSource(null)
        tabGlassSurface?.setBackdropSource(null)
    }

    open fun onTabReselectedExtra() {}

    open fun iOnTabSelected(tab: TabLayout.Tab?) {}

    abstract fun getFragment(position: Int): Fragment

    abstract fun initTabList()

    open fun initBar() {
        binding.toolBar.apply {
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener {
                onBackClick()
            }
        }
    }

    abstract fun onBackClick()

    override fun onStart() {
        super.onStart()
        refreshGlassSurfaces()
    }

    override fun onDestroyView() {
        pageChangeCallback?.let(binding.viewPager::unregisterOnPageChangeCallback)
        appBarOffsetChangedListener?.let(binding.appBar::removeOnOffsetChangedListener)
        glassRootAttachStateChangeListener?.let(binding.root::removeOnAttachStateChangeListener)
        unregisterGlassContentScrollListener()
        clearGlassBackdropSources()
        glassSurfacesInitialized = false
        pageChangeCallback = null
        appBarOffsetChangedListener = null
        glassRootAttachStateChangeListener = null
        toolbarGlassSurface = null
        tabGlassSurface = null
        super.onDestroyView()
        _binding = null
    }

}
