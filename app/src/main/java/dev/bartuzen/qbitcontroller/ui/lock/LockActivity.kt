package dev.bartuzen.qbitcontroller.ui.lock

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityLockBinding
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.toPx

@AndroidEntryPoint
class LockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLockBinding

    private val viewModel: LockViewModel by viewModels()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        val digitButtons = listOf(
            binding.button0,
            binding.button1,
            binding.button2,
            binding.button3,
            binding.button4,
            binding.button5,
            binding.button6,
            binding.button7,
            binding.button8,
            binding.button9
        )

        digitButtons.forEachIndexed { digit, view ->
            view.setOnClickListener {
                viewModel.addDigit(digit)
                binding.textMessage.text = ""
            }
        }

        binding.buttonDelete.setOnClickListener {
            viewModel.deleteLastDigit()
        }

        binding.buttonDelete.setOnLongClickListener {
            viewModel.resetPin()
            true
        }

        binding.buttonConfirm.setOnClickListener {
            if (viewModel.isPinCorrect()) {
                finish()
            } else {
                binding.textMessage.setText(R.string.lock_wrong_pin)
                viewModel.resetPin()
            }
        }

        viewModel.pin.launchAndCollectLatestIn(this) { pin ->
            val size = 12.toPx(this@LockActivity)
            val margin = 8.toPx(this@LockActivity)

            binding.layoutIndicator.removeAllViews()
            repeat(pin.length) {
                val circle = View(this@LockActivity).apply {
                    setBackgroundResource(R.drawable.password_indicator)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(margin, 0, margin, 0)
                    }
                }
                binding.layoutIndicator.addView(circle)
            }
        }
    }
}
