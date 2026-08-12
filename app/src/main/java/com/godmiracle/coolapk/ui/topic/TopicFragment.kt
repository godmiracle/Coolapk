package com.godmiracle.coolapk.ui.topic

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.ui.base.BasePagerFragment
import com.godmiracle.coolapk.ui.feed.reply.ReplyActivity
import com.godmiracle.coolapk.ui.home.IOnTabClickListener
import com.godmiracle.coolapk.ui.search.IOnSearchMenuClickContainer
import com.godmiracle.coolapk.ui.search.IOnSearchMenuClickListener
import com.godmiracle.coolapk.ui.search.SearchActivity
import com.godmiracle.coolapk.util.IntentUtil
import com.godmiracle.coolapk.util.PrefManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.GRAVITY_CENTER
import com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopicFragment : BasePagerFragment(), IOnSearchMenuClickContainer {

    private val viewModel by viewModels<TopicViewModel>(ownerProducer = { requireActivity() })
    override var tabController: IOnTabClickListener? = null
    private lateinit var subscribe: MenuItem
    private lateinit var order: MenuItem
    private var menuBlock: MenuItem? = null
    override var controller: IOnSearchMenuClickListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSelected()
        if (PrefManager.isLogin)
            initFab()
        initObserve()
    }

    override fun initFab() {
        super.initFab()
        fab.setOnClickListener {
            val intent = Intent(requireContext(), ReplyActivity::class.java)
            intent.putExtra("type", "createFeed")
            intent.putExtra(
                "targetType",
                if (viewModel.type == "topic") "tag" else "product_phone"
            )
            intent.putExtra("targetId", viewModel.id)
            if (viewModel.type == "topic")
                intent.putExtra("title", viewModel.title)
            val animationBundle = ActivityOptions.makeCustomAnimation(
                context,
                R.anim.anim_bottom_sheet_slide_up,
                R.anim.anim_bottom_sheet_slide_down
            ).toBundle()
            requireContext().startActivity(intent, animationBundle)
        }
    }

    override fun onTabReselectedExtra() {
        if (fabBehavior.isScrolledDown)
            fabBehavior.slideUp(fab, true)
    }

    private fun initSelected() {
        viewModel.tabSelected?.let {
            binding.viewPager.setCurrentItem(it, false)
            viewModel.tabSelected = null
        }
    }

    private fun initObserve() {
        viewModel.blockState.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                menuBlock?.title = if (it) "移除黑名单"
                else "加入黑名单"
            }
        }

        viewModel.followState.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                subscribe.title = if (it) "取消关注"
                else "关注"
            }
        }

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun iOnTabSelected(tab: TabLayout.Tab?) {
        order.isVisible = tab?.position == tabList.indexOf("讨论")
    }

    override fun getFragment(position: Int): Fragment =
        TopicContentFragment.newInstance(
            viewModel.topicList?.getOrNull(position)?.url.orEmpty(),
            viewModel.topicList?.getOrNull(position)?.title.orEmpty(),
        )

    override fun initTabList() {
        binding.tabLayout.apply {
            tabGravity = GRAVITY_CENTER
            tabMode = MODE_SCROLLABLE
        }
        tabList = viewModel.topicList?.map { it.title } ?: emptyList()
    }

    override fun onBackClick() {
        activity?.finish()
    }

    override fun initBar() {
        super.initBar()
        binding.collapsingToolbar.isTitleEnabled = false
        binding.toolBar.apply {
            title = if (viewModel.type == "topic") viewModel.url.replace("/t/", "")
            else viewModel.title
            viewModel.subtitle?.let { subtitle = it }

            inflateMenu(R.menu.topic_product_menu)

            order = menu.findItem(R.id.order)
            order.isVisible = viewModel.type == "product"
                    && binding.viewPager.currentItem == tabList.indexOf("讨论")
            menu.findItem(
                when (viewModel.productTitle) {
                    "最近回复" -> R.id.topicLatestReply
                    "热度排序" -> R.id.topicHot
                    "最新发布" -> R.id.topicLatestPublish
                    else -> throw IllegalArgumentException("type error")
                }
            )?.isChecked = true

            menuBlock = menu.findItem(R.id.block)
            subscribe = menu.findItem(R.id.subscribe)
            subscribe.isVisible = viewModel.type == "topic" || viewModel.type == "product"

            viewModel.checkMenuState()

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.search -> {
                        if (viewModel.type == "topic") {
                            IntentUtil.startActivity<SearchActivity>(requireContext()) {
                                putExtra("type", "topic")
                                putExtra("pageType", "tag")
                                putExtra("pageParam", viewModel.url.replace("/t/", ""))
                                putExtra("title", viewModel.url.replace("/t/", ""))
                            }
                        } else {
                            IntentUtil.startActivity<SearchActivity>(requireContext()) {
                                putExtra("type", "topic")
                                putExtra("pageType", "product_phone")
                                putExtra("pageParam", viewModel.id)
                                putExtra("title", viewModel.title)
                            }
                        }
                    }

                    R.id.topicLatestReply -> {
                        viewModel.productTitle = "最近回复"
                        controller?.onSearch("title", "最近回复", viewModel.id)
                    }

                    R.id.topicHot -> {
                        viewModel.productTitle = "热度排序"
                        controller?.onSearch("title", "热度排序", viewModel.id)
                    }

                    R.id.topicLatestPublish -> {
                        viewModel.productTitle = "最新发布"
                        controller?.onSearch("title", "最新发布", viewModel.id)
                    }

                    R.id.block -> {
                        val isBlocked = menuBlock?.title.toString() == "移除黑名单"
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            val title =
                                if (viewModel.type == "topic") viewModel.url
                                    .replace("/t/", "")
                                else viewModel.title
                            setTitle("确定将 $title ${menuBlock?.title}？")
                            setNegativeButton(android.R.string.cancel, null)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.title.let { title ->
                                    menuBlock?.title = if (isBlocked) {
                                        viewModel.deleteTopic(title)
                                        "加入黑名单"
                                    } else {
                                        viewModel.saveTopic(title)
                                        "移除黑名单"
                                    }
                                }
                            }
                            show()
                        }
                    }

                    R.id.subscribe -> {
                        viewModel.toggleFollow()
                    }

                }
                menu.findItem(
                    when (viewModel.productTitle) {
                        "最近回复" -> R.id.topicLatestReply
                        "热度排序" -> R.id.topicHot
                        "最新发布" -> R.id.topicLatestPublish
                        else -> throw IllegalArgumentException("type error")
                    }
                )?.isChecked = true
                return@setOnMenuItemClickListener true
            }
        }
    }
}
