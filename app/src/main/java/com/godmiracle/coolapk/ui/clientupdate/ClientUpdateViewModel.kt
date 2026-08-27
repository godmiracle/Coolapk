package com.godmiracle.coolapk.ui.clientupdate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godmiracle.coolapk.BuildConfig
import com.godmiracle.coolapk.logic.repository.GitHubReleaseRepository
import com.godmiracle.coolapk.logic.update.GitHubReleaseUpdateStatus
import com.godmiracle.coolapk.logic.update.ValidatedGitHubRelease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

sealed interface ClientUpdateState {
    data object Idle : ClientUpdateState
    data object Checking : ClientUpdateState
    data class Checked(
        val release: ValidatedGitHubRelease,
        val status: GitHubReleaseUpdateStatus
    ) : ClientUpdateState

    data class Error(val message: String) : ClientUpdateState
}

@HiltViewModel
class ClientUpdateViewModel @Inject constructor(
    private val repository: GitHubReleaseRepository
) : ViewModel() {

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Long = BuildConfig.VERSION_CODE.toLong()

    private val _state = MutableLiveData<ClientUpdateState>(ClientUpdateState.Idle)
    val state: LiveData<ClientUpdateState> = _state
    private var requestJob: Job? = null

    fun checkLatest() {
        if (requestJob?.isActive == true) return
        _state.value = ClientUpdateState.Checking
        requestJob = viewModelScope.launch {
            try {
                val release = repository.getLatestRelease()
                _state.value = ClientUpdateState.Checked(
                    release = release,
                    status = com.godmiracle.coolapk.logic.update.compareGitHubReleaseVersion(
                        currentVersionName,
                        release.version
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ClientUpdateState.Error(e.toClientUpdateUserMessage())
            }
        }
    }

}

internal fun Throwable.toClientUpdateUserMessage(): String {
    return when {
        this is HttpException -> "GitHub API 请求失败（HTTP ${code()}）"
        isGitHubConnectionFailure() -> "连接 GitHub 失败，请使用“浏览器下载 Release（推荐）”打开更新页"
        this is IOException -> message?.takeIf { it.isNotBlank() } ?: "网络或文件操作失败"
        else -> message?.takeIf { it.isNotBlank() } ?: "更新检查失败"
    }
}

private fun Throwable.isGitHubConnectionFailure(): Boolean {
    var current: Throwable? = this
    repeat(8) {
        val cause = current ?: return false
        if (
            cause is java.net.ConnectException ||
            cause is java.net.UnknownHostException ||
            cause is java.net.SocketTimeoutException ||
            cause.message?.contains("failed to connect", ignoreCase = true) == true ||
            cause.message?.contains("timeout", ignoreCase = true) == true
        ) {
            return true
        }
        current = cause.cause
    }
    return false
}
