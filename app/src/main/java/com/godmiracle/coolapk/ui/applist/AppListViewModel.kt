package com.godmiracle.coolapk.ui.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.godmiracle.coolapk.adapter.LoadingState
import com.godmiracle.coolapk.logic.model.AppItem
import com.godmiracle.coolapk.logic.model.UpdateCheckResponse
import com.godmiracle.coolapk.logic.repository.NetworkRepo
import com.godmiracle.coolapk.ui.base.BaseViewModel
import com.godmiracle.coolapk.util.PrefManager
import com.godmiracle.coolapk.util.Utils
import com.godmiracle.coolapk.util.Utils.getBase64
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import rikka.core.content.pm.longVersionCodeCompat
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val networkRepo: NetworkRepo
) : BaseViewModel() {

    val setFab: MutableLiveData<Boolean> = MutableLiveData()
    val items: MutableLiveData<List<AppItem>> = MutableLiveData()
    val appsUpdate = ArrayList<UpdateCheckResponse.Data>()

    override fun fetchData() {}

    private fun fetchAppsUpdate(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getAppsUpdate(pkg)
                .collect { result ->
                    result.getOrNull()?.let {
                        appsUpdate.clear()
                        appsUpdate.addAll(it)
                        setFab.postValue(true)
                    }
                }
        }

    }

    fun getItems(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appList = context.packageManager
                .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
            val newItems = ArrayList<AppItem>()
            val updateCheckJsonObject = JSONObject()

            appList.forEach { info ->
                if (((info.flags and ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM)) {
                    val packageInfo = context.packageManager.getPackageInfo(info.packageName, 0)

                    val appItem = AppItem().apply {
                        packageName = info.packageName
                        versionName =
                            "${packageInfo.versionName}(${packageInfo.longVersionCodeCompat})"
                        lastUpdateTime = packageInfo.lastUpdateTime
                    }

                    if (appItem.packageName != "com.godmiracle.coolapk")
                        newItems.add(appItem)

                    if (info.packageName != "com.godmiracle.coolapk")
                        updateCheckJsonObject.put(
                            info.packageName,
                            "0,${packageInfo.longVersionCodeCompat},${Utils.getInstalledAppMd5(info)}"
                        )
                }
            }

            isEnd = true
            items.postValue(newItems.sortedByDescending { it.lastUpdateTime })
            if (PrefManager.isCheckUpdate)
                fetchAppsUpdate(updateCheckJsonObject.toString().getBase64(false))
            loadingState.postValue(LoadingState.LoadingDone)
        }
    }

}