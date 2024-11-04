package mwongela.songbook.persistence

import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.settings.preferences.PreferencesState
import java.util.UUID

class DeviceIdProvider (
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
) {
    private val preferencesState by LazyExtractor(preferencesState)

    fun getDeviceId(): String {
        if (preferencesState.deviceId.isBlank()) {
            val uuid = newUUID()
            preferencesState.deviceId = uuid
            LoggerFactory.logger.debug("Device UUID assigned: $uuid")
        }
        return preferencesState.deviceId
    }

    fun newUUID(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
}
