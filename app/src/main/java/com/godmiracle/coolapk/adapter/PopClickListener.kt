package com.godmiracle.coolapk.adapter

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import com.godmiracle.coolapk.R

class PopClickListener(
    val listener: ItemListener,
    val context: Context,
    val entityType: String,
    val id: String,
    val uid: String,
    val position: Int
) :
    PopupMenu.OnMenuItemClickListener {
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.block -> {
                listener.onBlockUser(id, uid, position)
            }

            R.id.show -> {
                listener.showTotalReply(id, uid, position, null)
            }
        }
        return true
    }

}
