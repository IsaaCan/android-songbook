package mwongela.songbook.custom


import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import mwongela.songbook.R
import mwongela.songbook.custom.list.CustomSongListItem
import mwongela.songbook.custom.list.CustomSongListView
import mwongela.songbook.custom.sync.EditorSessionService
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.errorcheck.UiErrorHandler
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.InflatedLayout
import mwongela.songbook.layout.LocalFocusTraverser
import mwongela.songbook.layout.contextmenu.ContextMenuBuilder
import mwongela.songbook.layout.dialog.ConfirmDialogBuilder
import mwongela.songbook.layout.list.ListItemClickListener
import mwongela.songbook.layout.spinner.SinglePicker
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.persistence.user.custom.CustomCategory
import mwongela.songbook.persistence.user.custom.CustomSongsDb
import mwongela.songbook.settings.enums.CustomSongsOrdering
import mwongela.songbook.settings.enums.SettingsEnumService
import mwongela.songbook.settings.language.AppLanguageService
import mwongela.songbook.settings.preferences.PreferencesState
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.songselection.contextmenu.SongContextMenuBuilder
import mwongela.songbook.songselection.listview.ListScrollPosition
import mwongela.songbook.songselection.search.SongSearchFilter
import mwongela.songbook.songselection.search.sortSongsByFilterRelevance
import mwongela.songbook.songselection.tree.NoParentItemException
import mwongela.songbook.system.SoftKeyboardService
import mwongela.songbook.system.locale.InsensitiveNameComparator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class CustomSongsListLayoutController(
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    songContextMenuBuilder: LazyInject<SongContextMenuBuilder> = appFactory.songContextMenuBuilder,
    songOpener: LazyInject<SongOpener> = appFactory.songOpener,
    customSongService: LazyInject<CustomSongService> = appFactory.customSongService,
    appLanguageService: LazyInject<AppLanguageService> = appFactory.appLanguageService,
    exportFileChooser: LazyInject<ExportFileChooser> = appFactory.exportFileChooser,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    importFileChooser: LazyInject<ImportFileChooser> = appFactory.importFileChooser,
    songImportFileChooser: LazyInject<SongImportFileChooser> = appFactory.songImportFileChooser,
    settingsEnumService: LazyInject<SettingsEnumService> = appFactory.settingsEnumService,
    softKeyboardService: LazyInject<SoftKeyboardService> = appFactory.softKeyboardService,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
    editorSessionService: LazyInject<EditorSessionService> = appFactory.editorSessionService,
) : InflatedLayout(
    _layoutResourceId = R.layout.screen_custom_songs
), ListItemClickListener<CustomSongListItem> {
    private val songsRepository by LazyExtractor(songsRepository)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val songContextMenuBuilder by LazyExtractor(songContextMenuBuilder)
    private val songOpener by LazyExtractor(songOpener)
    private val customSongService by LazyExtractor(customSongService)
    private val appLanguageService by LazyExtractor(appLanguageService)
    private val songExportFileChooser by LazyExtractor(exportFileChooser)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val importFileChooser by LazyExtractor(importFileChooser)
    private val songImportFileChooser by LazyExtractor(songImportFileChooser)
    private val settingsEnumService by LazyExtractor(settingsEnumService)
    private val softKeyboardService by LazyExtractor(softKeyboardService)
    private val preferencesState by LazyExtractor(preferencesState)
    private val editorSessionService by LazyExtractor(editorSessionService)

    private var itemsListView: CustomSongListView? = null
    private var goBackButton: ImageButton? = null
    private var storedScroll: ListScrollPosition? = null
    private var tabTitleLabel: TextView? = null
    private var emptyListLabel: TextView? = null
    private var customSearchBarLayout: FrameLayout? = null
    private var searchFilterEdit: EditText? = null
    private var searchFilterClearButton: ImageButton? = null
    private var searchSongButton: ImageButton? = null
    private var moreActionsButton: ImageButton? = null
    private var songsSortButton: ImageButton? = null

    private var customCategory: CustomCategory? = null
    private var sortPicker: SinglePicker<CustomSongsOrdering>? = null
    private var searchingOn: Boolean = false
    private var itemNameFilter: String? = null
    private var searchFilterSubject: PublishSubject<String> = PublishSubject.create()
    private var subscriptions = mutableListOf<Disposable>()

    override fun showLayout(layout: View) {
        super.showLayout(layout)

        tabTitleLabel = layout.findViewById(R.id.tabTitleLabel)
        emptyListLabel = layout.findViewById(R.id.emptyListLabel)
        customSearchBarLayout = layout.findViewById(R.id.customSearchBarLayout)

        searchFilterEdit = layout.findViewById<EditText>(R.id.searchFilterEdit)?.apply {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchFilterSubject.onNext(s.toString())
                }
            })

            if (isFilterSet()) {
                setText(itemNameFilter, TextView.BufferType.EDITABLE)
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    softKeyboardService.hideSoftKeyboard(this)
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    clearFocus()
                    softKeyboardService.hideSoftKeyboard(this)
                    return@setOnEditorActionListener true
                }
                false
            }
        }

        moreActionsButton = layout.findViewById<ImageButton>(R.id.moreActionsButton)?.also {
            it.setOnClickListener {
                showMoreActions()
            }
        }

        goBackButton = layout.findViewById<ImageButton>(R.id.goBackButton)?.also {
            it.setOnClickListener { goUp() }
        }

        songsSortButton = layout.findViewById<ImageButton>(R.id.songsSortButton)?.apply {
            val title = uiResourceService.resString(R.string.song_list_ordering)
            sortPicker = SinglePicker(
                activity,
                entityNames = settingsEnumService.customSongsOrderingEnumEntries(),
                selected = settingsEnumService.preferencesState.customSongsOrdering,
                title = title,
            ) { selectedSorting ->
                if (settingsEnumService.preferencesState.customSongsOrdering != selectedSorting) {
                    settingsEnumService.preferencesState.customSongsOrdering = selectedSorting
                    updateHeader()
                    updateItemsList()
                }
            }
            setOnClickListener {
                sortPicker?.showChoiceDialog()
            }
        }

        searchSongButton = layout.findViewById<ImageButton>(R.id.searchSongButton)?.also {
            it.setOnClickListener {
                searchingOn = true
                updateHeader()
                updateItemsList()
                searchFilterEdit?.requestFocus()
                Handler(Looper.getMainLooper()).post {
                    softKeyboardService.showSoftKeyboard(searchFilterEdit)
                }
            }
        }

        searchFilterClearButton =
            layout.findViewById<ImageButton>(R.id.searchFilterClearButton)?.also {
                it.setOnClickListener {
                    onClearFilterClicked()
                }
            }

        itemsListView = layout.findViewById(R.id.itemsListView)
        itemsListView!!.init(activity as Context, this)
        updateHeader()
        updateItemsList()

        val localFocus = LocalFocusTraverser(
            currentViewGetter = { itemsListView?.selectedView },
            currentFocusGetter = { appFactory.activity.get().currentFocus?.id },
            preNextFocus = { _: Int, _: View ->
                when {
                    appFactory.navigationMenuController.get().isDrawerShown() -> R.id.nav_view
                    else -> 0
                }
            },
            nextLeft = { currentFocusId: Int, currentView: View ->
                when (currentFocusId) {
                    R.id.itemSongMoreButton, R.id.itemsListView -> {
                        (currentView as ViewGroup).descendantFocusability =
                            ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        itemsListView?.requestFocusFromTouch()
                    }
                }
                when {
                    currentFocusId == R.id.itemSongMoreButton -> -1
                    currentFocusId == R.id.itemsListView && customCategory != null -> R.id.goBackButton
                    currentFocusId == R.id.itemsListView && customCategory == null -> R.id.navMenuButton
                    else -> 0
                }
            },
            nextRight = { currentFocusId: Int, currentView: View ->
                when {
                    currentFocusId == R.id.itemsListView && currentView.findViewById<View>(R.id.itemSongMoreButton)?.isVisible == true -> {
                        (currentView as? ViewGroup)?.descendantFocusability =
                            ViewGroup.FOCUS_BEFORE_DESCENDANTS
                        R.id.itemSongMoreButton
                    }
                    else -> 0
                }
            },
            nextUp = { currentFocusId: Int, currentView: View ->
                when (currentFocusId) {
                    R.id.itemSongMoreButton, R.id.itemsListView -> {
                        (currentView as ViewGroup).descendantFocusability =
                            ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        itemsListView?.requestFocusFromTouch()
                    }
                }
                when {
                    currentFocusId == R.id.itemSongMoreButton -> -1
                    itemsListView?.selectedItemPosition == 0 -> {
                        when {
                            customCategory != null -> R.id.goBackButton
                            else -> R.id.navMenuButton
                        }
                    }
                    else -> 0
                }
            },
            nextDown = { currentFocusId: Int, currentView: View ->
                when (currentFocusId) {
                    R.id.itemSongMoreButton, R.id.itemsListView -> {
                        (currentView as ViewGroup).descendantFocusability =
                            ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        itemsListView?.requestFocusFromTouch()
                    }
                }
                0
            },
        )
        itemsListView?.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (localFocus.handleKey(keyCode))
                    return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        subscriptions.forEach { s -> s.dispose() }
        subscriptions.clear()
        subscriptions.add(searchFilterSubject
            .debounce(400, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                setSongFilter(searchFilterEdit?.text?.toString())
            }, UiErrorHandler::handleError)
        )
        subscriptions.add(songsRepository.dbChangeSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (isLayoutVisible())
                    updateItemsList()
            }, UiErrorHandler::handleError)
        )
    }

    private fun isFilterSet(): Boolean {
        if (itemNameFilter.isNullOrEmpty())
            return false
        return (itemNameFilter?.length ?: 0) >= 3
    }

    private fun setSongFilter(itemNameFilter: String?) {
        this.itemNameFilter = itemNameFilter
        if (itemNameFilter == null)
            searchFilterEdit?.setText("", TextView.BufferType.EDITABLE)
        storedScroll = null
        updateItemsList()
    }

    private fun onClearFilterClicked() {
        if (!itemNameFilter.isNullOrEmpty()) {
            setSongFilter(null)
        } else {
            softKeyboardService.hideSoftKeyboard(searchFilterEdit)
            searchingOn = false
            updateItemsList()
        }
        updateHeader()
    }

    private fun showMoreActions() {
        ContextMenuBuilder().showContextMenu(
            mutableListOf(
                ContextMenuBuilder.Action(R.string.action_create_new_custom_song) {
                    addCustomSong()
                },
                ContextMenuBuilder.Action(R.string.custom_songs_synchornize) {
                    editorSessionService.synchronizeWithWeb()
                },
                ContextMenuBuilder.Action(R.string.import_content_from_file) {
                    importOneSong()
                },
                ContextMenuBuilder.Action(R.string.edit_song_import_custom_songs) {
                    importCustomSongs()
                },
                ContextMenuBuilder.Action(R.string.edit_song_export_all_custom_songs) {
                    exportAllCustomSongs()
                },
            )
        )
    }

    private fun exportAllCustomSongs() {
        val json = Json {
            ignoreUnknownKeys = true
            allowStructuredMapKeys = true
            prettyPrint = false
            useArrayPolymorphism = false
        }
        val exportSongsDb = CustomSongsDb(songs = songsRepository.customSongsDao.customSongs.songs)
        val content = json.encodeToString(CustomSongsDb.serializer(), exportSongsDb)
        songExportFileChooser.showFileChooser(content, "customsongs.json") {
            uiInfoService.showInfo(R.string.custom_songs_exported)
        }
    }

    private fun importOneSong() {
        customSongService.showAddSongScreen()
        songImportFileChooser.showFileChooser()
    }

    private fun importCustomSongs() {
        ConfirmDialogBuilder().confirmAction(uiInfoService.resString(R.string.custom_songs_mass_import_hint)) {
            importFileChooser.importFile(sizeLimit = 10 * 1024 * 1024) { content: String, _ ->
                val json = Json {
                    ignoreUnknownKeys = true
                    allowStructuredMapKeys = true
                    prettyPrint = false
                    useArrayPolymorphism = false
                }

                val importedCustomSongsDb =
                    json.decodeFromString(CustomSongsDb.serializer(), content)
                val added =
                    songsRepository.customSongsDao.addNewCustomSongs(importedCustomSongsDb.songs)
                uiInfoService.showInfo(R.string.custom_songs_imported, added.toString())
            }
        }
    }

    private fun addCustomSong() {
        customSongService.showAddSongScreen()
    }

    private fun updateHeader() {
        val groupingEnabled =
            settingsEnumService.preferencesState.customSongsOrdering == CustomSongsOrdering.GROUP_CATEGORIES
        if (!groupingEnabled)
            customCategory = null

        tabTitleLabel?.text = when {
            customCategory != null && groupingEnabled -> customCategory?.name.orEmpty()
            else -> uiResourceService.resString(R.string.nav_custom_song)
        }

        goBackButton?.visibility = when {
            customCategory != null && groupingEnabled -> View.VISIBLE
            else -> View.GONE
        }

        customSearchBarLayout?.visibility = when {
            searchingOn -> View.VISIBLE
            else -> View.GONE
        }
        tabTitleLabel?.visibility = when {
            !searchingOn -> View.VISIBLE
            else -> View.GONE
        }
        songsSortButton?.visibility = when {
            !searchingOn -> View.VISIBLE
            else -> View.GONE
        }
        searchSongButton?.visibility = when {
            !searchingOn -> View.VISIBLE
            else -> View.GONE
        }
        moreActionsButton?.visibility = when {
            !searchingOn -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun updateItemsList() {
        val groupingEnabled =
            settingsEnumService.preferencesState.customSongsOrdering == CustomSongsOrdering.GROUP_CATEGORIES
        if (!groupingEnabled)
            customCategory = null
        val filtering = isFilterSet()

        val songItems: List<CustomSongListItem> = when {

            filtering -> {
                val songFilter =
                    SongSearchFilter(itemNameFilter.orEmpty(), preferencesState.songLyricsSearch)
                songsRepository.customSongsRepo.songs.get()
                    .filter { song -> songFilter.matchSong(song) }
                    .sortSongsByFilterRelevance(songFilter)
                    .map { CustomSongListItem(song = it) }
            }

            groupingEnabled && customCategory == null -> {
                val locale = appLanguageService.getCurrentLocale()
                val categoryNameComparator =
                    InsensitiveNameComparator<CustomCategory>(locale) { category -> category.name }
                val categories = songsRepository.customSongsDao.customCategories
                    .sortedWith(categoryNameComparator)
                    .map { CustomSongListItem(customCategory = it) }
                val uncategorized = songsRepository.customSongsRepo.uncategorizedSongs.get()
                    .sortSongs()
                    .map { CustomSongListItem(song = it) }
                categories + uncategorized
            }

            groupingEnabled && customCategory != null -> {
                customCategory!!.songs
                    .sortSongs()
                    .map { CustomSongListItem(song = it) }
            }

            else -> {
                songsRepository.customSongsRepo.songs.get()
                    .sortSongs()
                    .map { CustomSongListItem(song = it) }
            }

        }
        itemsListView?.items = songItems.toList()

        if (storedScroll != null) {
            Handler(Looper.getMainLooper()).post {
                itemsListView?.restoreScrollPosition(storedScroll)
            }
        }

        emptyListLabel?.visibility = when (itemsListView?.count) {
            0 -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun Iterable<Song>.sortSongs(): List<Song> {
        val locale = appLanguageService.getCurrentLocale()
        return when (settingsEnumService.preferencesState.customSongsOrdering) {
            CustomSongsOrdering.SORT_BY_TITLE, CustomSongsOrdering.GROUP_CATEGORIES -> {
                this.sortedBy { song -> song.displayName().lowercase(locale) }
            }
            CustomSongsOrdering.SORT_BY_ARTIST -> {
                this.sortedWith(
                    compareBy<Song> { song -> song.displayCategories().isEmpty() }
                        .thenBy { song -> song.displayCategories() }
                        .thenBy { song -> song.displayName().lowercase(locale) }
                )
            }
            CustomSongsOrdering.SORT_BY_LATEST -> {
                this.sortedWith(
                    compareBy(
                        { song -> -song.updateTime },
                        { song -> song.displayName().lowercase(locale) },
                    )
                )
            }
            CustomSongsOrdering.SORT_BY_OLDEST -> {
                this.sortedWith(
                    compareBy(
                        { song -> song.updateTime },
                        { song -> song.displayName().lowercase(locale) },
                    )
                )
            }
        }
    }

    override fun onItemClick(item: CustomSongListItem) {
        storedScroll = itemsListView?.currentScrollPosition
        if (item.song != null) {
            songOpener.openSongPreview(item.song)
        } else if (item.customCategory != null) {
            customCategory = item.customCategory
            updateHeader()
            updateItemsList()
        }
    }

    override fun onBackClicked() {
        if (searchingOn) {
            onClearFilterClicked()
            return
        }
        goUp()
    }

    private fun goUp() {
        try {
            if (customCategory == null)
                throw NoParentItemException()
            customCategory = null
            updateHeader()
            updateItemsList()
        } catch (e: NoParentItemException) {
            layoutController.showPreviousLayoutOrQuit()
        }
    }

    override fun onItemLongClick(item: CustomSongListItem) {
        onMoreActions(item)
    }

    override fun onMoreActions(item: CustomSongListItem) {
        item.song?.let {
            songContextMenuBuilder.showSongActions(it)
        }
    }

}
