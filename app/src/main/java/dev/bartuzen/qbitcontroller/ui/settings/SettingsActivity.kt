package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivitySettingsBinding

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setTitle(R.string.settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SettingsFragment())
                .commit()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.settingsActivityEvent.collect { event ->
                when (event) {
                    is SettingsViewModel.SettingsActivityEvent.MovePage -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, event.fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    is SettingsViewModel.SettingsActivityEvent.AddEditServerCompleted -> {
                        supportFragmentManager.popBackStack()
                    }
                }
            }
        }
    }
}