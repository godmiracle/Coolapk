package com.godmiracle.coolapk.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.godmiracle.coolapk.databinding.FragmentHomeBinding
import com.godmiracle.coolapk.ui.base.BaseFragment
import com.godmiracle.coolapk.ui.homefeed.HomeFeedFragment
import com.godmiracle.coolapk.ui.hometopic.HomeTopicFragment
import com.godmiracle.coolapk.ui.main.MainActivity
import com.godmiracle.coolapk.ui.others.CopyActivity
import com.godmiracle.coolapk.util.IntentUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(), IOnTabClickContainer {

    private val viewModel by viewModels<HomeViewModel>()
    override var tabController: IOnTabClickListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topGlassSurface.apply {
            setBackdropSource(binding.viewPager)
            setBlurRadiusDp(14f)
            setSurfaceAlpha(0.24f)
            setCornerRadiusDp(26f)
        }

        initButton()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: Tab) {
                viewModel.position = tab.position
            }

            override fun onTabUnselected(tab: Tab?) {}

            override fun onTabReselected(tab: Tab?) {
                if (tab?.text == "关注")
                    tabController?.onReturnTop(false)
                else {
                    tabController?.onReturnTop(true)
                    (activity as? MainActivity)?.showNavigationView()
                }
            }
        })

        viewModel.tabListLiveData.observe(viewLifecycleOwner) { tabList ->
            if (tabList.isEmpty()) {
                viewModel.initTab()
            } else {
                val enableList = tabList
                    .filter { it.isEnable && it.title != HomeViewModel.APPLICATION_TAB_TITLE }
                    .map { it.title }
                if (enableList.isEmpty()) {
                    viewModel.updateTab(viewModel.defaultList)
                } else {
                    initView(enableList)
                }
            }

        }

    }

    private fun initButton() {
        binding.menu.setOnClickListener {
            IntentUtil.startActivity<CopyActivity>(requireContext()) {
                putExtra("type", "homeMenu")
            }
        }
    }

    private fun initView(enableList: List<String>) {
        binding.viewPager.offscreenPageLimit = enableList.size
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return when (enableList[position]) {
                    "关注" -> HomeFeedFragment.newInstance("follow")
                    "头条" -> HomeFeedFragment.newInstance("feed")
                    "热榜" -> HomeFeedFragment.newInstance("rank")
                    "话题" -> HomeTopicFragment.newInstance("topic")
                    "数码" -> HomeTopicFragment.newInstance("product")
                    "酷图" -> HomeFeedFragment.newInstance("coolPic")
                    else -> throw IllegalArgumentException()
                }
            }

            override fun getItemCount() = enableList.size

        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = enableList[position]
        }.attach()
        if (viewModel.isInit) {
            viewModel.isInit = false
            if (enableList.contains("头条"))
                binding.viewPager.setCurrentItem(enableList.indexOf("头条"), false)
        }
    }

}
