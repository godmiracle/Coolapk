package com.godmiracle.coolapk.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.logic.model.StringEntity
import com.godmiracle.coolapk.ui.blacklist.IOnItemClickListener

class HistoryAdapter :
    ListAdapter<StringEntity, HistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    private lateinit var iOnItemClickListener: IOnItemClickListener

    fun setOnItemClickListener(iOnItemClickListener: IOnItemClickListener) {
        this.iOnItemClickListener = iOnItemClickListener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var keyWord: TextView = view.findViewById(R.id.keyWord)
        val delete: ImageView = view.findViewById(R.id.delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_search_history, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.keyWord.setOnLongClickListener {
            viewHolder.delete.isVisible = viewHolder.delete.visibility == View.GONE
            true
        }
        viewHolder.delete.setOnClickListener {
            iOnItemClickListener.onItemDeleteClick(
                viewHolder.keyWord.text.toString()
            )
            viewHolder.delete.isVisible = false
        }
        viewHolder.keyWord.setOnClickListener {
            iOnItemClickListener.onItemClick(viewHolder.keyWord.text.toString())
        }
        return viewHolder
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.keyWord.text = currentList[position].data
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<StringEntity>() {
    override fun areItemsTheSame(
        oldItem: StringEntity,
        newItem: StringEntity
    ): Boolean {
        return oldItem.data == newItem.data
    }

    override fun areContentsTheSame(
        oldItem: StringEntity,
        newItem: StringEntity
    ): Boolean {
        return oldItem.data == newItem.data
    }
}