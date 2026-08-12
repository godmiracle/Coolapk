package com.godmiracle.coolapk.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseFragmentContainerBinding
import com.godmiracle.coolapk.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : BaseActivity<BaseFragmentContainerBinding>() {

    private val pageType by lazy { intent.getStringExtra("pageType").orEmpty() }
    private val pageParam by lazy { intent.getStringExtra("pageParam").orEmpty() }
    private val title by lazy { intent.getStringExtra("title").orEmpty() }

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(
                    R.id.fragmentContainer,
                    SearchFragment.newInstance(pageType, pageParam, title)
                )
                commit()
            }
        }
    }
}
