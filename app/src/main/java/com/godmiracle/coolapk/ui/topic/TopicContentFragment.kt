package com.godmiracle.coolapk.ui.topic

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godmiracle.coolapk.ui.base.BaseAppFragment
import com.godmiracle.coolapk.ui.search.IOnSearchMenuClickContainer
import com.godmiracle.coolapk.ui.search.IOnSearchMenuClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TopicContentFragment : BaseAppFragment<TopicContentViewModel>(),
    IOnSearchMenuClickListener {

    @Inject
    lateinit var viewModelAssistedFactory: TopicContentViewModel.Factory
    override val viewModel by viewModels<TopicContentViewModel> {
        TopicContentViewModel.provideFactory(
            viewModelAssistedFactory,
            arguments?.getString("url").orEmpty(),
            arguments?.getString("title").orEmpty(),
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(url: String, title: String) =
            TopicContentFragment().apply {
                arguments = Bundle().apply {
                    putString("url", url)
                    putString("title", title)
                }
            }
    }

    override fun initObserve() {
        super.initObserve()

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSearch(type: String, value: String, id: String?) {
        TopicSort.fromLabel(value)?.let(viewModel::applySort)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.title == "讨论")
            (parentFragment as? IOnSearchMenuClickContainer)?.controller = this
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.title == "讨论")
            (parentFragment as? IOnSearchMenuClickContainer)?.controller = null
    }

}
