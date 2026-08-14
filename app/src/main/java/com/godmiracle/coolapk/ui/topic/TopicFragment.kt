package com.godmiracle.coolapk.ui.topic

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseViewTopicBinding
import com.godmiracle.coolapk.databinding.TopicSortBarBinding
import com.godmiracle.coolapk.ui.base.BasePagerFragment
import com.godmiracle.coolapk.ui.feed.reply.ReplyActivity
import com.godmiracle.coolapk.ui.home.IOnTabClickListener
import com.godmiracle.coolapk.ui.search.IOnSearchMenuClickContainer
import com.godmiracle.coolapk.ui.search.IOnSearchMenuClickListener
import com.godmiracle.coolapk.ui.search.SearchActivity
import com.godmiracle.coolapk.util.ImageUtil
import com.godmiracle.coolapk.util.IntentUtil
import com.godmiracle.coolapk.util.PrefManager
import com.godmiracle.coolapk.util.ReplaceViewHelper
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.GRAVITY_CENTER
import com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopicFragment : BasePagerFragment(), IOnSearchMenuClickContainer {

    private val viewModel by viewModels<TopicViewModel>(ownerProducer = { requireActivity() })
    override var tabController: IOnTabClickListener? = null
    private lateinit var topicHeader: BaseViewTopicBinding
    private lateinit var sortBar: TopicSortBarBinding
    private var updatingSortSelection = false
    private var menuBlock: MenuItem? = null
    override var controller: IOnSearchMenuClickListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTopicHeader()
        initSortBar()
        initSelected()
        updateSortBarVisibility(binding.viewPager.currentItem)
        if (PrefManager.isLogin)
            initFab()
        initObserve()
    }

    private fun initTopicHeader() {
        topicHeader = BaseViewTopicBinding.inflate(layoutInflater, null, false)
        ReplaceViewHelper(requireContext()).toReplaceView(binding.view, topicHeader.root)
        topicHeader.followButton.setOnClickListener {
            viewModel.toggleFollow()
        }
        renderTopicHeader()
    }

    private fun renderTopicHeader() {
        topicHeader.type.text = if (viewModel.type == "topic") "话题" else "数码"
        topicHeader.title.text = topicTitle()

        val intro = viewModel.subtitle?.trim()?.takeIf { it.isNotEmpty() }
        topicHeader.intro.text = intro.orEmpty()
        topicHeader.intro.isVisible = intro != null

        val data = viewModel.topicData
        bindStat(topicHeader.hotNum, data?.hotNumTxt ?: data?.hotNum, "热度")
        bindStat(
            topicHeader.discussionNum,
            data?.feedCommentNumTxt ?: data?.commentnumTxt ?: data?.commentCount,
            "讨论"
        )
        bindStat(topicHeader.followNum, data?.followNum, "关注")

        Glide.with(topicHeader.logo).clear(topicHeader.logo)
        if (data?.logo.isNullOrBlank()) {
            topicHeader.logo.setImageResource(
                if (viewModel.type == "topic") R.drawable.outline_tag_24
                else R.drawable.ic_phone
            )
            topicHeader.logo.setColorFilter(
                MaterialColors.getColor(
                    topicHeader.logo,
                    com.google.android.material.R.attr.colorPrimary
                )
            )
        } else {
            topicHeader.logo.clearColorFilter()
            ImageUtil.showIMG(topicHeader.logo, data?.logo)
        }
        renderFollowButton(viewModel.isFollow)
    }

    private fun bindStat(view: TextView, value: String?, suffix: String) {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
        view.text = normalized?.let { "$it$suffix" }.orEmpty()
        view.isVisible = normalized != null
    }

    private fun renderFollowButton(followed: Boolean) {
        topicHeader.followButton.text = if (followed) "已关注" else "关注"
    }

    private fun topicTitle(): String {
        return if (viewModel.type == "topic")
            viewModel.url.replace("/t/", "").ifBlank { viewModel.title }
        else
            viewModel.title
    }

    private fun initSortBar() {
        sortBar = TopicSortBarBinding.inflate(layoutInflater, binding.extraBar, false)
        binding.extraBar.addView(sortBar.root)
        sortBar.sortGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || updatingSortSelection)
                return@addOnButtonCheckedListener

            val sort = sortForButton(checkedId)
            viewModel.discussionSort = sort
            controller?.onSearch("sort", sort.label, null)
        }
    }

    private fun updateSortBarVisibility(position: Int) {
        val visible = tabList.getOrNull(position) == "讨论"
        binding.extraBar.isVisible = visible
        if (visible) {
            updatingSortSelection = true
            sortBar.sortGroup.check(buttonForSort(viewModel.discussionSort))
            updatingSortSelection = false
        }
    }

    private fun sortForButton(buttonId: Int): TopicSort = when (buttonId) {
        R.id.topicSortLatest -> TopicSort.LATEST
        R.id.topicSortHot -> TopicSort.HOT
        else -> TopicSort.DEFAULT
    }

    private fun buttonForSort(sort: TopicSort): Int = when (sort) {
        TopicSort.DEFAULT -> R.id.topicSortDefault
        TopicSort.LATEST -> R.id.topicSortLatest
        TopicSort.HOT -> R.id.topicSortHot
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
                renderFollowButton(it)
            }
        }

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun iOnTabSelected(tab: TabLayout.Tab?) {
        updateSortBarVisibility(tab?.position ?: -1)
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
            title = ""

            inflateMenu(R.menu.topic_product_menu)
            menu.findItem(R.id.order).isVisible = false
            menu.findItem(R.id.subscribe).isVisible = false
            menuBlock = menu.findItem(R.id.block)

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

                    R.id.block -> {
                        val isBlocked = menuBlock?.title.toString() == "移除黑名单"
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("确定将 ${topicTitle()} ${menuBlock?.title}？")
                            setNegativeButton(android.R.string.cancel, null)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                menuBlock?.title = if (isBlocked) {
                                    viewModel.deleteTopic(viewModel.title)
                                    "加入黑名单"
                                } else {
                                    viewModel.saveTopic(viewModel.title)
                                    "移除黑名单"
                                }
                            }
                            show()
                        }
                    }
                }
                true
            }
        }
    }
}
