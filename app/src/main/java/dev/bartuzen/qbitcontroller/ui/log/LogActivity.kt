package dev.bartuzen.qbitcontroller.ui.log

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.databinding.ActivityLogBinding
import dev.bartuzen.qbitcontroller.utils.applyNavigationBarInsets
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class LogActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
    }

    lateinit var binding: ActivityLogBinding

    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.layoutAppBar.applySystemBarInsets()
        binding.recyclerLog.applyNavigationBarInsets()

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
        if (serverId == -1) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val adapter = LogAdapter()
        binding.recyclerLog.adapter = adapter

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssFeed(serverId)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshRssFeed(serverId)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isLoading.launchAndCollectLatestIn(this) { isLoading ->
            if (isLoading) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.logs.filterNotNull().launchAndCollectLatestIn(this) { log ->
            adapter.submitLogs(log)
        }

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is LogViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(this@LogActivity, event.error))
                }
            }
        }
    }
}
