package dev.bartuzen.qbitcontroller.ui.rss.rules

import android.app.AlertDialog
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogRssAddRuleBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentRssRulesBinding
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
import dev.bartuzen.qbitcontroller.utils.toPx
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
            }
        )

        val adapter = RssRulesAdapter()
        binding.recyclerRules.adapter = adapter
        binding.recyclerRules.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val verticalPx = 8.toPx(requireContext())
                val horizontalPx = 8.toPx(requireContext())
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = verticalPx
                }
                outRect.bottom = verticalPx
                outRect.left = horizontalPx
                outRect.right = horizontalPx
            }
        })

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssRules(serverId)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshRssRules(serverId)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.rssRules.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { rules ->
            adapter.submitList(rules)
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
            }
        }
    }

    private fun showCreateRuleDialog() {
        lateinit var dialogBinding: DialogRssAddRuleBinding

        val dialog = showDialog(DialogRssAddRuleBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.rss_rule_create)
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
}
