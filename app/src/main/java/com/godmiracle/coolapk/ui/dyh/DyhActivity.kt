package com.godmiracle.coolapk.ui.dyh

import android.annotation.SuppressLint
import android.os.Bundle
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseFragmentContainerBinding
import com.godmiracle.coolapk.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DyhActivity : BaseActivity<BaseFragmentContainerBinding>() {

    private val id by lazy { intent.getStringExtra("id").orEmpty() }
    private val title by lazy { intent.getStringExtra("title").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beginTransaction()
    }

    @SuppressLint("CommitTransaction")
    private fun beginTransaction() {
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer, DyhFragment.newInstance(id, title)
                )
                .commit()
        }
    }

}