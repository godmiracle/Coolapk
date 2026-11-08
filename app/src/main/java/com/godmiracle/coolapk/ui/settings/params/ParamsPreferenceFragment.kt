package com.godmiracle.coolapk.ui.settings.params

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.core.graphics.ColorUtils
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.util.IosDeviceUtils
import com.godmiracle.coolapk.util.PrefManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ParamsPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
        isVerticalScrollBarEnabled = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(resources.getDrawable(R.drawable.divider, requireContext().theme))
    }

    @SuppressLint("InflateParams")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.params, null)
        refreshSummaries()

        findPreference<Preference>("iosDeviceId")?.setOnPreferenceClickListener {
            showReadOnlyDialog(getString(R.string.ios_device_id), PrefManager.iosDeviceId)
            true
        }

        bindProfileField("iosModel", R.string.ios_model) {
            IosDeviceUtils.updateProfile(model = it)
        }
        bindProfileField("iosHardwareId", R.string.ios_hardware_id) {
            IosDeviceUtils.updateProfile(hardwareId = it)
        }
        bindProfileField("iosSalesModel", R.string.ios_sales_model) {
            IosDeviceUtils.updateProfile(salesModel = it)
        }
        bindProfileField("iosSystemVersion", R.string.ios_system_version) {
            IosDeviceUtils.updateProfile(systemVersion = it)
        }

        findPreference<Preference>("xAppDevice")?.setOnPreferenceClickListener {
            showReadOnlyDialog("X-App-Device", IosDeviceUtils.appDevice())
            true
        }

        findPreference<Preference>("xAppToken")?.setOnPreferenceClickListener {
            val token = IosDeviceUtils.appToken()
            showReadOnlyDialog("X-App-Token", token)
            true
        }

        findPreference<Preference>("regenerate")?.setOnPreferenceClickListener {
            IosDeviceUtils.regenerate()
            refreshSummaries()
            Snackbar.make(
                requireView(),
                getString(R.string.ios_profile_regenerated),
                Snackbar.LENGTH_SHORT
            ).show()
            true
        }
    }

    private fun bindProfileField(key: String, titleRes: Int, update: (String) -> Unit) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener {
            val current = when (key) {
                "iosModel" -> PrefManager.iosModel
                "iosHardwareId" -> PrefManager.iosHardwareId
                "iosSalesModel" -> PrefManager.iosSalesModel
                else -> PrefManager.iosSystemVersion
            }
            showEditDialog(getString(titleRes), current) {
                update(it)
                refreshSummaries()
            }
            true
        }
    }

    private fun refreshSummaries() {
        IosDeviceUtils.current()
        findPreference<Preference>("iosDeviceId")?.summary =
            getString(R.string.ios_device_id_summary)
        findPreference<Preference>("iosModel")?.summary = PrefManager.iosModel
        findPreference<Preference>("iosHardwareId")?.summary = PrefManager.iosHardwareId
        findPreference<Preference>("iosSalesModel")?.summary =
            PrefManager.iosSalesModel.ifEmpty { getString(R.string.ios_sales_model_empty) }
        findPreference<Preference>("iosSystemVersion")?.summary = PrefManager.iosSystemVersion
        findPreference<Preference>("xAppDevice")?.summary = IosDeviceUtils.appDevice()
        findPreference<Preference>("xAppToken")?.summary =
            getString(R.string.ios_token_rotates)
    }

    private fun showEditDialog(title: String, value: String, onSave: (String) -> Unit) {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_x_app_token, null, false)
        val editText: EditText = view.findViewById(R.id.editText)
        editText.highlightColor = ColorUtils.setAlphaComponent(
            MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorPrimaryDark,
                0
            ),
            128
        )
        editText.setText(value)
        MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setTitle(title)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> onSave(editText.text.toString()) }
            .create()
            .apply {
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                editText.requestFocus()
            }
            .show()
    }

    private fun showReadOnlyDialog(title: String, value: String) {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_x_app_token, null, false)
        val editText: EditText = view.findViewById(R.id.editText)
        editText.setText(value)
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setTitle(title)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
