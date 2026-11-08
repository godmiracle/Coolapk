package com.godmiracle.coolapk.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale
import java.util.UUID

/**
 * Stable anonymous iOS-shaped identity used by the Coolapk API client.
 *
 * This app has no account login or CDID exchange. The persisted UUID and profile
 * are therefore kept stable for this installation.
 */
object IosDeviceUtils {
    private val lock = Any()

    @Volatile
    private var cached: IosDevice? = null

    fun current(): IosDevice {
        cached?.let { return it }
        synchronized(lock) {
            cached?.let { return it }
            return loadOrCreate().also { cached = it }
        }
    }

    fun regenerate(): IosDevice = synchronized(lock) {
        createAndPersist(current().id).also { cached = it }
    }

    fun updateProfile(
        model: String = current().model,
        hardwareId: String = current().hardwareId,
        salesModel: String = current().salesModel,
        systemVersion: String = current().systemVersion,
    ): IosDevice = synchronized(lock) {
        val base = current()
        val updated = IosDevice(
            id = base.id,
            model = sanitize(model).ifBlank { base.model },
            hardwareId = sanitize(hardwareId).ifBlank { base.hardwareId },
            salesModel = sanitize(salesModel),
            systemVersion = sanitize(systemVersion).ifBlank { base.systemVersion },
        )
        persist(updated)
        cached = updated
        updated
    }

    fun appDevice(device: IosDevice = current()): String =
        IosHeaders.appDevice(device)

    fun userAgent(device: IosDevice = current()): String =
        IosHeaders.userAgent(device)

    fun coolapkUserAgent(): String = userAgent()

    /**
     * Use the iOS UA only for Coolapk hosts. Third-party WebViews keep Android
     * WebView's default UA; other image hosts can retain their existing fallback.
     */
    fun webUserAgentFor(url: String?): String? {
        val host = url?.toHttpUrlOrNull()?.host?.lowercase(Locale.US) ?: return null
        return if (host == "coolapk.com" || host.endsWith(".coolapk.com")) {
            coolapkUserAgent()
        } else {
            null
        }
    }

    fun nowSeconds(): Long = System.currentTimeMillis() / 1000L

    fun appToken(
        timestampSeconds: Long = nowSeconds(),
        device: IosDevice = current(),
    ): String = IosHeaders.appToken(device, timestampSeconds)

    fun headers(
        timestampSeconds: Long = nowSeconds(),
        device: IosDevice = current(),
    ): Map<String, String> = IosHeaders.headers(device, timestampSeconds)

    private fun loadOrCreate(): IosDevice {
        val storedId = PrefManager.iosDeviceId
        val id = storedId.takeIf(::isUuid) ?: newRandomUuid()
        val model = PrefManager.iosModel
        val hardwareId = PrefManager.iosHardwareId
        val systemVersion = PrefManager.iosSystemVersion

        if (model.isNotBlank() && hardwareId.isNotBlank() && systemVersion.isNotBlank()) {
            return IosDevice(
                id = id,
                model = model,
                hardwareId = hardwareId,
                salesModel = PrefManager.iosSalesModel,
                systemVersion = systemVersion,
            ).also { if (id != storedId) persist(it) }
        }
        return createAndPersist(id)
    }

    private fun createAndPersist(id: String): IosDevice {
        val profile = PROFILES.random()
        return IosDevice(
            id = id,
            model = profile.model,
            hardwareId = profile.hardwareId,
            salesModel = profile.salesModel,
            systemVersion = VERSIONS.random(),
        ).also(::persist)
    }

    private fun persist(device: IosDevice) {
        PrefManager.saveIosDevice(
            id = device.id,
            model = device.model,
            hardwareId = device.hardwareId,
            salesModel = device.salesModel,
            systemVersion = device.systemVersion,
        )
    }

    private fun newRandomUuid(): String =
        UUID.randomUUID().toString().uppercase(Locale.US)

    private fun isUuid(value: String): Boolean =
        UUID_PATTERN.matches(value)

    private fun sanitize(value: String): String =
        value.trim().filter { it.code in 0x20..0x7E && it != ';' }

    private data class Profile(
        val model: String,
        val hardwareId: String,
        val salesModel: String,
    )

    private val PROFILES = listOf(
        Profile("iPhone 15", "iPhone15,4", "MTLD3CH/A"),
        Profile("iPhone 15", "iPhone15,4", "MTLG3CH/A"),
        Profile("iPhone 16", "iPhone17,3", "MYEV3CH/A"),
        Profile("iPhone 16", "iPhone17,3", "MYEW3CH/A"),
        Profile("iPhone 17", "iPhone18,3", "MG6W4CH/A"),
        Profile("iPhone 17", "iPhone18,3", "MG704CH/A"),
    )

    private val VERSIONS = listOf("26.7", "26.7.1", "27.0.1")

    private val UUID_PATTERN =
        Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
}
