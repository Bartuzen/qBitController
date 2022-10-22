package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hannesdorfmann.fragmentargs.FragmentArgs
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentDeleteDialogBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig

@FragmentWithArgs
@AndroidEntryPoint
class TorrentDeleteDialogFragment : DialogFragment() {
    private var _binding: FragmentTorrentDeleteDialogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentOverviewViewModel by viewModels(
        ownerProducer = {
            requireParentFragment()
        }
    )

    @Arg
    lateinit var serverConfig: ServerConfig

    @Arg
    lateinit var torrentHash: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FragmentArgs.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentTorrentDeleteDialogBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.torrent_delete)
            .setView(binding.root)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.deleteTorrent(
                    serverConfig,
                    torrentHash,
                    binding.checkBoxDeleteFiles.isChecked
                )
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}