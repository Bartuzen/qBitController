package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemDividerBinding

class DividerAdapter : RecyclerView.Adapter<DividerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemDividerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount() = 1

    class ViewHolder(binding: ItemDividerBinding) : RecyclerView.ViewHolder(binding.root)
}
