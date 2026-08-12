package com.godmiracle.coolapk.ui.follow

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.databinding.ItemLocalFollowBinding
import com.godmiracle.coolapk.logic.model.LocalFollow
import com.godmiracle.coolapk.logic.model.LocalFollowType
import com.godmiracle.coolapk.util.ImageUtil
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors

class LocalFollowAdapter(
    private val onClick: (LocalFollow) -> Unit,
    private val onDelete: (LocalFollow) -> Unit
) : ListAdapter<LocalFollow, LocalFollowAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: ItemLocalFollowBinding,
        private val onClick: (LocalFollow) -> Unit,
        private val onDelete: (LocalFollow) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocalFollow) {
            binding.type.text = if (item.type == LocalFollowType.TOPIC) "话题" else "数码"
            binding.title.text = item.title
            Glide.with(binding.icon).clear(binding.icon)
            if (item.avatar.isBlank()) {
                binding.icon.setImageResource(
                    if (item.type == LocalFollowType.TOPIC) R.drawable.outline_tag_24
                    else R.drawable.ic_phone
                )
                binding.icon.setColorFilter(
                    MaterialColors.getColor(
                        binding.icon,
                        com.google.android.material.R.attr.colorPrimary
                    )
                )
            } else {
                binding.icon.clearColorFilter()
                binding.icon.setImageDrawable(null)
                ImageUtil.showIMG(binding.icon, item.avatar)
            }
            binding.root.setOnClickListener { onClick(item) }
            binding.delete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLocalFollowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onClick,
            onDelete
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<LocalFollow>() {
        override fun areItemsTheSame(oldItem: LocalFollow, newItem: LocalFollow): Boolean {
            return oldItem.type == newItem.type && oldItem.targetId == newItem.targetId
        }

        override fun areContentsTheSame(oldItem: LocalFollow, newItem: LocalFollow): Boolean {
            return oldItem == newItem
        }
    }

}
