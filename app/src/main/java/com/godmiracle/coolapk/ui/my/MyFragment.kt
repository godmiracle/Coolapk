package com.godmiracle.coolapk.ui.my

import android.os.Bundle
import android.view.View
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.FragmentMyBinding
import com.godmiracle.coolapk.ui.base.BaseFragment
import com.godmiracle.coolapk.ui.history.HistoryActivity
import com.godmiracle.coolapk.ui.main.MainActivity
import com.godmiracle.coolapk.ui.settings.SettingsActivity
import com.godmiracle.coolapk.util.IntentUtil

class MyFragment : BaseFragment<FragmentMyBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.applyFloatingNavigationContentInset(binding.content)

        binding.localFavorite.setOnClickListener {
            IntentUtil.startActivity<HistoryActivity>(requireContext()) {
                putExtra("type", "favorite")
            }
        }

        binding.browseHistory.setOnClickListener {
            IntentUtil.startActivity<HistoryActivity>(requireContext()) {
                putExtra("type", "browse")
            }
        }

        binding.settings.setOnClickListener {
            IntentUtil.startActivity<SettingsActivity>(requireContext()) {}
        }
    }
}
