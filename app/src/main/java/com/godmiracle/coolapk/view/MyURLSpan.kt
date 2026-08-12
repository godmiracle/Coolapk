package com.godmiracle.coolapk.view

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.godmiracle.coolapk.util.ImageUtil
import com.godmiracle.coolapk.util.NetWorkUtil.openLink
import com.godmiracle.coolapk.util.http2https

class MyURLSpan(
    private val mContext: Context,
    private val mUrl: String,
    private val imgList: List<String>?,
    private val showMoreReply: (() -> Unit)? = null
) :
    ClickableSpan() {

    override fun onClick(widget: View) {
        if (mUrl == "") {
            return
        } else if (mUrl.contains("/feed/replyList")) {
            showMoreReply?.let { it() }
        } else if (mUrl.contains("image.coolapk.com")) {
            if (imgList == null) {
                ImageUtil.startBigImgViewSimple(mContext, mUrl.http2https)
            } else {
                ImageUtil.startBigImgViewSimple(mContext, imgList)
            }
        } else {
            openLink(mContext, mUrl, null)
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        /*ds.color = MaterialColors.getColor(
            mContext,
            com.google.android.material.R.attr.colorControlNormal,
            0
        )*/ //设置文本颜色
        ds.isUnderlineText = false //取消下划线
    }
}