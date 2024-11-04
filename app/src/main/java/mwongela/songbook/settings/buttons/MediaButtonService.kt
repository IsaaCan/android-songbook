package mwongela.songbook.settings.buttons


import mwongela.songbook.info.UiResourceService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory

class MediaButtonService(
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
) {
    private val uiResourceService by LazyExtractor(uiResourceService)

    fun mediaButtonBehavioursEntries(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        for (item in MediaButtonBehaviours.values()) {
            val displayName = uiResourceService.resString(item.nameResId)
            map[item.id.toString()] = displayName
        }
        return map
    }

}