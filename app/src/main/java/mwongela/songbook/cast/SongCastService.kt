package mwongela.songbook.cast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mwongela.songbook.R
import mwongela.songbook.custom.sync.SongHasher
import mwongela.songbook.info.errorcheck.UiErrorHandler
import mwongela.songbook.info.logger.Logger
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.appFactory
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.general.model.SongNamespace
import mwongela.songbook.persistence.general.model.SongStatus
import mwongela.songbook.settings.chordsnotation.ChordsNotation
import mwongela.songbook.songpreview.SongPreviewLayoutController
import mwongela.songbook.util.buildSongName
import mwongela.songbook.util.defaultScope
import mwongela.songbook.util.interpolate
import mwongela.songbook.util.ioScope
import mwongela.songbook.util.mainScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.Date


class SongCastService {
    private val uiInfoService by LazyExtractor(appFactory.uiInfoService)
    private val layoutController by LazyExtractor(appFactory.layoutController)
    private val activityController by LazyExtractor(appFactory.activityController)
    private val songPreviewLayoutController by LazyExtractor(appFactory.songPreviewLayoutController)
    private val songOpener by LazyExtractor(appFactory.songOpener)
    private val scrollService by LazyExtractor(appFactory.scrollService)

    private val logger: Logger = LoggerFactory.logger
    private val requester = SongCastRequester()
    private var streamSocket = StreamSocket(::onEventBroadcast)
    private var myName: String = ""
    var myMemberPublicId: String = ""
    var sessionCode: String? = null
    private var ephemeralSong: Song? = null
    var onSessionUpdated: () -> Unit = {}
    var sessionState: SessionState = SessionState()
    private var oldState: SessionState = SessionState()
    private var periodicRefreshJob: Job? = null
    private var periodicReconnectJob: Job? = null
    private var lastSessionChange: Long = 0 // millis
    private var joinTimestamp: Long = 0
    var clientFollowScroll: Boolean by mutableStateOf(true)
    var clientOpenPresentedSongs: Boolean by mutableStateOf(true)
    var lastSharedScroll: CastScroll? = null
    private val logEvents: MutableList<LogEvent> = mutableListOf()
    private val sessionStateMutex = Mutex()

    val presenters: List<CastMember> get() = sessionState.members.filter { it.type == CastMemberType.OWNER.value }
    val spectators: List<CastMember> get() = sessionState.members.filter { it.type == CastMemberType.GUEST.value }

    fun isInRoom(): Boolean = sessionCode != null

    fun isPresenter(): Boolean = isInRoom() && presenters.any { it.public_member_id == myMemberPublicId }

    fun isPresenting(): Boolean = isPresenter() && sessionState.castSongDto?.chosen_by == myMemberPublicId

    fun isSongSelected(): Boolean = sessionState.castSongDto != null

    fun createSessionAsync(memberName: String): Deferred<Result<CastSessionJoined>> {
        return requester.createSessionAsync(memberName) { responseData ->
            initRoom(responseData)
        }
    }

    fun joinSessionAsync(sessionCode: String, memberName: String): Deferred<Result<CastSessionJoined>> {
        return requester.joinSessionAsync(sessionCode, memberName) { responseData ->
            initRoom(responseData)
        }
    }

    fun restoreSessionAsync(): Deferred<Result<CastSessionJoined>> {
        return requester.restoreSessionAsync { responseData ->
            initRoom(responseData)
        }
    }

    private suspend fun initRoom(responseData: CastSessionJoined) {
        sessionCode = responseData.short_id
        requester.sessionCode = responseData.short_id
        myName = responseData.member_name
        myMemberPublicId = responseData.public_member_id
        joinTimestamp = Date().time / 1000

        logEvents.clear()
        sessionStateMutex.withLock {
            sessionState.initialized = false
            sessionState.members = listOf()
            sessionState.castSongDto = null
            sessionState.currentScroll = null
            sessionState.chatMessages = listOf()
            sessionState.createdTime = 0
            ephemeralSong = null
        }
        streamSocket.close()
        streamSocket.connect(responseData.short_id)
        lastSessionChange = Date().time
        lastSharedScroll = null

        val memberJoinedResId = when (isPresenter()) {
            true -> R.string.songcast_joined_the_room_as_presenter
            false -> R.string.songcast_joined_the_room_as_spectator
        }
        addSystemLogEvent(memberJoinedResId, responseData.member_name)
        defaultScope.launch {
            refreshSessionDetails()
            logEvents.add(
                SystemLogEvent(
                    timestampMs = sessionState.createdTime * 1000,
                    text = uiInfoService.resString(R.string.songcast_session_created),
                )
            )
            run {
                val ephemeralSongN = ephemeralSong ?: return@run
                val castSongDtoN = sessionState.castSongDto ?: return@run
                val presenter: CastMember? =
                    sessionState.members.find { it.public_member_id == castSongDtoN.chosen_by }
                val presenterName = presenter?.name ?: "Unknown"
                logEvents.add(
                    SongLogEvent(
                        timestampMs = Date().time,
                        author = presenterName,
                        song = ephemeralSongN,
                    )
                )
            }
            refreshUI()
        }
        if (periodicRefreshJob?.isActive == true)
            periodicRefreshJob?.cancel()
        if (periodicReconnectJob?.isActive == true)
            periodicReconnectJob?.cancel()
        periodicRefreshJob = ioScope.launch {
            try {
                periodicRefresh()
            } catch (e: CancellationException) {
                logger.debug("Periodic refresh coroutine cancelled")
            }
        }
        periodicReconnectJob = ioScope.launch {
            try {
                periodicReconnect()
            } catch (e: CancellationException) {
                logger.debug("Periodic reconnect coroutine cancelled")
            }
        }
    }

