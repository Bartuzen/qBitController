package dev.bartuzen.qbitcontroller.ui.search

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivitySearchBinding
import dev.bartuzen.qbitcontroller.ui.search.start.SearchStartFragment

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySearchBinding.inflate(layoutInflater)
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
                replace(R.id.container, SearchStartFragment(serverId))
            }
        }
    }
}
