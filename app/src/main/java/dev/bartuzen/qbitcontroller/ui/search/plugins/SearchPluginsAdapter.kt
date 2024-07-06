package dev.bartuzen.qbitcontroller.ui.search.plugins

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemPluginsPluginBinding
import dev.bartuzen.qbitcontroller.model.Plugin

class SearchPluginsAdapter : RecyclerView.Adapter<SearchPluginsAdapter.ViewHolder>() {
    private var plugins: List<Plugin> = emptyList()

    private val _pluginsEnabledState = mutableMapOf<String, Boolean>()
    val pluginsEnabledState get() = _pluginsEnabledState.toMap()

    private val _pluginsToDelete = mutableListOf<String>()
    val pluginsToDelete get() = _pluginsToDelete.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPluginsPluginBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(plugins[position])
    }

    override fun getItemCount() = plugins.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitPlugins(plugins: List<Plugin>) {
        this.plugins.forEach { plugin ->
            if (plugin !in plugins) {
                _pluginsEnabledState.remove(plugin.name)
                _pluginsToDelete.remove(plugin.name)
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

            binding.textVersion.setOnClickListener {
                binding.checkboxPlugin.isChecked = !binding.checkboxPlugin.isChecked
            }

            binding.textUrl.setOnClickListener {
                binding.checkboxPlugin.isChecked = !binding.checkboxPlugin.isChecked
            }

            binding.imageDelete.setOnClickListener {
                val plugin = plugins[bindingAdapterPosition]
                if (plugin.name in _pluginsToDelete) {
                    _pluginsToDelete.remove(plugin.name)
                } else {
                    _pluginsToDelete.add(plugin.name)
                }
                notifyItemChanged(bindingAdapterPosition, Unit)
            }
        }

        fun bind(plugin: Plugin) {
            val context = binding.root.context

            binding.checkboxPlugin.isChecked = _pluginsEnabledState[plugin.name] ?: plugin.isEnabled
            binding.checkboxPlugin.text = plugin.fullName
            binding.textVersion.text = context.getString(R.string.search_plugins_version, plugin.version)
            binding.textUrl.text = plugin.url

            if (plugin.name in _pluginsToDelete) {
                binding.imageDelete.setImageResource(R.drawable.ic_undo)
                binding.checkboxPlugin.isEnabled = false
                binding.textVersion.isEnabled = false
                binding.textUrl.isEnabled = false
            } else {
                binding.imageDelete.setImageResource(R.drawable.ic_delete)
                binding.checkboxPlugin.isEnabled = true
                binding.textVersion.isEnabled = true
                binding.textUrl.isEnabled = true
            }
        }
    }
}
