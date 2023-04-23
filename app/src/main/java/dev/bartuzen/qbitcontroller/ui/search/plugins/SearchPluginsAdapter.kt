package dev.bartuzen.qbitcontroller.ui.search.plugins

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemPluginsPluginBinding
import dev.bartuzen.qbitcontroller.model.Plugin

class SearchPluginsAdapter : RecyclerView.Adapter<SearchPluginsAdapter.ViewHolder>() {
    private var plugins: List<Plugin> = emptyList()

    private var _pluginsEnabledState = mutableMapOf<String, Boolean>()
    val pluginsEnabledState get() = _pluginsEnabledState.toMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPluginsPluginBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(plugins[position])
    }

    override fun getItemCount() = plugins.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitPlugins(plugins: List<Plugin>) {
        if (this.plugins.isNotEmpty()) {
            this.plugins.forEach { plugin ->
                if (plugin !in this.plugins) {
                    _pluginsEnabledState.remove(plugin.name)
                }
            }
        }

        this.plugins = plugins
        plugins.forEach { plugin ->
            if (plugin.name !in _pluginsEnabledState) {
                _pluginsEnabledState[plugin.name] = plugin.isEnabled
            }
        }

        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemPluginsPluginBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.checkboxPlugin.setOnCheckedChangeListener { _, isChecked ->
                val plugin = plugins[bindingAdapterPosition]
                _pluginsEnabledState[plugin.name] = isChecked
            }
        }

        fun bind(plugin: Plugin) {
            binding.checkboxPlugin.isChecked = _pluginsEnabledState[plugin.name] ?: plugin.isEnabled
            binding.checkboxPlugin.text = plugin.fullName
        }
    }
}
