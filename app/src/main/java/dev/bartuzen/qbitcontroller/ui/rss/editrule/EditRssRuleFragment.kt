package dev.bartuzen.qbitcontroller.ui.rss.editrule

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentEditRssRuleBinding
import dev.bartuzen.qbitcontroller.model.RssRule
import dev.bartuzen.qbitcontroller.utils.applySafeDrawingInsets
import dev.bartuzen.qbitcontroller.utils.getColorCompat
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
import dev.bartuzen.qbitcontroller.utils.toPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class EditRssRuleFragment() : Fragment(R.layout.fragment_edit_rss_rule) {
    private val binding by viewBinding(FragmentEditRssRuleBinding::bind)

    private val viewModel: EditRssRuleViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val ruleName get() = arguments?.getString("ruleName")!!

    constructor(serverId: Int, ruleName: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "ruleName" to ruleName,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.layoutAppBar.applySafeDrawingInsets(bottom = false)
        binding.progressIndicator.applySafeDrawingInsets(top = false, bottom = false)
        binding.scrollView.applySafeDrawingInsets(top = false)

        binding.toolbar.title = ruleName
        binding.toolbar.setNavigationOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                requireActivity().finish()
            }
        }
        binding.toolbar.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.rss_edit_rule, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_save -> {
                            val newRuleDefinition = constructRuleDefinition()
                            if (newRuleDefinition != null) {
                                viewModel.setRule(serverId, ruleName, newRuleDefinition)
                            }
                        }
                        else -> return false
                    }
                    return true
                }
            },
        )

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadData(serverId, ruleName)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isLoading.launchAndCollectLatestIn(this) { isLoading ->
            if (isLoading) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        binding.dropdownAddPaused.setItems(
            R.string.rss_rule_use_global_settings,
            R.string.rss_rule_add_paused_always,
            R.string.rss_rule_add_paused_never,
        )

        binding.dropdownContentLayout.setItems(
            R.string.rss_rule_use_global_settings,
            R.string.torrent_add_content_layout_original,
            R.string.torrent_add_content_layout_subfolder,
            R.string.torrent_add_content_layout_no_subfolder,
        )

        binding.checkboxSavePathEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.inputLayoutSavePath.isEnabled = isChecked
        }

        viewModel.categories.value?.let { categories ->
            val categoryOptions = categories.toMutableList().apply { add(0, "") }
            binding.dropdownCategory.setItems(categoryOptions)
        }

        viewModel.isFetched.launchAndCollectLatestIn(viewLifecycleOwner) { isFetched ->
            binding.checkboxEnabled.isEnabled = isFetched
            binding.checkboxUseRegex.isEnabled = isFetched
            binding.inputLayoutMustContain.isEnabled = isFetched
            binding.inputLayoutMustNotContain.isEnabled = isFetched
            binding.inputLayoutEpisodeFilter.isEnabled = isFetched
            binding.checkboxSmartEpisodeFilter.isEnabled = isFetched
            binding.inputLayoutCategory.isEnabled = isFetched
            binding.dropdownCategory.isEnabled = isFetched
            binding.checkboxSavePathEnabled.isEnabled = isFetched
            binding.inputLayoutSavePath.isEnabled = isFetched && binding.checkboxSavePathEnabled.isChecked
            binding.inputLayoutIgnoreDays.isEnabled = isFetched
            binding.inputLayoutAddPaused.isEnabled = isFetched
            binding.dropdownAddPaused.isEnabled = isFetched
            binding.inputLayoutContentLayout.isEnabled = isFetched
            binding.dropdownContentLayout.isEnabled = isFetched
        }

        var job: Job? = null
        job = combine(viewModel.rssRule, viewModel.categories, viewModel.feeds) { rssRule, categories, feeds ->
            if (rssRule != null && categories != null && feeds != null) {
                Triple(rssRule, categories, feeds)
            } else {
                null
            }
        }.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { (rssRule, categories, feeds) ->
            binding.checkboxEnabled.isChecked = rssRule.isEnabled
            binding.checkboxUseRegex.isChecked = rssRule.useRegex
            binding.inputLayoutMustContain.text = rssRule.mustContain
            binding.inputLayoutMustNotContain.text = rssRule.mustNotContain
            binding.inputLayoutEpisodeFilter.text = rssRule.episodeFilter
            binding.checkboxSmartEpisodeFilter.isChecked = rssRule.smartFilter
            binding.checkboxSavePathEnabled.isChecked = rssRule.savePath.isNotEmpty()
            binding.inputLayoutSavePath.text = rssRule.savePath
            binding.inputLayoutIgnoreDays.text = rssRule.ignoreDays.toString()
            binding.dropdownAddPaused.setPosition(
                when (rssRule.addPaused) {
                    null -> 0
                    true -> 1
                    false -> 2
                },
            )
            binding.dropdownContentLayout.setPosition(
                when (rssRule.torrentContentLayout) {
                    "Original" -> 1
                    "Subfolder" -> 2
                    "NoSubfolder" -> 3
                    else -> 0
                },
            )

            val categoryOptions = categories.toMutableList().apply { add(0, "") }
            binding.dropdownCategory.setItems(categoryOptions)
            binding.dropdownCategory.setPosition(categoryOptions.indexOf(rssRule.assignedCategory))

            binding.layoutFeeds.removeAllViews()
            if (feeds.isNotEmpty()) {
                feeds.forEach { (name, url) ->
                    val checkbox = MaterialCheckBox(requireContext()).apply {
                        isChecked = rssRule.affectedFeeds.contains(url)
                        text = name
                    }
                    binding.layoutFeeds.addView(checkbox)
                }
            } else {
                val textView = MaterialTextView(requireContext()).apply {
                    setText(R.string.rss_rule_no_feed_found)
                    setTextColor(requireContext().getColorCompat(R.color.md_theme_error))
                    setPadding(8.toPx(requireContext()))
                }
                binding.layoutFeeds.addView(textView)
            }

            job?.cancel()
        }

        val selectedFeeds = savedInstanceState?.getStringArrayList("selectedFeeds")
        if (selectedFeeds != null) {
            binding.layoutFeeds.removeAllViews()
            val feeds = viewModel.feeds.value
            if (feeds?.isNotEmpty() == true) {
                feeds.forEach { (name, url) ->
                    val checkbox = MaterialCheckBox(requireContext()).apply {
                        isChecked = selectedFeeds.contains(url)
                        text = name
                    }
                    binding.layoutFeeds.addView(checkbox)
                }
            } else {
                val textView = MaterialTextView(requireContext()).apply {
                    setText(R.string.rss_rule_no_feed_found)
                    setTextColor(requireContext().getColorCompat(R.color.md_theme_error))
                    setPadding(8.toPx(requireContext()))
                }
                binding.layoutFeeds.addView(textView)
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is EditRssRuleViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                EditRssRuleViewModel.Event.RuleUpdated -> {
                    showSnackbar(R.string.rss_rule_saved_successfully)
                }
                EditRssRuleViewModel.Event.RuleNotFound -> {
                    showSnackbar(R.string.rss_rule_not_found)
                }
            }
        }
    }

    private fun constructRuleDefinition(): RssRule? {
        val categories = viewModel.categories.value ?: return null
        val feeds = viewModel.feeds.value ?: return null

        val isEnabled = binding.checkboxEnabled.isChecked
        val mustContain = binding.inputLayoutMustContain.text
        val mustNotContain = binding.inputLayoutMustNotContain.text
        val useRegex = binding.checkboxUseRegex.isChecked
        val episodeFilter = binding.inputLayoutEpisodeFilter.text
        val ignoreDays = binding.inputLayoutIgnoreDays.text.toIntOrNull() ?: 0
        val addPaused = when (binding.dropdownAddPaused.position) {
            1 -> true
            2 -> false
            else -> null
        }
        val category = binding.dropdownCategory.position.let { position ->
            if (position == 0) {
                ""
            } else {
                categories[position - 1]
            }
        }
        val savePath = if (binding.checkboxSavePathEnabled.isChecked) binding.inputLayoutSavePath.text else ""
        val contentLayout = when (binding.dropdownContentLayout.position) {
            1 -> "Original"
            2 -> "Subfolder"
            3 -> "NoSubfolder"
            else -> null
        }
        val smartFilter = binding.checkboxSmartEpisodeFilter.isChecked

        val affectedFeeds = binding.layoutFeeds.children.mapIndexed { index, view ->
            if (view is MaterialCheckBox && view.isChecked) {
                feeds[index].second
            } else {
                null
            }
        }.filterNotNull().toList()

        return RssRule(
            isEnabled = isEnabled,
            mustContain = mustContain,
            mustNotContain = mustNotContain,
            useRegex = useRegex,
            episodeFilter = episodeFilter,
            ignoreDays = ignoreDays,
            addPaused = addPaused,
            assignedCategory = category,
            savePath = savePath,
            torrentContentLayout = contentLayout,
            smartFilter = smartFilter,
            affectedFeeds = affectedFeeds,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val feeds = viewModel.feeds.value ?: return
        val selectedFeedUrls = binding.layoutFeeds.children.mapIndexed { index, view ->
            if (view is MaterialCheckBox && view.isChecked) {
                feeds[index].second
            } else {
                null
            }
        }.filterNotNull().toList()
        outState.putStringArrayList("selectedFeeds", ArrayList(selectedFeedUrls))
    }
}
