package com.godmiracle.coolapk.ui.hometopic

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.godmiracle.coolapk.adapter.PlaceHolderAdapter
import com.godmiracle.coolapk.ui.base.BaseAppFragment
import com.godmiracle.coolapk.util.setSpaceFooterView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeTopicContentFragment : BaseAppFragment<HomeTopicContentViewModel>() {

    @Inject
    lateinit var viewModelAssistedFactory: HomeTopicContentViewModel.Factory
    override val viewModel by viewModels<HomeTopicContentViewModel> {
        HomeTopicContentViewModel.provideFactory(
            viewModelAssistedFactory,
            arguments?.getString("url").orEmpty(),
            arguments?.getString("title").orEmpty(),
        )
    }
    private val placeHolderAdapter by lazy { PlaceHolderAdapter() }

    companion object {
        @JvmStatic
        fun newInstance(url: String, title: String) = HomeTopicContentFragment().apply {
            arguments = Bundle().apply {
                putString("url", url)
                putString("title", title)
            }
        }
    }

    override fun initView() {
        super.initView()

        binding.vfContainer.setOnDisplayedChildChangedListener {
            binding.recyclerView.setSpaceFooterView(placeHolderAdapter)
        }
    }

}
