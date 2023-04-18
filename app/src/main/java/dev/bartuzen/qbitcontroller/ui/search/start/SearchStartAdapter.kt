package dev.bartuzen.qbitcontroller.ui.search.start

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemPluginBinding
import dev.bartuzen.qbitcontroller.databinding.ItemSearchStartHeaderBinding
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.utils.text

class SearchStartAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var plugins: List<Plugin> = emptyList()
    private var selectedCategoryPosition = 0
    private var selectedPluginOption: PluginSelection = PluginSelection.ENABLED
        set(value) {
            val oldState = field
            field = value

            if (oldState != value) {
                if (oldState == PluginSelection.SELECTED || value == PluginSelection.SELECTED) {
                    notifyItemRangeChanged(1, plugins.size, Unit)
                }
            }
        }
    private val selectedPlugins = mutableListOf<Plugin>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = if (viewType == R.layout.item_search_start_header) {
        HeaderViewHolder(ItemSearchStartHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    } else {
        PluginViewHolder(ItemPluginBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(selectedPluginOption)
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
        val searchQuery get() = binding.inputLayoutQuery.text
        val category
            get() = when (val position = binding.dropdownCategory.position) {
                0 -> "all"
                1 -> "anime"
                2 -> "books"
                3 -> "games"
                4 -> "movies"
                5 -> "music"
                6 -> "pictures"
                7 -> "software"
                8 -> "tv"
                else -> throw IllegalStateException("Unknown category position: $position")
            }
        val plugins
            get() = when (val id = binding.radioGroupPlugin.checkedRadioButtonId) {
                R.id.radio_plugins_enabled -> "enabled"
                R.id.radio_plugins_all -> "all"
                R.id.radio_plugins_select -> selectedPlugins.joinToString("|") { it.name }
                else -> throw IllegalStateException("Unknown id: $id")
            }

        init {
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

        fun bind(pluginSelection: PluginSelection) {
            val selectedOption = when (pluginSelection) {
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
            binding.dropdownCategory.onItemChangeListener = { position ->
                selectedCategoryPosition = position
            }
            binding.dropdownCategory.setPosition(selectedCategoryPosition)
        }
    }

    inner class PluginViewHolder(private val binding: ItemPluginBinding) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var plugin: Plugin

        init {
            binding.checkboxPlugin.addOnCheckedStateChangedListener { _, state ->
                if (state == MaterialCheckBox.STATE_CHECKED) {
                    selectedPlugins.add(plugin)
                } else {
                    selectedPlugins.remove(plugin)
                }
            }
        }

        fun bind(plugin: Plugin) {
            this.plugin = plugin

            binding.checkboxPlugin.isEnabled = selectedPluginOption == PluginSelection.SELECTED
            binding.checkboxPlugin.isChecked = plugin in selectedPlugins
            binding.checkboxPlugin.text = plugin.fullName
        }
    }

    enum class PluginSelection {
        ALL, ENABLED, SELECTED
    }
}
