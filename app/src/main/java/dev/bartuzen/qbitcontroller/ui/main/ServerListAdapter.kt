package dev.bartuzen.qbitcontroller.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemServerBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig

class ServerListAdapter(private val listener: OnItemClickListener? = null) :
    ListAdapter<ServerConfig, ServerListAdapter.ViewHolder>(DiffCallBack()) {
    var selectedServerId: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)
        currentItem?.let { serverConfig ->
            holder.bind(serverConfig)
        }
    }

    inner class ViewHolder(private val binding: ItemServerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { serverConfig ->
                        currentList.forEachIndexed { i, config ->
                            if (config.id == selectedServerId || config.id == serverConfig.id) {
                                notifyItemChanged(i)
                            }
                        }
                        selectedServerId = serverConfig.id

                        listener?.onClick(serverConfig)
                    }
                }
            }
        }

        fun bind(serverConfig: ServerConfig) {
            binding.root.setBackgroundResource(
                if (serverConfig.id == selectedServerId) {
                    R.color.server_selected_background
                } else {
                    android.R.color.transparent
                }
            )

            if (serverConfig.name != null) {
                binding.textName.text = serverConfig.name
            } else {
                binding.textName.visibility = View.GONE
            }
            binding.textHost.text = serverConfig.host
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<ServerConfig>() {
        override fun areItemsTheSame(oldItem: ServerConfig, newItem: ServerConfig) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ServerConfig, newItem: ServerConfig) =
            oldItem.name == newItem.name && oldItem.host == newItem.name
    }

    interface OnItemClickListener {
        fun onClick(serverConfig: ServerConfig)
    }
}
