package dev.bartuzen.qbitcontroller.ui.rss

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityRssBinding
import dev.bartuzen.qbitcontroller.ui.rss.feeds.RssFeedsFragment
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets

@AndroidEntryPoint
class RssActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
    }

    private lateinit var binding: ActivityRssBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRssBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.layoutAppBar.applySystemBarInsets(bottom = false)

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)

        if (serverId == -1) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.container, RssFeedsFragment(serverId))
            }
        }
    }
}
