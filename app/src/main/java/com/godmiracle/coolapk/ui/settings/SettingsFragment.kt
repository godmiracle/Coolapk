package com.godmiracle.coolapk.ui.settings

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.text.HtmlCompat
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.fragment.app.DialogFragment
import com.godmiracle.coolapk.BuildConfig
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.DialogAboutBinding
import com.godmiracle.coolapk.databinding.FragmentSettingsBinding
import com.godmiracle.coolapk.ui.base.BaseFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.material.app.LocaleDelegate

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMenu()

    }

    private fun initMenu() {
        binding.toolBar.setNavigationIcon(R.drawable.ic_back)
        binding.toolBar.setNavigationOnClickListener { activity?.finish() }
        binding.toolBar.inflateMenu(R.menu.settings_menu)
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.feedback -> {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.about_repository_issues_url))
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(requireContext(), "打开失败", Toast.LENGTH_SHORT).show()
                    }
                }

                R.id.about -> AboutDialog().show(childFragmentManager, "about")

            }
            return@setOnMenuItemClickListener true
        }
    }

    class AboutDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val binding: DialogAboutBinding =
                DialogAboutBinding.inflate(layoutInflater, null, false)
            binding.designAboutTitle.setText(R.string.app_name)
            binding.designAboutInfo.movementMethod = LinkMovementMethodCompat.getInstance()
            binding.designAboutInfo.highlightColor = ColorUtils.setAlphaComponent(
                MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorPrimaryDark,
                    0
                ), 128
            )
            binding.designAboutInfo.text = HtmlCompat.fromHtml(
                getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"" + getString(R.string.about_repository_url) + "\">GitHub</a></b>",
                ), HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            binding.designAboutInfo.append("\n\n")
            binding.designAboutInfo.append(
                HtmlCompat.fromHtml(
                    getString(
                        R.string.about_dialog_info,
                        getString(R.string.about_repository_url),
                        getString(R.string.about_fork_source_url),
                        getString(R.string.about_original_repository_url)
                    ),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            )
            binding.designAboutVersion.text = java.lang.String.format(
                LocaleDelegate.defaultLocale,
                "%s (%d)",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
            return MaterialAlertDialogBuilder(requireContext()).setView(binding.root).create()
        }
    }


}
