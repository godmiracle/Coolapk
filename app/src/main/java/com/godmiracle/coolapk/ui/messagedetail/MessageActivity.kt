package com.godmiracle.coolapk.ui.messagedetail

import android.annotation.SuppressLint
import android.os.Bundle
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseFragmentContainerBinding
import com.godmiracle.coolapk.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessageActivity : BaseActivity<BaseFragmentContainerBinding>() {

    private val type by lazy { intent.getStringExtra("type").orEmpty() }

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
                    R.id.fragmentContainer, MessageFragment.newInstance(type)
                )
                .commit()
        }
    }

}