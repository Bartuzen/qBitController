package dev.bartuzen.baselineprofile

import android.os.Build
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")!!,
            includeInStartupProfile = true
        ) {
            pressHome()
            startActivityAndWait()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.executeShellCommand(
                    "pm grant $packageName android.permission.POST_NOTIFICATIONS"
                ).close()
            }

            val addServer = device.findObject(By.text("No servers have been found. Click here to add one.")) != null
            if (addServer) {
                device.findObject(By.res("overflow")).click()
                device.findObject(By.text("Settings")).clickAndWait(Until.newWindow(), 5000)
                device.findObject(By.text("Add new server")).clickAndWait(Until.newWindow(), 5000)

                device.findObject(By.res(packageName,"edit_host")).text = "192.168.1.20"
                device.findObject(By.res(packageName,"edit_port")).text = "8080"
                device.findObject(By.res(packageName,"edit_username")).text = "admin"
                device.findObject(By.res(packageName,"edit_password")).text = "123456"
                device.findObject(By.res(packageName, "menu_save")).click()
                device.pressBack()
            }

            device.wait(Until.hasObject(By.res("torrent_0")), 5000)
            device.findObject(By.res("torrent_0")).clickAndWait(Until.newWindow(), 5000)
            device.findObject(By.text("Files")).click()
            device.findObject(By.text("Pieces")).click()
            device.findObject(By.text("Trackers")).click()
            device.findObject(By.text("Peers")).click()
            device.findObject(By.text("HTTP Sources")).click()
            device.pressBack()
            device.waitForWindowUpdate(null, 5000)

            device.findObject(By.desc("Add Torrent")).click()
            device.pressBack()
            device.waitForWindowUpdate(null, 5000)

            device.findObject(By.res("overflow")).click()
            device.findObject(By.text("RSS")).click()
            device.pressBack()
            device.waitForWindowUpdate(null, 5000)

            device.findObject(By.res("overflow")).click()
            device.findObject(By.text("Search Online")).click()
            device.pressBack()
            device.waitForWindowUpdate(null, 5000)

            device.findObject(By.res("overflow")).click()
            device.findObject(By.text("Execution Log")).click()
            device.pressBack()
            device.waitForWindowUpdate(null, 5000)
        }
    }
}
