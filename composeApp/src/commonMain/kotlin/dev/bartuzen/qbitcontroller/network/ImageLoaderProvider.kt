package dev.bartuzen.qbitcontroller.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig

class ImageLoaderProvider(
    serverManager: ServerManager,
    private val requestManager: RequestManager,
) {

    private val imageLoaders = mutableMapOf<Int, ImageLoader>()

    init {
        serverManager.addServerListener(
            object : ServerManager.ServerListener {
                override fun onServerAddedListener(serverConfig: ServerConfig) {}

                override fun onServerRemovedListener(serverConfig: ServerConfig) {
                    imageLoaders.remove(serverConfig.id)
                }

                override fun onServerChangedListener(serverConfig: ServerConfig) {
                    imageLoaders.remove(serverConfig.id)
                }
            },
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
