package mwongela.songbook.songselection.latest


import android.view.View
import android.widget.ImageButton
import mwongela.songbook.R
import mwongela.songbook.info.errorcheck.UiErrorHandler
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.InflatedLayout
import mwongela.songbook.persistence.general.SongsUpdater
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.settings.language.AppLanguageService
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.songselection.SongClickListener
import mwongela.songbook.songselection.contextmenu.SongContextMenuBuilder
import mwongela.songbook.songselection.listview.LazySongListView
import mwongela.songbook.songselection.listview.ListScrollPosition
import mwongela.songbook.songselection.search.SongSearchItem
import mwongela.songbook.songselection.tree.SongTreeItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class LatestSongsLayoutController(
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    songContextMenuBuilder: LazyInject<SongContextMenuBuilder> = appFactory.songContextMenuBuilder,
    songOpener: LazyInject<SongOpener> = appFactory.songOpener,
    appLanguageService: LazyInject<AppLanguageService> = appFactory.appLanguageService,
    songsUpdater: LazyInject<SongsUpdater> = appFactory.songsUpdater,
) : InflatedLayout(
    _layoutResourceId = R.layout.screen_latest_songs
), SongClickListener {
    private val songsRepository by LazyExtractor(songsRepository)
    private val songContextMenuBuilder by LazyExtractor(songContextMenuBuilder)
    private val songOpener by LazyExtractor(songOpener)
    private val appLanguageService by LazyExtractor(appLanguageService)
    private val songsUpdater by LazyExtractor(songsUpdater)

    private var itemsListView: LazySongListView? = null
    private var storedScroll: ListScrollPosition? = null
    private var subscriptions = mutableListOf<Disposable>()
    private val latestSongsCount = 200

    override fun showLayout(layout: View) {
        super.showLayout(layout)

        itemsListView = layout.findViewById<LazySongListView>(R.id.itemsList)?.also {
            it.init(activity, this, songContextMenuBuilder)
        }
        updateItemsList()

        layout.findViewById<ImageButton>(R.id.updateLatestSongs)?.let {
            it.setOnClickListener {
                songsUpdater.updateSongsDbAsync(forced = true)
            }
        }

        subscriptions.forEach { s -> s.dispose() }
        subscriptions.clear()
        subscriptions.add(songsRepository.dbChangeSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (isLayoutVisible())
                    updateItemsList()
            }, UiErrorHandler::handleError)
        )
    }

    private fun updateItemsList() {
        val acceptedLanguages = appLanguageService.selectedSongLanguages
        val acceptedLangCodes = acceptedLanguages.map { lang -> lang.langCode } + "" + null
        val latestSongs = songsRepository.publicSongsRepo.songs.get()
            .asSequence()
            .filter { it.isPublic() }
            .filter { song -> song.language in acceptedLangCodes }
            .sortedBy { song -> -song.updateTime }
            .take(latestSongsCount)
            .map { song -> SongSearchItem.song(song) }
            .toList()
        itemsListView?.setItems(latestSongs)

        if (storedScroll != null) {
            itemsListView?.restoreScrollPosition(storedScroll)
        }
    }

    override fun onSongItemClick(item: SongTreeItem) {
        storedScroll = itemsListView?.currentScrollPosition
        if (item.isSong) {
            songOpener.openSongPreview(item.song!!)
        }
    }

    override fun onSongItemLongClick(item: SongTreeItem) {
        if (item.isSong) {
            songContextMenuBuilder.showSongActions(item.song!!)
        }
    }
}
