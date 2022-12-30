package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.category

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentCategoryBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewFragment
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull

class TorrentCategoryDialog() : DialogFragment(R.layout.dialog_torrent_category) {
    private val viewModel: TorrentCategoryViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!
    private val selectedCategory get() = arguments?.getString("selectedCategory")

    private val parentFragment get() = (requireParentFragment() as TorrentOverviewFragment)

    constructor(serverConfig: ServerConfig, selectedCategory: String?) : this() {
        arguments = bundleOf(
            "serverConfig" to serverConfig,
            "selectedCategory" to selectedCategory
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).apply {
        val binding = DialogTorrentCategoryBinding.inflate(layoutInflater)
        setView(binding.root)
        setTitle(R.string.torrent_category_dialog_title)
        setPositiveButton { _, _ ->
            val selectedCategory = binding.chipGroupCategory.checkedChipId.let { id ->
                if (id != View.NO_ID) {
                    binding.chipGroupCategory.findViewById<Chip>(id).text.toString()
                } else {
                    null
                }
            }
            parentFragment.onSetCategoryDialogResult(selectedCategory)
        }
        setNegativeButton()

        viewModel.updateCategories(serverConfig)

        viewModel.categories.filterNotNull().launchAndCollectLatestIn(this@TorrentCategoryDialog) { categories ->
            categories.forEach { category ->
                val chip = Chip(requireContext())
                chip.text = category
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_category)
                chip.ellipsize = TextUtils.TruncateAt.END
                chip.isCheckable = true

                if (category == selectedCategory) {
                    chip.isChecked = true
                }

                binding.chipGroupCategory.addView(chip)
            }

            cancel()
        }

        viewModel.eventFlow.launchAndCollectLatestIn(this@TorrentCategoryDialog) { event ->
            when (event) {
                is TorrentCategoryViewModel.Event.Error -> {
                    parentFragment.onSetCategoryDialogError(event.result)
                    dismiss()
                }
            }
        }
    }.create()
}
