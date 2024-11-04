package mwongela.songbook.info.errorcheck

import mwongela.songbook.info.UiResourceService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory

class LocalizedError(
    val messageRes: Int,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
) : RuntimeException() {
    private val uiResourceService by LazyExtractor(uiResourceService)

    override val message: String
        get() {
            return uiResourceService.resString(messageRes)
        }
}