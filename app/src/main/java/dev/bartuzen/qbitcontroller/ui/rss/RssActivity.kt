package dev.bartuzen.qbitcontroller.ui.rss

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityRssBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.rss.feeds.RssFeedsFragment
import dev.bartuzen.qbitcontroller.utils.getParcelable

@AndroidEntryPoint
class RssActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_CONFIG = "dev.bartuzen.qbitcontroller.SERVER_CONFIG"
    }

    private lateinit var binding: ActivityRssBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRssBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serverConfig = intent.getParcelable<ServerConfig>(Extras.SERVER_CONFIG)

        if (serverConfig == null) {
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

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.container, RssFeedsFragment(serverConfig))
        }
    }
}
