package com.example.c001apk.ui.my

import android.os.Bundle
import android.view.View
import com.example.c001apk.R
import com.example.c001apk.databinding.FragmentMyBinding
import com.example.c001apk.ui.base.BaseFragment
import com.example.c001apk.ui.history.HistoryActivity
import com.example.c001apk.ui.settings.SettingsActivity
import com.example.c001apk.util.IntentUtil

class MyFragment : BaseFragment<FragmentMyBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