    private fun exitRoom() {
        sessionCode = null
        requester.sessionCode = ""
        sessionState.initialized = false
        sessionState.members = listOf()
        sessionState.castSongDto = null
        sessionState.currentScroll = null
        sessionState.chatMessages = listOf()
        ephemeralSong = null
        streamSocket.close()
        periodicRefreshJob = null
        periodicReconnectJob = null
        lastSessionChange = 0
        lastSharedScroll = null
        logEvents.clear()
        if (periodicRefreshJob?.isActive == true)
            periodicRefreshJob?.cancel()
        if (periodicReconnectJob?.isActive == true)
            periodicReconnectJob?.cancel()
    }

    private suspend fun periodicRefresh() {
        var lastShot: Long = Date().time
        while (isInRoom()) {
            val interval: Long? = when {
                !activityController.isForeground -> null
                lastSessionChange == 0L -> 0
                streamSocket.ioSocket?.connected() == false -> (2000..3000).random().toLong()
                else -> {
                    val millis = Date().time - lastSessionChange
                    val fraction = millis.interpolate(0, 10 * 60_000) // 0-10 min -> 0-1
                    val penalty = (fraction * 2 * 60_000).toLong() // 0-1 -> 0-2 min
                    (2_000..3_000).random().toLong() + penalty
                }
            }
            if (interval != null && Date().time - lastShot >= interval) {
                val waitedS = (Date().time - lastShot) / 1000

                try {
                    logger.debug("refreshing SongCast session, waited ${waitedS}s...")
                    refreshSessionDetails().onFailure {
                        throw it
                    }
                } catch (e: Throwable) {
                    UiErrorHandler().handleContextError(e, R.string.songcast_connection_context)
                    delay(5_000)
                }

                lastShot = Date().time
            }
            delay(1_000)
        }
    }

    private suspend fun periodicReconnect() {
        var lastShot: Long = Date().time
        while (isInRoom()) {
            val interval: Long? = when {
                !activityController.isForeground -> null
                !streamSocket.initialized -> null
                streamSocket.ioSocket?.connected() == false -> 0
                else -> null
            }
            if (interval != null && Date().time - lastShot >= interval) {
                try {
                    uiInfoService.showInfoAction(
                        R.string.songcast_reconnecting_to_room,
                        actionResId = R.string.songcast_action_lobby,
                        action = { showLobby() },
                    )
                    streamSocket.reconnect()
                    uiInfoService.clearSnackBars()
                } catch (e: Throwable) {
                    UiErrorHandler().handleContextError(e, R.string.songcast_connection_context)
                }
                lastShot = Date().time
            }
            delay(1_000)
        }
    }

    fun dropSessionAsync(): Deferred<Result<Unit>> {
        return requester.dropSessionAsync { exitRoom() }
    }

    private fun getSessionDetailsAsync(): Deferred<Result<CastSession>> {
        return requester.getSessionDetailsAsync { responseData ->
            sessionStateMutex.withLock {
                oldState = sessionState.copy()
                sessionState.members = responseData.members
                sessionState.castSongDto = responseData.song
                sessionState.currentScroll = responseData.scroll
                sessionState.chatMessages = responseData.chat_messages
                sessionState.createdTime = responseData.create_timestamp
                sessionState.initialized = true
                ephemeralSong = responseData.song?.let { buildEphemeralSong(it) }
            }
        }
    }

    private fun postSongPresentAsync(payload: CastSongSelected): Deferred<Result<Unit>> {
        return requester.postSongPresentAsync(payload) {}
    }

    fun postScrollControlAsync(payload: CastScroll): Deferred<Result<Unit>> {
        return requester.postScrollControlAsync(payload)
    }

