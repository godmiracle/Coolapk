package com.godmiracle.coolapk.util

import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

/**
 * iOS Coolapk request identity, aligned with XiaoMengXinX/c001apk_next.
 *
 * Keep this class free of Android APIs so the header and token format can be
 * compared byte-for-byte with the upstream implementation.
 */
data class IosDevice(
    val id: String,
    val model: String,
    val hardwareId: String,
    val salesModel: String,
    val systemVersion: String,
)

object IosHeaders {
    const val BUNDLE_ID = "com.coolapk.app"
    const val APP_VERSION = "16.6.1"
    const val APP_CODE = "2610151"
    const val API_VERSION = "16"
    const val SDK_LOCALE = "zh-HK"
    const val ACCEPT_LANGUAGE = "zh-Hans-HK;q=1.0, zh-Hant-HK;q=0.9, en-HK;q=0.8"

    private const val TOKEN_PREFIX =
        "token://com.coolapk.app/1e7870f7312eb435aef6b59b211a9880?"

    fun appDevice(device: IosDevice): String {
        val hardware = if (device.salesModel.isBlank()) {
            device.hardwareId
        } else {
            "${device.hardwareId}/${device.salesModel}"
        }
        val plain = listOf(
            device.id, "", "", "",
            "Apple", "Apple", device.model, hardware
        ).joinToString("; ")
        return Base64.getEncoder()
            .encodeToString(plain.toByteArray(Charsets.UTF_8))
            .reversed()
            .trimStart('=')
    }

    fun userAgent(device: IosDevice): String =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 " +
            "(#Build; Apple; ${device.hardwareId}; ${device.model}; iOS_${device.systemVersion}) " +
            "+iCoolMarket/$APP_VERSION-$APP_CODE"

    fun appToken(device: IosDevice, timestampSeconds: Long): String {
        val timeMd5 = md5Hex(timestampSeconds.toString())
        val material = TOKEN_PREFIX + timeMd5 + "$" + device.id + "&" + BUNDLE_ID
        val digest = md5Hex(
            Base64.getEncoder().encodeToString(material.toByteArray(Charsets.UTF_8))
        )
        return digest + device.id + "0x" + Integer.toHexString(timestampSeconds.toInt())
    }

    fun headers(device: IosDevice, timestampSeconds: Long): Map<String, String> =
        linkedMapOf(
            "Accept" to "*/*",
            "Accept-Language" to ACCEPT_LANGUAGE,
            "User-Agent" to userAgent(device),
            "X-Requested-With" to "XMLHttpRequest",
            "X-Sdk-Int" to device.systemVersion,
            "X-Sdk-Locale" to SDK_LOCALE,
            "X-App-Id" to BUNDLE_ID,
            "X-App-Version" to APP_VERSION,
            "X-App-Code" to APP_CODE,
            "X-Api-Version" to API_VERSION,
            "X-App-Device" to appDevice(device),
            "X-App-Token" to appToken(device, timestampSeconds),
        )

    private fun md5Hex(value: String): String =
        md5Hex(value.toByteArray(Charsets.UTF_8))

    private fun md5Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("MD5").digest(bytes).joinToString("") {
            "%02x".format(Locale.US, it.toInt() and 0xFF)
        }
}
