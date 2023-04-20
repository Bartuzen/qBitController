package dev.bartuzen.qbitcontroller.ui.search.start

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemPluginBinding
import dev.bartuzen.qbitcontroller.databinding.ItemSearchStartHeaderBinding
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.utils.text

class SearchStartAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var plugins: List<Plugin> = emptyList()

    var searchQuery = ""
    var selectedCategoryPosition = 0
    var selectedPluginOption: PluginSelection = PluginSelection.ENABLED
        set(value) {
            val oldState = field
            field = value

            if (oldState != value) {
                if (oldState == PluginSelection.SELECTED || value == PluginSelection.SELECTED) {
                    notifyItemRangeChanged(1, plugins.size, Unit)
                }
            }
        }
    val selectedPlugins = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = if (viewType == R.layout.item_search_start_header) {
        HeaderViewHolder(ItemSearchStartHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    } else {
        PluginViewHolder(ItemPluginBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind()
            }
            is PluginViewHolder -> {
                holder.bind(plugins[position - 1])
            }
        }
    }

    override fun getItemCount() = plugins.size + 1

    override fun getItemViewType(position: Int) =
        if (position == 0) R.layout.item_search_start_header else R.layout.item_plugin

    @SuppressLint("NotifyDataSetChanged")
    fun submitPlugins(plugins: List<Plugin>) {
        this.plugins = plugins
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(val binding: ItemSearchStartHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.inputLayoutQuery.editText!!.addTextChangedListener { text ->
                searchQuery = text.toString()
            }

            binding.dropdownCategory.onItemChangeListener = { position ->
                selectedCategoryPosition = position
            }

            binding.radioGroupPlugin.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radio_plugins_enabled -> {
                        selectedPluginOption = PluginSelection.ENABLED
                    }
                    R.id.radio_plugins_all -> {
                        selectedPluginOption = PluginSelection.ALL
                    }
                    R.id.radio_plugins_select -> {
                        selectedPluginOption = PluginSelection.SELECTED
                    }
                }
            }
        }

        fun bind() {
            binding.inputLayoutQuery.text = searchQuery

            binding.dropdownCategory.setItems(
                R.string.search_start_category_all,
                R.string.search_start_category_anime,
                R.string.search_start_category_books,
                R.string.search_start_category_games,
                R.string.search_start_category_movies,
                R.string.search_start_category_music,
                R.string.search_start_category_pictures,
                R.string.search_start_category_software,
                R.string.search_start_category_tv_shows
            )
            binding.dropdownCategory.setPosition(selectedCategoryPosition)

            val selectedOption = when (selectedPluginOption) {
                PluginSelection.ENABLED -> {
                    R.id.radio_plugins_enabled
                }
                PluginSelection.ALL -> {
                    R.id.radio_plugins_all
                }
                PluginSelection.SELECTED -> {
                    R.id.radio_plugins_select
                }
            }
            binding.radioGroupPlugin.check(selectedOption)
        }
    }

    inner class PluginViewHolder(private val binding: ItemPluginBinding) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var plugin: Plugin

        init {
            binding.checkboxPlugin.addOnCheckedStateChangedListener { _, state ->
                if (state == MaterialCheckBox.STATE_CHECKED) {
                    selectedPlugins.add(plugin.name)
                } else {
                    selectedPlugins.remove(plugin.name)
                }
            }
        }

        fun bind(plugin: Plugin) {
            this.plugin = plugin

            binding.checkboxPlugin.isEnabled = selectedPluginOption == PluginSelection.SELECTED
            binding.checkboxPlugin.isChecked = plugin.name in selectedPlugins
            binding.checkboxPlugin.text = plugin.fullName
        }
    }

    enum class PluginSelection {
        ENABLED, ALL, SELECTED
    }
}
