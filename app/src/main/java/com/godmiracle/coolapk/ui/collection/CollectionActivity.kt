package com.godmiracle.coolapk.ui.collection

import android.annotation.SuppressLint
import android.os.Bundle
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseFragmentContainerBinding
import com.godmiracle.coolapk.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionActivity : BaseActivity<BaseFragmentContainerBinding>() {

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    CollectionFragment()
                )
                .commit()
        }
    }

}