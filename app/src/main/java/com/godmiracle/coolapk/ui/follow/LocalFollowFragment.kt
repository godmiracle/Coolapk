package com.godmiracle.coolapk.ui.follow

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.FragmentLocalFollowBinding
import com.godmiracle.coolapk.logic.model.LocalFollow
import com.godmiracle.coolapk.logic.model.LocalFollowType
import com.godmiracle.coolapk.logic.repository.LocalFollowRepo
import com.godmiracle.coolapk.ui.topic.TopicActivity
import com.godmiracle.coolapk.ui.main.MainActivity
import com.godmiracle.coolapk.util.IntentUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocalFollowFragment : Fragment(R.layout.fragment_local_follow) {

    @Inject
    lateinit var localFollowRepo: LocalFollowRepo

    private var _binding: FragmentLocalFollowBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LocalFollowAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalFollowBinding.bind(view)

        adapter = LocalFollowAdapter(::openFollow, ::removeFollow)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LocalFollowFragment.adapter
            setHasFixedSize(true)
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    newState: Int
                ) {
                    super.onScrollStateChanged(recyclerView, newState)
                    (activity as? MainActivity)?.onContentScrollStateChanged(newState)
                }
            })
        }

        localFollowRepo.observeAll().observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.emptyState.isVisible = it.isEmpty()
            binding.recyclerView.isVisible = it.isNotEmpty()
        }
    }

    private fun openFollow(item: LocalFollow) {
        IntentUtil.startActivity<TopicActivity>(requireContext()) {
            putExtra("type", item.type)
            putExtra("title", item.title)
            if (item.type == LocalFollowType.TOPIC) {
                putExtra("url", item.targetId)
                putExtra("id", "")
            } else {
                putExtra("url", "")
                putExtra("id", item.targetId)
            }
        }
    }

    private fun removeFollow(item: LocalFollow) {
        viewLifecycleOwner.lifecycleScope.launch {
            localFollowRepo.delete(item.type, item.targetId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