    fun postChatMessageAsync(payload: CastChatMessageSent): Deferred<Result<Unit>> {
        return requester.postChatMessageAsync(payload)
    }

    fun promoteMemberAsync(memberPubId: String): Deferred<Result<Unit>> {
        return requester.promoteMemberAsync(memberPubId)
    }

    suspend fun presentMyOpenedSong(song: Song) {
        if (!isPresenter()) return
        // opening the song presented by someone else (or returning to the same one)
        if (song.namespace == SongNamespace.Ephemeral) return

        val castSongId: String = SongHasher().hashSong(song)
        val payload = CastSongSelected(
            id = castSongId,
            title = song.title,
            artist = song.artist,
            content = song.content.orEmpty(),
            chords_notation_id = song.chordsNotation.id,
        )
        val castSongDto = CastSong(
            id = payload.id,
            chosen_by = myMemberPublicId,
            title = payload.title,
            artist = payload.artist,
            content = payload.content,
            chords_notation_id = payload.chords_notation_id,
        )
        val ephemeralSongN = buildEphemeralSong(castSongDto)
        sessionStateMutex.withLock {
            sessionState.castSongDto = castSongDto
            sessionState.currentScroll = null
            ephemeralSong = ephemeralSongN
        }

        logEvents.add(
            SongLogEvent(timestampMs = Date().time, author = myName, song = ephemeralSongN)
        )
        val songName = buildSongName(payload.title, payload.artist)
        uiInfoService.showInfo(R.string.songcast_song_selected_by_me, songName)

        songPreviewLayoutController.addOnInitListener {
            defaultScope.launch {
                val result = postSongPresentAsync(payload).await()
                result.fold(onSuccess = {
                    scrollService.shareScrollControl()
                }, onFailure = { e ->
                    UiErrorHandler().handleError(e, R.string.error_communication_breakdown)
                })
            }
        }
    }

    private fun buildEphemeralSong(songDto: CastSong): Song {
        val now: Long = Date().time
        return Song(
            id = songDto.id,
            title = songDto.title,
            categories = mutableListOf(),
            content = songDto.content,
            versionNumber = 1,
            createTime = now,
            updateTime = now,
            status = SongStatus.PUBLISHED,
            customCategoryName = songDto.artist,
            chordsNotation = ChordsNotation.mustParseById(songDto.chords_notation_id),
            namespace = SongNamespace.Ephemeral,
        )
    }

    private fun followsPresentedSong(pubMemberId: String?): Boolean {
        return when {
            !clientOpenPresentedSongs -> false
            pubMemberId == null -> false
            pubMemberId == myMemberPublicId -> false
            //songPreviewLayoutController.currentSong == ephemeralSong -> false // the same song opened again
            else -> true
        }
    }

    fun openPresentedSong() {
        val ephemeralSongN = ephemeralSong ?: return
        defaultScope.launch {
            songOpener.openSongPreview(ephemeralSongN) {
                adaptToScrollControl()
            }
        }
    }

    private suspend fun onEventBroadcast(data: JSONObject) {
        when (val type = data.getString("type")) {
            "SongSelectedEvent" -> refreshSessionDetails()
            "SongDeselectedEvent" -> refreshSessionDetails()
            "CastMembersUpdatedEvent" -> refreshSessionDetails()
            "ChatMessageReceivedEvent" -> refreshSessionDetails()
            "SongScrolledEvent" -> refreshSessionDetails()
            else -> {
                logger.warn("Unknown SongCast event type: $type")
                refreshSessionDetails()
            }
        }
    }

    fun refreshSessionIfInRoom() {
        if (!isInRoom())
            return
        ioScope.launch {
            refreshSessionDetails()
        }
    }

    suspend fun refreshSessionDetails(): Result<Unit> {
        val result = getSessionDetailsAsync().await()
        result.fold(onSuccess = {
            sessionStateMutex.withLock {
                val compareResult = watchSessionState(oldState, sessionState)
                if (compareResult)
                    lastSessionChange = Date().time
            }
            refreshUI()
            return Result.success(Unit)
        }, onFailure = { e ->
            UiErrorHandler().handleContextError(e, R.string.songcast_connection_context)
            return Result.failure(e)
        })
    }

