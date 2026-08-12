package com.example.c001apk.ui.follow

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.c001apk.R
import com.example.c001apk.databinding.FragmentLocalFollowBinding
import com.example.c001apk.logic.model.LocalFollow
import com.example.c001apk.logic.model.LocalFollowType
import com.example.c001apk.logic.repository.LocalFollowRepo
import com.example.c001apk.ui.topic.TopicActivity
import com.example.c001apk.ui.main.MainActivity
import com.example.c001apk.util.IntentUtil
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
                override fun onScrolled(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(recyclerView, dx, dy)
                    (activity as? MainActivity)?.let {
                        if (dy > 0) it.hideNavigationView()
                        else if (dy < 0) it.showNavigationView()
                    }
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
