package com.godmiracle.coolapk.ui.feed

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.adapter.FooterAdapter
import com.godmiracle.coolapk.adapter.FooterState
import com.godmiracle.coolapk.adapter.HeaderAdapter
import com.godmiracle.coolapk.adapter.ItemListener
import com.godmiracle.coolapk.databinding.FragmentFeedBinding
import com.godmiracle.coolapk.logic.model.FeedEntity
import com.godmiracle.coolapk.logic.model.TotalReplyResponse
import com.godmiracle.coolapk.ui.base.BaseFragment
import com.godmiracle.coolapk.ui.feed.reply.reply2reply.Reply2ReplyBottomSheetDialog
import com.godmiracle.coolapk.ui.feed.reply.reply2reply.ReplyRefreshListener
import com.godmiracle.coolapk.ui.others.CopyActivity
import com.godmiracle.coolapk.ui.others.WebViewActivity
import com.godmiracle.coolapk.util.ClipboardUtil
import com.godmiracle.coolapk.util.IntentUtil
import com.godmiracle.coolapk.util.PrefManager
import com.godmiracle.coolapk.util.dp
import com.godmiracle.coolapk.util.makeToast
import com.godmiracle.coolapk.view.LiquidGlassFrameLayout
import com.godmiracle.coolapk.view.StaggerItemDecoration
import com.godmiracle.coolapk.view.StickyItemDecorator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs


@AndroidEntryPoint
class FeedFragment : BaseFragment<FragmentFeedBinding>() {

    private val viewModel by viewModels<FeedViewModel>(ownerProducer = { requireActivity() })
    private val isPortrait by lazy { resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
    private lateinit var feedDataAdapter: FeedDataAdapter
    private lateinit var feedReplyAdapter: FeedReplyAdapter
    private lateinit var feedFixAdapter: FeedFixAdapter
    private lateinit var footerAdapter: FooterAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var sLayoutManager: StaggeredGridLayoutManager
    private var dialog: AlertDialog? = null
    private var toolbarGlassSurface: LiquidGlassFrameLayout? = null
    private var toolbarBackdropSource: View? = null
    private var appBarOffsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null
    private var rootAttachStateChangeListener: View.OnAttachStateChangeListener? = null
    private var feedScrollListener: RecyclerView.OnScrollListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initGlassSurfaces()

        binding.tabLayout.post {
            initView(binding.swipeRefresh.height - binding.tabLayout.height)
            initToolBar()
            initData()
            initRefresh()
            initScroll()
            initObserve()
        }

    }

