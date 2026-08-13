package com.godmiracle.coolapk.ui.discover

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.godmiracle.coolapk.databinding.FragmentDiscoverBinding
import com.godmiracle.coolapk.ui.base.BaseFragment
import com.godmiracle.coolapk.ui.home.IOnTabClickContainer
import com.godmiracle.coolapk.ui.home.IOnTabClickListener
import com.godmiracle.coolapk.ui.homefeed.HomeFeedFragment
import com.godmiracle.coolapk.ui.main.MainActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiscoverFragment : BaseFragment<FragmentDiscoverBinding>(), IOnTabClickContainer {

    private val tabList = listOf("生活", "酷图")
    override var tabController: IOnTabClickListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topGlassSurface.apply {
            setBackdropSource(binding.viewPager)
            setBlurRadiusDp(14f)
            setSurfaceAlpha(0.24f)
            setCornerRadiusDp(26f)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: Tab) {}

            override fun onTabUnselected(tab: Tab?) {}

            override fun onTabReselected(tab: Tab?) {
                tabController?.onReturnTop(true)
                (activity as? MainActivity)?.showNavigationView()
            }
        })

        binding.viewPager.apply {
            offscreenPageLimit = tabList.size
            adapter = object : FragmentStateAdapter(this@DiscoverFragment) {
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> HomeFeedFragment.newInstance("life")
                        1 -> HomeFeedFragment.newInstance("coolPic")
                        else -> throw IllegalArgumentException()
                    }
                }

                override fun getItemCount() = tabList.size
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabList[position]
        }.attach()
    }
}
