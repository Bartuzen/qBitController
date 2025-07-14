package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import platform.Foundation.NSURL

actual fun isPlatformUrlValid(url: String) = NSURL.URLWithString(url) != null
