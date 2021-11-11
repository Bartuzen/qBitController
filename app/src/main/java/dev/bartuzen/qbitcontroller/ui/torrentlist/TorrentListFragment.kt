package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentListBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.setItemMargin
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.collect

@FragmentWithArgs
@AndroidEntryPoint
class TorrentListFragment : ArgsFragment(R.layout.fragment_torrent_list) {
    private var _binding: FragmentTorrentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentListViewModel by viewModels()

    @Arg lateinit var serverConfig: ServerConfig

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentListBinding.bind(view)

        setHasOptionsMenu(true)

        val adapter = TorrentListAdapter(object : TorrentListAdapter.OnItemClickListener {
            override fun onClick(torrent: Torrent) {
                val intent = Intent(context, TorrentActivity::class.java).apply {
                    putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
                    putExtra(TorrentActivity.Extras.TORRENT, torrent)
                    putExtra(TorrentActivity.Extras.SERVER_CONFIG, serverConfig)
                }
                startActivity(intent)
            }
        })
        binding.recyclerTorrentList.adapter = adapter
        binding.recyclerTorrentList.setItemMargin(8, 8)

        viewModel.torrentList.observe(viewLifecycleOwner) { torrentList ->
            adapter.submitList(torrentList)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (viewModel.torrentList.isSet) {
            binding.progressIndicator.visibility = View.GONE
        } else {
            viewModel.updateTorrentList(serverConfig)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.torrentListEvent.collect { event ->
                when (event) {
                    is TorrentListViewModel.TorrentListEvent.OnRequestComplete -> {
                        binding.progressIndicator.visibility = View.GONE
                        if (event.result != RequestResult.SUCCESS) {
                            context?.getErrorMessage(event.result)?.let {
                                showSnackbar(it)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.torrent_list_menu, menu)

        viewModel.torrentSort.observe(viewLifecycleOwner) {
            val selectedSort = when (viewModel.torrentSort.value) {
                TorrentSort.HASH -> R.id.menu_sort_hash
                TorrentSort.NAME -> R.id.menu_sort_name
                TorrentSort.DOWNLOAD_SPEED -> R.id.menu_sort_dlspeed
                TorrentSort.UPLOAD_SPEED -> R.id.menu_sort_upspeed
                TorrentSort.PRIORITY -> R.id.menu_sort_priority
                else -> return@observe
            }
            menu.findItem(selectedSort).isChecked = true
        }


    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_sort_name -> {
            viewModel.setTorrentSort(TorrentSort.NAME)
            true
        }
        R.id.menu_sort_hash -> {
            viewModel.setTorrentSort(TorrentSort.HASH)
            true
        }
        R.id.menu_sort_dlspeed -> {
            viewModel.setTorrentSort(TorrentSort.DOWNLOAD_SPEED)
            true
        }
        R.id.menu_sort_upspeed -> {
            viewModel.setTorrentSort(TorrentSort.UPLOAD_SPEED)
            true
        }
        R.id.menu_sort_priority -> {
            viewModel.setTorrentSort(TorrentSort.PRIORITY)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}