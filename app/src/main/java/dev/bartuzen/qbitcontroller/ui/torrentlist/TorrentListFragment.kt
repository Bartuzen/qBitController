package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentListBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.filterNotNull

@FragmentWithArgs
@AndroidEntryPoint
class TorrentListFragment : ArgsFragment(R.layout.fragment_torrent_list) {
    private var _binding: FragmentTorrentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentListViewModel by viewModels()

    @Arg
    lateinit var serverConfig: ServerConfig

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentListBinding.bind(view)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.torrent_list_menu, menu)

                viewModel.torrentSort.launchAndCollectLatestIn(viewLifecycleOwner) { sort ->
                    val selectedSort = when (sort) {
                        TorrentSort.HASH -> R.id.menu_sort_hash
                        TorrentSort.NAME -> R.id.menu_sort_name
                        TorrentSort.DOWNLOAD_SPEED -> R.id.menu_sort_dlspeed
                        TorrentSort.UPLOAD_SPEED -> R.id.menu_sort_upspeed
                        TorrentSort.PRIORITY -> R.id.menu_sort_priority
                    }
                    menu.findItem(selectedSort).isChecked = true
                }

            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val sort = when (menuItem.itemId) {
                    R.id.menu_sort_name -> TorrentSort.NAME
                    R.id.menu_sort_hash -> TorrentSort.HASH
                    R.id.menu_sort_dlspeed -> TorrentSort.DOWNLOAD_SPEED
                    R.id.menu_sort_upspeed -> TorrentSort.UPLOAD_SPEED
                    R.id.menu_sort_priority -> TorrentSort.PRIORITY
                    else -> return false
                }
                viewModel.setTorrentSort(sort)

                viewModel.isLoading.value = true
                viewModel.updateTorrentList(serverConfig, sort).invokeOnCompletion {
                    viewModel.isLoading.value = false
                }

                return true
            }

        }, viewLifecycleOwner)

        val adapter = TorrentListAdapter(object : TorrentListAdapter.OnItemClickListener {
            override fun onClick(torrent: Torrent) {
                val intent = Intent(context, TorrentActivity::class.java).apply {
                    putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
                    putExtra(TorrentActivity.Extras.SERVER_CONFIG, serverConfig)
                }
                startActivity(intent)
            }
        })
        binding.recyclerTorrentList.adapter = adapter
        binding.recyclerTorrentList.setItemMargin(8, 8)

        viewModel.torrentList.filterNotNull()
            .launchAndCollectLatestIn(viewLifecycleOwner) { torrentList ->
                adapter.submitList(torrentList)
            }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.torrentListEvent.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentListViewModel.TorrentListEvent.Error -> {
                    showSnackbar(requireContext().getErrorMessage(event.result))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}