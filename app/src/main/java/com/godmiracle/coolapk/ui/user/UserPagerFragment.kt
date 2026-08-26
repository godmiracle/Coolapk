package com.godmiracle.coolapk.ui.user

import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseUserPageBinding
import com.godmiracle.coolapk.databinding.BaseViewUserBinding
import com.godmiracle.coolapk.ui.base.BaseFragment
import com.godmiracle.coolapk.ui.search.SearchActivity
import com.godmiracle.coolapk.util.DateUtils
import com.godmiracle.coolapk.util.IntentUtil
import com.godmiracle.coolapk.util.ReplaceViewHelper
import com.godmiracle.coolapk.view.LiquidGlassFrameLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserPagerFragment : BaseFragment<BaseUserPageBinding>() {

    private val viewModel by viewModels<UserViewModel>(ownerProducer = { requireActivity() })
    private lateinit var userBinding: BaseViewUserBinding
    private var menuBlock: MenuItem? = null
    private var toolbarGlassSurface: LiquidGlassFrameLayout? = null
    private var toolbarBackdropSource: View? = null
    private var appBarOffsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null
    private var rootAttachStateChangeListener: View.OnAttachStateChangeListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBar()
        initPage()
        initUser()
        initGlassSurface()
        initObserve()
    }

    private fun initPage() {
        if (childFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, UserFragment())
                .commit()
        }
    }

    private fun initObserve() {
        viewModel.blockState.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                menuBlock?.title = getMenuTitle(
                    if (it) "移除黑名单"
                    else "加入黑名单"
                )

            }
        }

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initUser() {
        val replaceViewHelper = ReplaceViewHelper(requireContext())
        userBinding = BaseViewUserBinding.inflate(layoutInflater, null, false)
        replaceViewHelper.toReplaceView(binding.view, userBinding.root)
        userBinding.userData = viewModel.userData
        userBinding.listener = viewModel.ItemClickListener()
    }

    private fun initGlassSurface() {
        toolbarGlassSurface = binding.toolBar.parent as? LiquidGlassFrameLayout
        appBarOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, _ ->
            refreshGlassSurface()
        }.also(binding.appBar::addOnOffsetChangedListener)
        rootAttachStateChangeListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                refreshGlassSurface()
                view.post {
                    if (this@UserPagerFragment.view === view) {
                        refreshGlassSurface()
                    }
                }
            }

            override fun onViewDetachedFromWindow(view: View) {
                clearGlassSurface()
            }
        }.also(binding.root::addOnAttachStateChangeListener)

        val root = binding.root
        root.post {
            if (view === root) {
                refreshGlassSurface()
            }
        }
        refreshGlassSurface()
    }

    private fun refreshGlassSurface() {
        val surface = toolbarGlassSurface ?: return
        val source = binding.collapsingToolbar.run {
            (0 until childCount)
                .map(::getChildAt)
                .filter { child ->
                    child !== surface &&
                            child.visibility == View.VISIBLE &&
                            child.isAttachedToWindow
                }
                .let { candidates ->
                    candidates.firstOrNull { it !== binding.view }
                        ?: candidates.firstOrNull()
                }
        }
        if (toolbarBackdropSource !== source) {
            toolbarBackdropSource = source
            surface.setBackdropSource(source)
        }
        surface.refreshBackdrop()
    }

    private fun clearGlassSurface() {
        toolbarBackdropSource = null
        toolbarGlassSurface?.setBackdropSource(null)
    }

    private fun getMenuTitle(title: CharSequence?): SpannableString {
        return SpannableString(title).also {
            it.setSpan(
                ForegroundColorSpan(
                    MaterialColors.getColor(
                        requireContext(),
                        com.google.android.material.R.attr.colorControlNormal,
                        0
                    )
                ),
                0, title?.length ?: 0, 0
            )
        }
    }

    private fun initBar() {
        binding.collapsingToolbar.title = viewModel.userData?.username
        binding.toolBar.apply {
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener {
                activity?.finish()
            }

            inflateMenu(R.menu.user_menu)
            menuBlock = menu?.findItem(R.id.block)
            menuBlock?.title = getMenuTitle(menuBlock?.title)

            val menuShare = menu?.findItem(R.id.share)
            menuShare?.title = getMenuTitle(menuShare?.title)

            menu?.findItem(R.id.check)?.title = getMenuTitle(menu?.findItem(R.id.check)?.title)

            viewModel.checkMenuState()

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.check -> {
                        val data = viewModel.userData
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle(data?.username)
                            setMessage(
                                """
                                uid: ${data?.uid}
                                
                                等级: Lv.${data?.level}
                                
                                性别: ${if (data?.gender == 0) "女" else if (data?.gender == 1) "男" else "未知"}
                                
                                注册时长: ${((System.currentTimeMillis() / 1000 - (data?.regdate ?: 0)) / 24 / 3600)} 天
                                
                                注册时间: ${DateUtils.timeStamp2Date(data?.regdate ?: 0)}
                            """.trimIndent()
                            )
                            show()
                        }
                    }

                    R.id.search -> {
                        IntentUtil.startActivity<SearchActivity>(requireContext()) {
                            putExtra("pageType", "user")
                            putExtra("pageParam", viewModel.uid)
                            putExtra("title", viewModel.userData?.username)
                        }
                    }

                    R.id.block -> {
                        val isBlocked = menuBlock?.title.toString() == "移除黑名单"
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("确定将 ${viewModel.userData?.username} ${menuBlock?.title}？")
                            setNegativeButton(android.R.string.cancel, null)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.uid.let { uid ->
                                    menuBlock?.title = if (isBlocked) {
                                        viewModel.deleteUid(uid)
                                        getMenuTitle("加入黑名单")
                                    } else {
                                        viewModel.saveUid(uid)
                                        getMenuTitle("移除黑名单")
                                    }
                                }
                            }
                            show()
                        }
                    }

                    R.id.share -> {
                        IntentUtil.shareText(
                            requireContext(),
                            "https://www.coolapk1s.com/u/${viewModel.uid}"
                        )
                    }

                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshGlassSurface()
    }

    override fun onDestroyView() {
        appBarOffsetChangedListener?.let(binding.appBar::removeOnOffsetChangedListener)
        rootAttachStateChangeListener?.let(binding.root::removeOnAttachStateChangeListener)
        clearGlassSurface()
        appBarOffsetChangedListener = null
        rootAttachStateChangeListener = null
        toolbarGlassSurface = null
        super.onDestroyView()
    }
}
