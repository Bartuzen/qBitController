package dev.bartuzen.qbitcontroller.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.bartuzen.qbitcontroller.data.ServerManager

class ImageLoaderProvider(
    serverManager: ServerManager,
    private val requestManager: RequestManager,
) {

    private val imageLoaders = mutableMapOf<Int, ImageLoader>()

    init {
        serverManager.addServerListener(
            remove = { imageLoaders.remove(it.id) },
            change = { imageLoaders.remove(it.id) },
        )
    }

    @Composable
    fun getImageLoader(serverId: Int): ImageLoader {
        val platformContext = LocalPlatformContext.current
        return remember {
            imageLoaders.getOrPut(serverId) {
                ImageLoader.Builder(platformContext)
                    .components {
                        add(
                            KtorNetworkFetcherFactory(
                                httpClient = requestManager.getHttpClient(serverId),
                            ),
                        )
                    }.build()
            }
        }
    }
}
