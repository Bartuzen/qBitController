package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivitySettingsBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerFragment
import dev.bartuzen.qbitcontroller.utils.getParcelable
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    object Extras {
        const val MOVE_TO_ADD_SERVER = "dev.bartuzen.qbitcontroller.MOVE_TO_ADD_SERVER"

        const val EDIT_SERVER_CONFIG = "dev.bartuzen.qbitcontroller.EDIT_SERVER_CONFIG"
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
        val editServerConfig = intent.getParcelable<ServerConfig>(Extras.EDIT_SERVER_CONFIG)

        if (savedInstanceState == null) {
            if (moveToAddServer) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.container, SettingsFragment())
                }

                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    setDefaultAnimations()
                    val fragment = AddEditServerFragment()
                    replace(R.id.container, fragment)
                    addToBackStack(null)
                }
            } else if (editServerConfig != null) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    val fragment = AddEditServerFragment(editServerConfig)
                    replace(R.id.container, fragment)
                }
            } else {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.container, SettingsFragment())
                }
            }
        }
    }
}
