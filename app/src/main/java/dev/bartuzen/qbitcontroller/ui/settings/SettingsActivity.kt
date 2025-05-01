package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivitySettingsBinding
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerFragment
import dev.bartuzen.qbitcontroller.utils.applySafeDrawingInsets
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    object Extras {
        const val MoveToAddServer = "dev.bartuzen.qbitcontroller.MOVE_TO_ADD_SERVER"

        const val EditServerId = "dev.bartuzen.qbitcontroller.EDIT_SERVER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        theme.applyStyle(R.style.ThemeOverlay_Material3_Preference, true)

        enableEdgeToEdge()
        binding.layoutAppBar.applySafeDrawingInsets(bottom = false)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        val moveToAddServer = intent.getBooleanExtra(Extras.MoveToAddServer, false)
        val editServerConfigId = intent.getIntExtra(Extras.EditServerId, -1)

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
            } else if (editServerConfigId != -1) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    val fragment = AddEditServerFragment(editServerConfigId)
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
