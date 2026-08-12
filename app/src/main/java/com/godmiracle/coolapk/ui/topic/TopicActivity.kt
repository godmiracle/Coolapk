package com.godmiracle.coolapk.ui.topic

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.adapter.LoadingState
import com.godmiracle.coolapk.constant.Constants.LOADING_EMPTY
import com.godmiracle.coolapk.logic.model.TopicBean
import com.godmiracle.coolapk.ui.base.BaseViewActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import java.net.URLDecoder

@AndroidEntryPoint
class TopicActivity : BaseViewActivity<TopicViewModel>() {

    override val viewModel by viewModels<TopicViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<TopicViewModel.Factory> { factory ->
                factory.create(
                    URLDecoder.decode(intent.getStringExtra("url").orEmpty(), "UTF-8"),
                    URLDecoder.decode(intent.getStringExtra("title").orEmpty(), "UTF-8"),
                    intent.getStringExtra("id").orEmpty(),
                    intent.getStringExtra("type").orEmpty(),
                )
            }
        }
    )

    override fun getSavedData(savedInstanceState: Bundle?) {
        if (viewModel.topicList == null) {
            viewModel.topicList = if (Build.VERSION.SDK_INT >= 33)
                savedInstanceState?.getParcelableArrayList("topicList", TopicBean::class.java)
            else
                savedInstanceState?.getParcelableArrayList("topicList")

        }
    }

    @SuppressLint("CommitTransaction")
    override fun beginTransaction() {
        if (!viewModel.topicList.isNullOrEmpty()) {
            if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, TopicFragment())
                    .commit()
            }
        } else {
            viewModel.loadingState.value = LoadingState.LoadingError(LOADING_EMPTY)
        }
    }

    override fun fetchData() {
        if (viewModel.type == "topic") {
            viewModel.url = viewModel.url.replace("/t/", "")
            viewModel.fetchTopicLayout()
        } else if (viewModel.type == "product") {
            viewModel.fetchProductLayout()
        }
    }

    override fun initData() {
        if (viewModel.isAInit) {
            viewModel.isAInit = false
            if (viewModel.topicList == null) {
                viewModel.activityState.value = LoadingState.Loading
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.topicList?.let {
            outState.putParcelableArrayList("topicList", it)
        }
        super.onSaveInstanceState(outState)
    }

}