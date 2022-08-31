package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.hannesdorfmann.fragmentargs.FragmentArgs
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentTrackersAddDialogBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig

@FragmentWithArgs
@AndroidEntryPoint
class TorrentTrackersAddDialogFragment : DialogFragment() {
    private var _binding: FragmentTorrentTrackersAddDialogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentTrackersViewModel by viewModels(
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
        _binding = FragmentTorrentTrackersAddDialogBinding.inflate(layoutInflater)

        return AlertDialog.Builder(requireActivity())
            .setTitle(R.string.torrent_trackers_add)
            .setView(binding.root)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.addTrackers(
                    serverConfig,
                    torrentHash,
                    binding.editTrackers.text.toString()
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