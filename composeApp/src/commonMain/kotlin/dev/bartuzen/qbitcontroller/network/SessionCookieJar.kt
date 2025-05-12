package dev.bartuzen.qbitcontroller.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {
    private var cookies: List<Cookie>? = null

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies = cookies
    }

    override fun loadForRequest(url: HttpUrl) = if (!url.encodedPath.endsWith("login")) {
        cookies ?: emptyList()
    } else {
        emptyList()
    }
}
