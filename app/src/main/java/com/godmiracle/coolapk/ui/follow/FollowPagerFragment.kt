package com.godmiracle.coolapk.ui.follow

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.godmiracle.coolapk.ui.base.BasePagerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowPagerFragment : BasePagerFragment() {

    private val uid by lazy { arguments?.getString("uid").orEmpty() }
    private val type by lazy { arguments?.getString("type").orEmpty() }
    private val embedded by lazy { arguments?.getBoolean("embedded") == true }

    companion object {
        @JvmStatic
        fun newInstance(uid: String, type: String, embedded: Boolean = false) =
            FollowPagerFragment().apply {
                arguments = Bundle().apply {
                    putString("uid", uid)
                    putString("type", type)
                    putBoolean("embedded", embedded)
                }
            }
    }

    override fun getFragment(position: Int): Fragment =
        when (type) {
            "follow" ->
                if (embedded)
                    LocalFollowFragment()
                else FollowFragment.newInstance(uid, "follow")

            else -> FollowFragment.newInstance(uid, type)
        }

    override fun initTabList() {
        tabList =
            when (type) {
                "follow" ->
                    if (embedded) {
                        binding.tabLayout.isVisible = false
                        listOf("")
                    }
                    else {
                        binding.tabLayout.isVisible = false
                        listOf("")
                    }
                else -> {
                    binding.tabLayout.isVisible = false
                    listOf("")
                }
            }
    }

    override fun onBackClick() {
        if (!embedded)
            activity?.finish()
    }

    override fun initBar() {
        super.initBar()
        if (embedded) {
            binding.toolBar.navigationIcon = null
            binding.toolBar.setNavigationOnClickListener(null)
        }
        binding.collapsingToolbar.isTitleEnabled = false
        binding.toolBar.title = when (type) {
            "feed" -> "我的动态"

            "follow" -> {
                if (embedded)
                    "关注"
                else
                    "TA关注的人"
            }

            "fans" -> {
                "TA的粉丝"
            }

            else -> type

        }
    }

}
