package dev.bartuzen.qbitcontroller.ui.base

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class MultiSelectAdapter<T, K, VH : MultiSelectAdapter.ViewHolder<T, K>>(
    diffCallBack: DiffUtil.ItemCallback<T>,
    protected val getKey: (item: T) -> K
) : ListAdapter<T, VH>(diffCallBack) {
    private val _selectedItems = mutableListOf<K>()
    val selectedItems: List<K> get() = _selectedItems

    val isInSelectionMode get() = selectedItems.isNotEmpty()

    val selectedItemCount get() = selectedItems.size

    protected fun isItemSelected(key: K) = key in selectedItems

    private var onClick: ((item: T) -> Unit)? = null
    private var onSelectionModeStart: (() -> Unit)? = null
    private var onSelectionModeEnd: (() -> Unit)? = null
    private var onUpdateSelection: (() -> Unit)? = null

    fun onClick(block: (item: T) -> Unit) {
        onClick = block
    }

    fun onSelectionModeStart(block: () -> Unit) {
        onSelectionModeStart = block
    }

    fun onSelectionModeEnd(block: () -> Unit) {
        onSelectionModeEnd = block
    }

    fun onUpdateSelection(block: () -> Unit) {
        onUpdateSelection = block
    }

    fun finishSelection() {
        val selectedItemsCopy = selectedItems.toList()
        _selectedItems.clear()
        currentList.forEachIndexed { index, item ->
            if (getKey(item) in selectedItemsCopy) {
                notifyItemChanged(index)
            }
        }
    }

    fun selectAll() {
        currentList.forEachIndexed { index, item ->
            val key = getKey(item)
            if (key !in selectedItems) {
                _selectedItems.add(key)
                notifyItemChanged(index)
            }
        }
        onUpdateSelection?.invoke()
    }

    fun selectInverse() {
        val inverseList = mutableListOf<K>()

        currentList.forEach { item ->
            val key = getKey(item)
            if (key !in selectedItems) {
                inverseList.add(key)
            }
        }

        _selectedItems.clear()
        _selectedItems.addAll(inverseList)

        if (inverseList.isEmpty()) {
            onSelectionModeEnd?.invoke()
        } else {
            onUpdateSelection?.invoke()
        }

        currentList.forEachIndexed { index, _ ->
            notifyItemChanged(index)
        }
    }

    // if an item is removed after list changed, remove its key from selected list
    override fun onCurrentListChanged(previousList: MutableList<T>, currentList: MutableList<T>) {
        val updatedList = selectedItems.filter { key ->
            currentList.find { item ->
                getKey(item) == key
            } != null
        }

        val isOldListEmpty = selectedItems.isEmpty()

        _selectedItems.clear()
        _selectedItems.addAll(updatedList)

        if (updatedList.size != selectedItemCount) {
            onUpdateSelection?.invoke()
        }

        if (!isOldListEmpty && updatedList.isEmpty()) {
            onSelectionModeEnd?.invoke()
        }
    }

    open class ViewHolder<T, K>(itemView: View, private val adapter: MultiSelectAdapter<T, K, *>) :
        RecyclerView.ViewHolder(itemView) {

        private var isSelected: Boolean
            get() = if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                val item = adapter.getItem(bindingAdapterPosition)
                val key = adapter.getKey(item)
                adapter.isItemSelected(key)
            } else {
                false
            }
            set(value) {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    adapter.getItem(bindingAdapterPosition)?.let { item: T ->
                        if (value) {
                            adapter._selectedItems.add(adapter.getKey(item))
                        } else {
                            adapter._selectedItems.remove(adapter.getKey(item))
                        }
                        adapter.notifyItemChanged(bindingAdapterPosition, Unit)
                    }
                }
            }

        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    if (adapter.isInSelectionMode) {
                        if (isSelected) {
                            isSelected = false
                            adapter.onUpdateSelection?.invoke()
                            if (!adapter.isInSelectionMode) {
                                adapter.onSelectionModeEnd?.invoke()
                            }
                        } else {
                            isSelected = true
                            adapter.onUpdateSelection?.invoke()
                        }
                    } else {
                        adapter.getItem(bindingAdapterPosition)?.let { item ->
                            adapter.onClick?.invoke(item)
                        }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && !adapter.isInSelectionMode) {
                    isSelected = true
                    adapter.onSelectionModeStart?.invoke()
                    adapter.onUpdateSelection?.invoke()
                }
                true
            }
        }
    }
}
