package com.godmiracle.coolapk.ui.clientupdate

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.ActivityClientUpdateBinding
import com.godmiracle.coolapk.logic.network.NetworkEndpoints
import com.godmiracle.coolapk.logic.update.GitHubReleaseUpdateStatus
import com.godmiracle.coolapk.logic.update.ValidatedGitHubRelease
import com.godmiracle.coolapk.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClientUpdateActivity : BaseActivity<ActivityClientUpdateBinding>() {

    private val viewModel by viewModels<ClientUpdateViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.client_update)

        binding.currentVersion.text = getString(
            R.string.update_current_version,
            viewModel.currentVersionName,
            viewModel.currentVersionCode
        )
        binding.checkButton.setOnClickListener { viewModel.checkLatest() }
        binding.browserButton.setOnClickListener { openReleasePage() }
        viewModel.state.observe(this, ::render)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun render(state: ClientUpdateState) {
        binding.checkButton.isEnabled = state !is ClientUpdateState.Checking
        binding.progressBar.isVisible = state is ClientUpdateState.Checking
        binding.browserButton.isVisible = state is ClientUpdateState.Idle ||
            state is ClientUpdateState.Error
        binding.releaseTitle.isVisible = false
        binding.releaseMeta.isVisible = false
        binding.releaseBody.isVisible = false

        when (state) {
            ClientUpdateState.Idle -> {
                binding.status.text = getString(R.string.update_ready_to_check)
            }

            ClientUpdateState.Checking -> {
                binding.status.text = getString(R.string.update_checking)
            }

            is ClientUpdateState.Checked -> {
                renderRelease(state.release)
                binding.status.text = statusText(state.release, state.status)
                binding.browserButton.isVisible = state.status == GitHubReleaseUpdateStatus.AVAILABLE ||
                    state.status == GitHubReleaseUpdateStatus.CURRENT_DEVELOPMENT
            }

            is ClientUpdateState.Error -> {
                binding.status.text = state.message
            }
        }
    }

    private fun renderRelease(release: ValidatedGitHubRelease) {
        binding.releaseTitle.text = getString(
            R.string.update_release_title,
            release.title,
            release.tagName
        )
        binding.releaseMeta.text = release.publishedAt?.let {
            getString(R.string.update_release_published_at, it)
        }.orEmpty()
        binding.releaseBody.text = release.body
        binding.releaseTitle.isVisible = true
        binding.releaseMeta.isVisible = binding.releaseMeta.text.isNotBlank()
        binding.releaseBody.isVisible = true
    }

    private fun statusText(
        release: ValidatedGitHubRelease,
        status: GitHubReleaseUpdateStatus
    ): String {
        return when (status) {
            GitHubReleaseUpdateStatus.AVAILABLE -> getString(
                R.string.update_available,
                release.version
            )

            GitHubReleaseUpdateStatus.CURRENT_DEVELOPMENT -> getString(
                R.string.update_available_development,
                release.version
            )

            GitHubReleaseUpdateStatus.UP_TO_DATE -> getString(
                R.string.update_up_to_date,
                release.version
            )

            GitHubReleaseUpdateStatus.CURRENT_NEWER -> getString(
                R.string.update_current_newer,
                release.version
            )
        }
    }

    private fun openReleasePage() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(NetworkEndpoints.GITHUB_RELEASE_LATEST_PAGE_URL)
                )
            )
        } catch (e: ActivityNotFoundException) {
            showMessage(getString(R.string.update_browser_not_found))
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
