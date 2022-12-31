package dev.bartuzen.qbitcontroller.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemAddServerBinding

class AddServerAdapter(
    private val onClick: () -> Unit
) : RecyclerView.Adapter<AddServerAdapter.ViewHolder>() {
    var isVisible = false
        set(value) {
            if (field != value) {
                if (value) {
                    notifyItemInserted(0)
                } else {
                    notifyItemRemoved(0)
                }
            }
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAddServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount() = if (isVisible) 1 else 0

    inner class ViewHolder(binding: ItemAddServerBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClick()
                }
            }
        }
    }
}
