package mwongela.songbook.songselection


import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import mwongela.songbook.R
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.logger.Logger
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.layout.navigation.NavigationMenuController
import mwongela.songbook.persistence.repository.AllSongsRepository
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.songselection.contextmenu.SongContextMenuBuilder
import mwongela.songbook.songselection.listview.SongListView
import mwongela.songbook.songselection.tree.SongTreeItem
import mwongela.songbook.songselection.tree.SongTreeSorter

abstract class SongSelectionLayoutController(
    layoutController: LazyInject<LayoutController> = appFactory.layoutController,
    appCompatActivity: LazyInject<AppCompatActivity> = appFactory.appCompatActivity,
    navigationMenuController: LazyInject<NavigationMenuController> = appFactory.navigationMenuController,
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    songContextMenuBuilder: LazyInject<SongContextMenuBuilder> = appFactory.songContextMenuBuilder,
    songOpener: LazyInject<SongOpener> = appFactory.songOpener,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
) : SongClickListener {
    protected val layoutController by LazyExtractor(layoutController)
    protected val activity by LazyExtractor(appCompatActivity)
    protected val songsRepository by LazyExtractor(songsRepository)
    protected val songContextMenuBuilder by LazyExtractor(songContextMenuBuilder)
    protected val songOpener by LazyExtractor(songOpener)
    protected val uiResourceService by LazyExtractor(uiResourceService)
    private val navigationMenuController by LazyExtractor(navigationMenuController)

    protected val logger: Logger = LoggerFactory.logger
    protected var actionBar: ActionBar? = null
    protected var itemsListView: SongListView? = null

    fun initSongSelectionLayout(layout: View) {
        // Toolbar
        val toolbar1 = layout.findViewById<Toolbar>(R.id.toolbar1)
        if (toolbar1 != null) {
            activity.setSupportActionBar(toolbar1)
            actionBar = activity.supportActionBar
            if (actionBar != null) {
                actionBar!!.setDisplayHomeAsUpEnabled(false)
                actionBar!!.setDisplayShowHomeEnabled(false)
            }
            // navigation menu button
            val navMenuButton = layout.findViewById<ImageButton>(R.id.navMenuButton)
            navMenuButton.setOnClickListener { navigationMenuController.navDrawerShow() }
        }

        itemsListView = layout.findViewById(R.id.itemsList)
    }

    open fun updateSongItemsList() {
        val items: MutableList<SongTreeItem> = getSongItems(songsRepository.allSongsRepo)
        val sortedItems = SongTreeSorter().sort(items)
        itemsListView!!.setItems(sortedItems)
    }

    open fun getSongItems(songsRepo: AllSongsRepository): MutableList<SongTreeItem> {
        return mutableListOf()
    }

    fun openSongPreview(item: SongTreeItem) {
        songOpener.openSongPreview(item.song!!)
    }

    override fun onSongItemClick(item: SongTreeItem) {
        if (item.isSong) {
            openSongPreview(item)
        }
    }

    override fun onSongItemLongClick(item: SongTreeItem) {
        if (item.isSong) {
            songContextMenuBuilder.showSongActions(item.song!!)
        } else {
            onSongItemClick(item)
        }
    }
}
