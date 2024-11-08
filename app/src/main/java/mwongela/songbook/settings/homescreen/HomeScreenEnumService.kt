package mwongela.songbook.settings.homescreen


import mwongela.songbook.info.UiResourceService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory

class HomeScreenEnumService(
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
) {
    private val uiResourceService by LazyExtractor(uiResourceService)

    fun homeScreenEnumsEntries(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        for (item in HomeScreenEnum.values()) {
            val displayName = uiResourceService.resString(item.nameResId)
            map[item.id.toString()] = displayName
        }
        return map
    }

}