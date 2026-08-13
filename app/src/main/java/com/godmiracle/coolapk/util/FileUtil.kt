package com.godmiracle.coolapk.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object FileUtil {

    /**
     * 获取可用的cache路径
     */
    fun getAvailableCacheDir(context: Context): File? {
        val file: File? = if (isExternalStorageWritable) {
            context.externalCacheDir
        } else {
            context.cacheDir
        }
        return file
    }

    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

}
