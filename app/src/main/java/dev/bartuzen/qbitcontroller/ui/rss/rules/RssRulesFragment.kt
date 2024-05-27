package dev.bartuzen.qbitcontroller.ui.rss.rules

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogRssAddRuleBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentRssRulesBinding
import dev.bartuzen.qbitcontroller.ui.rss.editrule.EditRssRuleFragment
import dev.bartuzen.qbitcontroller.utils.applyNavigationBarInsets
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class RssRulesFragment() : Fragment(R.layout.fragment_rss_rules) {
    private val binding by viewBinding(FragmentRssRulesBinding::bind)

    private val viewModel: RssRulesViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!

    constructor(serverId: Int) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerRules.applyNavigationBarInsets()

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.rss_rules, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_add -> {
                            showCreateRuleDialog()
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        val adapter = RssRulesAdapter(
            onClick = { ruleName ->
                val fragment = EditRssRuleFragment(serverId, ruleName)
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    setDefaultAnimations()
                    replace(R.id.container, fragment)
                    addToBackStack(null)
                }
            },
            onLongClick = { ruleName ->
                showRuleLongClickDialog(ruleName)
            }
        )
        binding.recyclerRules.adapter = adapter

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssRules(serverId)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshRssRules(serverId)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.rssRules.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { rules ->
            adapter.submitList(rules.keys.toList())
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is RssRulesViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                RssRulesViewModel.Event.RuleCreated -> {
                    showSnackbar(R.string.rss_rule_create_success)
                    viewModel.loadRssRules(serverId)
                }
                RssRulesViewModel.Event.RuleRenamed -> {
                    showSnackbar(R.string.rss_rule_rename_success)
                    viewModel.loadRssRules(serverId)
                }
                RssRulesViewModel.Event.RuleDeleted -> {
                    showSnackbar(R.string.rss_rule_delete_success)
                    viewModel.loadRssRules(serverId)
                }
            }
        }
    }

    private fun showCreateRuleDialog() {
        lateinit var dialogBinding: DialogRssAddRuleBinding

        val dialog = showDialog(DialogRssAddRuleBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.rss_rule_action_create)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogBinding.inputLayoutName.text
            if (name.isNotBlank()) {
                viewModel.createRule(serverId, name)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutName.error = getString(R.string.rss_rule_name_cannot_be_empty)
            }
        }
    }

    private fun showRuleLongClickDialog(name: String) {
        showDialog {
            setTitle(name)
            setItems(
                arrayOf(
                    getString(R.string.rss_rule_rename),
                    getString(R.string.rss_rule_delete)
                )
            ) { _, which ->
                when (which) {
                    0 -> {
                        showRenameRuleDialog(name)
                    }
                    1 -> {
                        showDeleteRuleDialog(name)
                    }
                }
            }
            setNegativeButton()
        }
    }

    private fun showRenameRuleDialog(name: String) {
        lateinit var dialogBinding: DialogRssAddRuleBinding

        val dialog = showDialog(DialogRssAddRuleBinding::inflate) { binding ->
            dialogBinding = binding

            binding.inputLayoutName.text = name

            setTitle(R.string.rss_rule_rename)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newName = dialogBinding.inputLayoutName.text
            if (newName.isNotBlank()) {
                viewModel.renameRule(serverId, name, newName)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutName.error = getString(R.string.rss_rule_name_cannot_be_empty)
            }
        }
    }

    private fun showDeleteRuleDialog(name: String) {
        showDialog {
            setTitle(R.string.rss_rule_delete)
            setMessage(getString(R.string.rss_rule_delete_confirm, name))
            setPositiveButton { _, _ ->
                viewModel.deleteRule(serverId, name)
            }
            setNegativeButton()
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.rss_rules)
    }
}