    private fun initRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorPrimary,
                    0
                )
            )
            setOnRefreshListener {
                if (!viewModel.isLoadMore) {
                    binding.swipeRefresh.isRefreshing = true
                    refreshData()
                }
            }
        }
    }

    private fun initScroll() {
        feedScrollListener?.let(binding.recyclerView::removeOnScrollListener)
        feedScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    lastVisibleItemPosition =
                        if (isPortrait) mLayoutManager.findLastVisibleItemPosition()
                        else sLayoutManager.findLastVisibleItemPositions(null).max()

                    if (lastVisibleItemPosition + 1 == binding.recyclerView.adapter?.itemCount
                        && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                        && !binding.swipeRefresh.isRefreshing
                    ) {
                        loadMore()
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                refreshGlassSurfaces()
                firstVisibleItemPosition =
                    if (isPortrait) mLayoutManager.findFirstVisibleItemPosition()
                    else sLayoutManager.findFirstVisibleItemPositions(null).min()
                val shouldShow =
                    if (firstVisibleItemPosition in (0..1)) scrollYDistance >= 40.dp
                    else true
                binding.toolBar.title = if (shouldShow) null else viewModel.feedTypeName
                if (shouldShow && !binding.titleProfile.isVisible) {
                    binding.titleProfile.alpha = 0f
                    binding.titleProfile.animate().alpha(1f).setDuration(500)
                }
                binding.titleProfile.isVisible = shouldShow
            }
        }.also(binding.recyclerView::addOnScrollListener)
    }

    private fun initGlassSurfaces() {
        val root = binding.root
        toolbarGlassSurface = binding.toolbarGlassSurface
        appBarOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, _ ->
            refreshGlassSurfaces()
        }.also(binding.appBar::addOnOffsetChangedListener)
        rootAttachStateChangeListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                bindGlassBackdropSources()
                refreshGlassSurfaces()
                view.post {
                    if (this@FeedFragment.view === view) {
                        refreshGlassSurfaces()
                    }
                }
            }

            override fun onViewDetachedFromWindow(view: View) {
                clearGlassBackdropSources()
            }
        }.also(root::addOnAttachStateChangeListener)

        bindGlassBackdropSources()
        root.post {
            if (this@FeedFragment.view === root) {
                refreshGlassSurfaces()
            }
        }
        refreshGlassSurfaces()
    }

    private fun bindGlassBackdropSources() {
        val toolbarSource = binding.contentLayout
        if (toolbarBackdropSource !== toolbarSource) {
            toolbarBackdropSource = toolbarSource
            toolbarGlassSurface?.setBackdropSource(toolbarSource)
        }
    }

    private fun refreshGlassSurfaces() {
        toolbarGlassSurface?.refreshBackdrop()
    }

    private fun clearGlassBackdropSources() {
        toolbarBackdropSource = null
        toolbarGlassSurface?.setBackdropSource(null)
    }

    private fun setStickySortVisible(show: Boolean) {
        binding.tabLayout.isVisible = show
    }

    private fun loadMore() {
        viewModel.isLoadMore = true
        viewModel.fetchFeedReply()
    }

    private fun initObserve() {
        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.footerState.observe(viewLifecycleOwner) {
            footerAdapter.setLoadState(it)
            if (it !is FooterState.Loading) {
                binding.swipeRefresh.isRefreshing = false
                binding.indicator.parent.isIndeterminate = false
                binding.indicator.parent.isVisible = false
            }
            if (dialog != null) {
                dialog?.dismiss()
                dialog = null
            }
        }

        viewModel.feedReplyData.observe(viewLifecycleOwner) {
            viewModel.listSize = it.size
            feedReplyAdapter.submitList(it)
            if (viewModel.isViewReply) {
                viewModel.isViewReply = false
                if (firstVisibleItemPosition > viewModel.itemCount && ::mLayoutManager.isInitialized)
                    mLayoutManager.scrollToPositionWithOffset(viewModel.itemCount, 0)
            }
        }

    }

    private fun scrollToPosition(position: Int) {
        binding.recyclerView.scrollToPosition(position)
    }

    private fun initData() {
        if (viewModel.isInit) {
            viewModel.isInit = false
            refreshData()
        }
    }

    private fun refreshData() {
        firstVisibleItemPosition = 0
        lastVisibleItemPosition = 0
        viewModel.firstItem = null
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.fetchFeedReply()
    }

    @SuppressLint("SetTextI18n")
    private fun initView(height: Int) {
        feedDataAdapter = FeedDataAdapter(
            ItemClickListener(),
            viewModel.feedDataList,
            viewModel.articleList
        )
        feedReplyAdapter = FeedReplyAdapter(ItemClickListener())
        feedFixAdapter =
            FeedFixAdapter(viewModel.replyCount.toString(), RefreshReplyListener())

        binding.apply {
            refreshListener = RefreshReplyListener()
            listener = ItemClickListener()
            username = viewModel.funame
            avatarUrl = viewModel.avatar
            dateline = viewModel.dateLine
            deviceTitle = viewModel.device
        }
        footerAdapter = FooterAdapter(ReloadListener(), height)

        binding.replyCount.text = "共 ${viewModel.replyCount} 回复"
        setListType()

        binding.recyclerView.apply {
            adapter =
                ConcatAdapter(
                    HeaderAdapter(),
                    feedDataAdapter,
                    feedFixAdapter,
                    feedReplyAdapter,
                    footerAdapter
                )
            layoutManager =
                if (isPortrait) {
                    mLayoutManager = LinearLayoutManager(requireContext())
                    mLayoutManager
                } else {
                    setStickySortVisible(false)
                    sLayoutManager =
                        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    sLayoutManager
                }
            if (viewModel.isViewReply) {
                viewModel.isViewReply = false
                if (isPortrait) {
                    footerAdapter.setLoadState(FooterState.LoadingReply)
                    scrollToPosition(viewModel.itemCount)
                } else {
                    footerAdapter.setLoadState(FooterState.Loading)
                }
            } else {
                footerAdapter.setLoadState(FooterState.Loading)
            }
            if (itemDecorationCount == 0)
                if (isPortrait)
                    addItemDecoration(
                        StickyItemDecorator(requireContext(), 1, viewModel.itemCount,
                            object : StickyItemDecorator.SortShowListener {
                                override fun showSort(show: Boolean) {
                                    setStickySortVisible(show)
                                }
                            })
                    )
                else
                    addItemDecoration(StaggerItemDecoration(10.dp))
        }
    }

    private fun initToolBar() {
        binding.toolBar.apply {
            title = viewModel.feedTypeName
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener {
                activity?.finish()
            }
            setOnClickListener {
                binding.recyclerView.stopScroll()
                scrollToPosition(0)
            }
            inflateMenu(R.menu.feed_menu)

            menu.findItem(R.id.showReply).isVisible = isPortrait
            menu.findItem(R.id.showQuestion).isVisible = viewModel.feedType == "answer"

            val favorite = menu.findItem(R.id.favorite)
            lifecycleScope.launch(Dispatchers.Main) {
                val isFavorite = viewModel.isFavorite(viewModel.id)
                favorite.title = if (isFavorite) "取消收藏"
                else "收藏"
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.showQuestion -> {
                        viewModel.feedDataList?.getOrNull(0)?.fid?.let {
                            IntentUtil.startActivity<FeedActivity>(requireContext()) {
                                putExtra("id", it)
                            }
                        }
                    }

                    R.id.showReply -> {
                        binding.recyclerView.stopScroll()
                        if (firstVisibleItemPosition <= viewModel.itemCount - 1)
                            mLayoutManager.scrollToPositionWithOffset(viewModel.itemCount, 0)
                        else scrollToPosition(0)
                    }

                    R.id.block -> {
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("确定将 ${viewModel.funame} 加入黑名单？")
                            setNegativeButton(android.R.string.cancel, null)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.saveUid(viewModel.feedUid.toString())
                            }
                            show()
                        }
                    }

                    R.id.share -> {
                        IntentUtil.shareText(
                            requireContext(),
                            "https://www.coolapk1s.com/feed/${viewModel.id}"
                        )
                    }

                    R.id.copyLink -> {
                        ClipboardUtil.copyText(
                            requireContext(),
                            "https://www.coolapk1s.com/feed/${viewModel.id}"
                        )
                    }

                    R.id.favorite -> {
                        lifecycleScope.launch(Dispatchers.Main) {
                            val isFavorite = favorite.title == "取消收藏"
                            if (isFavorite) {
                                viewModel.delete(viewModel.id)
                                favorite.title = "收藏"
                                requireContext().makeToast("已取消收藏")
                            } else {
                                try {
                                    val fav = FeedEntity(
                                        viewModel.id,
                                        viewModel.feedUid.toString(),
                                        viewModel.funame.toString(),
                                        viewModel.avatar.toString(),
                                        viewModel.device.toString(),
                                        if (!viewModel.articleList.isNullOrEmpty())
                                            viewModel.articleMsg.toString()
                                        else {
                                            with(viewModel.feedDataList?.getOrNull(0)?.message.toString()) {
                                                if (this.length > 150) this.substring(0, 150)
                                                else this
                                            }
                                        }, // 还未加载完会空指针
                                        if (!viewModel.articleList.isNullOrEmpty()) viewModel.articleDateLine.toString()
                                        else viewModel.feedDataList?.getOrNull(0)?.dateline.toString()
                                    )
                                    viewModel.insert(fav)
                                    favorite.title = "取消收藏"
                                    requireContext().makeToast("已收藏")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    requireContext().makeToast("请稍后再试")
                                }
                            }
                        }
                    }

                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    inner class ReloadListener : FooterAdapter.FooterListener {
        override fun onReLoad() {
            viewModel.isEnd = false
            loadMore()
        }
    }

    inner class RefreshReplyListener : ReplyRefreshListener {
        @SuppressLint("InflateParams")
        override fun onRefreshReply(listType: String) {
            viewModel.listType = listType
            setListType()
            viewModel.firstItem = null
            viewModel.lastItem = null
            binding.recyclerView.stopScroll()
            if (firstVisibleItemPosition > 1)
                viewModel.isViewReply = true
            viewModel.fromFeedAuthor = if (listType == "") 1
            else 0
            viewModel.page = 1
            viewModel.isEnd = false
            viewModel.isRefreshing = true
            viewModel.isLoadMore = false
            dialog = MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_MaterialAlertDialog_Rounded
            ).apply {
                setView(
                    LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_refresh, null, false)
                )
                setCancelable(false)
            }.create()
            dialog?.show()
            val decorView: View? = dialog?.window?.decorView
            val paddingTop: Int = decorView?.paddingTop ?: 0
            val paddingBottom: Int = decorView?.paddingBottom ?: 0
            val paddingLeft: Int = decorView?.paddingLeft ?: 0
            val paddingRight: Int = decorView?.paddingRight ?: 0
            val width = 80.dp + paddingLeft + paddingRight
            val height = 80.dp + paddingTop + paddingBottom
            dialog?.window?.setLayout(width, height)
            viewModel.fetchFeedReply()
        }
    }

    private fun setListType() {
        when (viewModel.listType) {
            "lastupdate_desc" -> binding.buttonToggle.check(R.id.lastUpdate)
            "dateline_desc" -> binding.buttonToggle.check(R.id.dateLine)
            "popular" -> binding.buttonToggle.check(R.id.popular)
            "" -> binding.buttonToggle.check(R.id.author)
        }
        feedFixAdapter.setListType(viewModel.listType)
    }

    inner class ItemClickListener : ItemListener {
        override fun onViewFeed(
            view: View,
            id: String?,
            uid: String?,
            username: String?,
            userAvatar: String?,
            deviceTitle: String?,
            message: String?,
            dateline: String?,
            rid: Any?,
            isViewReply: Any?
        ) {
            super.onViewFeed(
                view,
                id,
                uid,
                username,
                userAvatar,
                deviceTitle,
                message,
                dateline,
                rid,
                isViewReply
            )
            if (!uid.isNullOrEmpty() && PrefManager.isRecordHistory)
                viewModel.saveHistory(
                    id.toString(), uid.toString(), username.toString(), userAvatar.toString(),
                    deviceTitle.toString(), message.toString(), dateline.toString()
                )
        }

        override fun onExpand(
            view: View,
            id: String,
            uid: String,
            text: String?,
            position: Int,
            rPosition: Int?
        ) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.feed_reply_menu, menu)
                setOnMenuItemClickListener(
                    PopClickListener(
                        id,
                        uid,
                        text,
                        position,
                        rPosition
                    )
                )
                show()
            }
        }

        override fun showTotalReply(
            id: String,
            uid: String,
            position: Int,
            rPosition: Int?,
            intercept: Boolean
        ) {
            val mBottomSheetDialogFragment =
                Reply2ReplyBottomSheetDialog.newInstance(
                    viewModel.feedUid.toString(),
                    uid,
                    id
                )
            val feedReplyList = viewModel.feedReplyData.value ?: emptyList()
            if (rPosition == null || rPosition == -1)
                mBottomSheetDialogFragment.oriReply.add(feedReplyList[position])
            else
                feedReplyList[position].replyRows?.getOrNull(rPosition)?.let {
                    mBottomSheetDialogFragment.oriReply.add(it.copy(
                        message = with(it.message) {
                            val start = indexOfFirst { char -> char == ':' }
                            val end = indexOf("<a class=\\\"feed-forward-pic\\\"")
                            if (end != -1)
                                substring(start + 2, end - 1)
                            else
                                substring(start + 2)
                        }
                    ))
                }

            mBottomSheetDialogFragment.show(childFragmentManager, "Dialog")
        }

    }

    inner class PopClickListener(
        private val id: String,
        private val uid: String,
        private val text: String?,
        private val position: Int,
        private val rPosition: Int?
    ) :
        PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.block -> {
                    viewModel.saveUid(uid)
                    val newList: List<TotalReplyResponse.Data> =
                        if (rPosition == null || rPosition == -1) {
                            viewModel.feedReplyData.value?.toMutableList().also {
                                it?.removeAt(position)
                            } ?: emptyList()
                        } else {
                            viewModel.feedReplyData.value?.mapIndexed { index, reply ->
                                if (index == position) {
                                    reply.copy(
                                        lastupdate = System.currentTimeMillis(),
                                        replyRows = reply.replyRows.also {
                                            it?.removeAt(rPosition)
                                        }
                                    )
                                } else reply
                            } ?: emptyList()
                        }
                    viewModel.feedReplyData.value = newList
                }

                R.id.copy -> {
                    IntentUtil.startActivity<CopyActivity>(requireContext()) {
                        putExtra("text", text)
                    }
                }

                R.id.show -> {
                    ItemClickListener().showTotalReply(
                        id,
                        uid,
                        position,
                        rPosition
                    )
                }
            }
            return true
        }
    }

    private val scrollYDistance: Int
        get() {
            val firstVisibleChildView =
                if (isPortrait) mLayoutManager.findViewByPosition(firstVisibleItemPosition)
                else sLayoutManager.findViewByPosition(firstVisibleItemPosition)
            return abs(firstVisibleChildView?.top ?: 0)
        }

    override fun onDestroyView() {
        feedScrollListener?.let(binding.recyclerView::removeOnScrollListener)
        appBarOffsetChangedListener?.let(binding.appBar::removeOnOffsetChangedListener)
        rootAttachStateChangeListener?.let(binding.root::removeOnAttachStateChangeListener)
        clearGlassBackdropSources()
        feedScrollListener = null
        appBarOffsetChangedListener = null
        rootAttachStateChangeListener = null
        toolbarGlassSurface = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

}
