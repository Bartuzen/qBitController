package dev.bartuzen.qbitcontroller.ui.view

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.LayoutRes
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat

class ExposedDropdownTextView(context: Context, attrs: AttributeSet?) : MaterialAutoCompleteTextView(context, attrs) {
    var position = 0
        private set

    init {
        inputType = InputType.TYPE_NULL
        keyListener = null
    }

    fun setItems(items: List<String>) {
        adapter = NonFilterArrayAdapter(
            context,
            R.layout.item_dropdown,
            items
        ).apply {
            setOnItemClickListener { _, _, position, _ ->
                this@ExposedDropdownTextView.position = position
                onItemChangeListener?.invoke(position)
            }
        }

        setAdapter(adapter)
        setText(items.firstOrNull())
    }

    fun setItems(vararg items: String) {
        setItems(items.toList())
    }

    @JvmName("setItemIds")
    fun setItems(itemIds: List<Int>) {
        setItems(itemIds.map { context.getString(it) })
    }

    fun setItems(vararg itemIds: Int) {
        setItems(itemIds.toList())
    }

    fun setPosition(position: Int) {
        this.position = position
        setText(adapter.getItem(position))
        onItemChangeListener?.invoke(position)
    }

    private lateinit var adapter: NonFilterArrayAdapter<String>

    var onItemChangeListener: ((position: Int) -> Unit)? = null

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putInt("position", position)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            position = state.getInt("position")
            super.onRestoreInstanceState(state.getParcelableCompat("superState"))

            setText(adapter.getItem(position))
            onItemChangeListener?.invoke(position)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class NonFilterArrayAdapter<T>(context: Context, @LayoutRes resource: Int, objects: List<T>) :
        ArrayAdapter<T>(context, resource, objects) {

        override fun getFilter() = NonFilter()

        class NonFilter : Filter() {
            override fun performFiltering(constraint: CharSequence?) = FilterResults()

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
        }
    }
}
