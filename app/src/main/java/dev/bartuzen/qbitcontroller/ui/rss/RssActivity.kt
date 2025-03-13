package dev.bartuzen.qbitcontroller.ui.rss

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityRssBinding
import dev.bartuzen.qbitcontroller.ui.rss.feeds.RssFeedsFragment

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

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)

        if (serverId == -1) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.container, RssFeedsFragment(serverId))
            }
        }
    }
}
