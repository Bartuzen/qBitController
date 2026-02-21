package dev.bartuzen.qbitcontroller.di

import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.notification.TorrentDownloadedNotifier
import dev.bartuzen.qbitcontroller.data.repositories.AddTorrentRepository
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.data.repositories.log.LogRepository
import dev.bartuzen.qbitcontroller.data.repositories.rss.EditRssRuleRepository
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssArticlesRepository
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssFeedRepository
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssRulesRepository
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchPluginsRepository
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchResultRepository
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchStartRepository
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentFilesRepository
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentOverviewRepository
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentPeersRepository
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentTrackersRepository
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentWebSeedsRepository
import dev.bartuzen.qbitcontroller.network.ImageLoaderProvider
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.UpdateChecker
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentViewModel
import dev.bartuzen.qbitcontroller.ui.log.LogViewModel
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesViewModel
import dev.bartuzen.qbitcontroller.ui.rss.editrule.EditRssRuleViewModel
import dev.bartuzen.qbitcontroller.ui.rss.feeds.RssFeedsViewModel
import dev.bartuzen.qbitcontroller.ui.rss.rules.RssRulesViewModel
import dev.bartuzen.qbitcontroller.ui.search.plugins.SearchPluginsViewModel
import dev.bartuzen.qbitcontroller.ui.search.result.SearchResultViewModel
import dev.bartuzen.qbitcontroller.ui.search.start.SearchStartViewModel
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerViewModel
import dev.bartuzen.qbitcontroller.ui.settings.appearance.AppearanceSettingsViewModel
import dev.bartuzen.qbitcontroller.ui.settings.general.GeneralSettingsViewModel
import dev.bartuzen.qbitcontroller.ui.settings.network.NetworkSettingsViewModel
import dev.bartuzen.qbitcontroller.ui.settings.servers.ServersViewModel
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFilesViewModel
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewViewModel
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers.TorrentPeersViewModel
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers.TorrentTrackersViewModel
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds.TorrentWebSeedsViewModel
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    includes(platformModule)

    singleOf(::RequestManager)
    single { SettingsManager(get(named("settings"))) }
    single { ServerManager(get(named("servers"))) }
    singleOf(::TorrentDownloadedNotifier)
    single { ConfigMigrator(get(named("settings")), get(named("servers"))) }
    singleOf(::ImageLoaderProvider)
    singleOf(::UpdateChecker)

    singleOf(::TorrentListRepository)

    singleOf(::TorrentOverviewRepository)
    singleOf(::TorrentFilesRepository)
    singleOf(::TorrentTrackersRepository)
    singleOf(::TorrentPeersRepository)
    singleOf(::TorrentWebSeedsRepository)

    singleOf(::AddTorrentRepository)

    singleOf(::RssFeedRepository)
    singleOf(::RssArticlesRepository)
    singleOf(::RssRulesRepository)
    singleOf(::EditRssRuleRepository)

    singleOf(::SearchStartRepository)
    singleOf(::SearchResultRepository)
    singleOf(::SearchPluginsRepository)

    singleOf(::LogRepository)

    viewModelOf(::TorrentListViewModel)

    viewModel { (serverId: Int, hash: String) -> TorrentOverviewViewModel(serverId, hash, get(), get(), get()) }
    viewModel { (serverId: Int, hash: String) -> TorrentFilesViewModel(serverId, hash, get(), get()) }
    viewModel { (serverId: Int, hash: String) -> TorrentTrackersViewModel(serverId, hash, get(), get()) }
    viewModel { (serverId: Int, hash: String) -> TorrentPeersViewModel(serverId, hash, get(), get(), get()) }
    viewModel { (serverId: Int, hash: String) -> TorrentWebSeedsViewModel(serverId, hash, get(), get()) }

    viewModel { (initialServerId: Int?) -> AddTorrentViewModel(initialServerId, get(), get(), get(), get()) }

    viewModel { (serverId: Int) -> RssFeedsViewModel(serverId, get()) }
    viewModel { (serverId: Int, feedPath: List<String>, uid: String?) ->
        RssArticlesViewModel(serverId, feedPath, uid, get(), get())
    }
    viewModel { (serverId: Int) -> RssRulesViewModel(serverId, get()) }
    viewModel { (serverId: Int, ruleName: String) -> EditRssRuleViewModel(serverId, ruleName, get(), get()) }

    viewModel { (serverId: Int) -> SearchStartViewModel(serverId, get()) }
    viewModel { (serverId: Int, searchQuery: String, category: String, plugins: String) ->
        SearchResultViewModel(serverId, searchQuery, category, plugins, get(), get(), get())
    }
    viewModel { (serverId: Int) -> SearchPluginsViewModel(serverId, get()) }

    viewModel { (serverId: Int) -> LogViewModel(serverId, get()) }

    viewModelOf(::ServersViewModel)
    viewModel { (serverId: Int?) -> AddEditServerViewModel(serverId, get(), get()) }
    viewModelOf(::GeneralSettingsViewModel)
    viewModelOf(::AppearanceSettingsViewModel)
    viewModelOf(::NetworkSettingsViewModel)
}

expect val platformModule: Module