    private fun watchSessionState(oldState: SessionState, newState: SessionState): Boolean {
        if (!oldState.initialized)
            return true

        var changed = false
        if (oldState.castSongDto != newState.castSongDto) {
            newState.castSongDto?.let { castSongDto ->
                onSongSelectedEventDto(castSongDto)
            }
            changed = true
        }
        if (oldState.chatMessages != newState.chatMessages) {
            val newMessages = newState.chatMessages.minus(oldState.chatMessages.toSet())
            if (newMessages.isNotEmpty()) {
                val message = newMessages.last()
                if (message.author != this.myName) {
                    uiInfoService.showInfoAction(
                        R.string.songcast_new_chat_message, message.author, message.text,
                        actionResId = R.string.songcast_action_lobby,
                        action = { showLobby() },
                    )
                }
            }
            changed = true
        }
        if (oldState.members != newState.members) {
            val droppedMembers = oldState.members.toSet().minus(newState.members.toSet())
            droppedMembers.forEach { member ->
                uiInfoService.showInfoAction(
                    R.string.songcast_member_dropped, member.name,
                    actionResId = R.string.songcast_action_lobby,
                    action = { showLobby() },
                )
                addSystemLogEvent(R.string.songcast_member_dropped, member.name)
            }
            val newMembers = newState.members.toSet().minus(oldState.members.toSet())
            newMembers.forEach { member ->
                val memberJoinedResId = when (member.type) {
                    CastMemberType.OWNER.value -> R.string.songcast_joined_the_room_as_presenter
                    else -> R.string.songcast_joined_the_room_as_spectator
                }
                uiInfoService.showInfoAction(
                    memberJoinedResId, member.name,
                    actionResId = R.string.songcast_action_lobby,
                    action = { showLobby() },
                )
                addSystemLogEvent(memberJoinedResId, member.name)
            }
            changed = true
        }
        if (oldState.currentScroll != newState.currentScroll) {
            val scrollDto = newState.currentScroll
            scrollDto?.run {
                adaptToScrollControl()
            }
            changed = true
        }
        return changed
    }

    private fun onSongSelectedEventDto(castSongDto: CastSong) {
        val presenter: CastMember? = sessionState.members.find { it.public_member_id == castSongDto.chosen_by }
        val presenterName = presenter?.name ?: "Unknown"
        val songName = buildSongName(castSongDto.title, castSongDto.artist)
        ephemeralSong?.let { ephemeralSong ->
            logEvents.add(
                SongLogEvent(timestampMs = Date().time, author = presenterName, song = ephemeralSong)
            )
        }
        val amIPresenting = presenter?.public_member_id == myMemberPublicId
        val followsSong = followsPresentedSong(presenter?.public_member_id)
        when {
            amIPresenting || followsSong -> uiInfoService.showInfo(
                R.string.songcast_song_selected, presenterName, songName,
            )
            else -> uiInfoService.showInfoAction(
                R.string.songcast_song_selected, presenterName, songName,
                actionResId = R.string.songcast_action_open_song,
                action = { openPresentedSong() },
            )
        }

        refreshUI()
        if (followsSong) {
            openPresentedSong()
        }
    }

    fun showLobby() {
        layoutController.showLayout(SongCastLobbyLayout::class)
    }

    private fun adaptToScrollControl() {
        val scrollDto = sessionState.currentScroll ?: return
        val chordsNotation = ephemeralSong?.chordsNotation ?: ChordsNotation.default
        if (clientFollowScroll && !isPresenting() && layoutController.isState(SongPreviewLayoutController::class)) {
            logger.debug("scrolling by SongCast event: mode=${scrollDto.mode}, start=${scrollDto.view_start}")
            scrollService.adaptToScrollControl(
                scrollDto.view_start, scrollDto.visible_text, scrollDto.mode, chordsNotation,
            )
        }
    }

    private fun addSystemLogEvent(resourceId: Int, vararg args: Any?) {
        val text = uiInfoService.resString(resourceId, *args)
        logEvents.add(
            SystemLogEvent(timestampMs = Date().time, text = text)
        )
        refreshUI()
    }

    fun generateChatEvents(): List<LogEvent> {
        val allEvents = mutableListOf<LogEvent>()
        allEvents.addAll(logEvents)
        allEvents.addAll(sessionState.chatMessages.map {
            MessageLogEvent(
                timestampMs = it.timestamp * 1000,
                author = it.author,
                text = it.text,
            )
        })
        return allEvents.sortedBy { it.timestampMs }
    }

    private fun refreshUI() {
        mainScope.launch {
            onSessionUpdated()
        }
    }

    fun isSameSongPresented(song: Song?): Boolean {
        val songN = song ?: return false
        val ephemeralSongN = ephemeralSong ?: return false
        val hash1 = SongHasher().hashSong(songN)
        val hash2 = SongHasher().hashSong(ephemeralSongN)
        return hash1 == hash2
    }
}

data class SessionState(
    var initialized: Boolean = false, // first fetch done
    var members: List<CastMember> = listOf(),
    var castSongDto: CastSong? = null,
    var currentScroll: CastScroll? = null,
    var chatMessages: List<CastChatMessage> = listOf(),
    var createdTime: Long = 0, // in seconds
)
