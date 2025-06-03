package dev.bartuzen.qbitcontroller.ui.settings.appearance

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.bartuzen.qbitcontroller.utils.stringResource
import me.zhanghai.compose.preference.Preference
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.settings_language

@Composable
actual fun LanguagePreference() {
    Preference(
        title = { Text(text = stringResource(Res.string.settings_language)) },
        onClick = {
            val url = NSURL(string = UIApplicationOpenSettingsURLString)
            if (UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
            }
        },
    )
}
