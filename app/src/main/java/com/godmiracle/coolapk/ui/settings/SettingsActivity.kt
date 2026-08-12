package com.godmiracle.coolapk.ui.settings

import android.os.Bundle
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.BaseFragmentContainerBinding
import com.godmiracle.coolapk.ui.base.BaseActivity

class SettingsActivity : BaseActivity<BaseFragmentContainerBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SettingsFragment())
                .commit()
        }
    }
}
