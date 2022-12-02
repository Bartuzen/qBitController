package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivitySettingsBinding
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerFragmentBuilder

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    object Extras {
        const val MOVE_TO_ADD_SERVER = "dev.bartuzen.qbitcontroller.MOVE_TO_ADD_SERVER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        val moveToAddServer = intent.getBooleanExtra(Extras.MOVE_TO_ADD_SERVER, false)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.container, SettingsFragment())
            }

            if (moveToAddServer) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    val fragment = AddEditServerFragmentBuilder()
                        .build()
                    replace(R.id.container, fragment)
                    addToBackStack(null)
                }
            }
        }
    }
}
