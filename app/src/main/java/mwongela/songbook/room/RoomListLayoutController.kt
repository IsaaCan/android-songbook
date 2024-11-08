package mwongela.songbook.room

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import mwongela.songbook.R
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.errorcheck.SafeClickListener
import mwongela.songbook.info.errorcheck.UiErrorHandler
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.InflatedLayout
import mwongela.songbook.layout.contextmenu.ContextMenuBuilder
import mwongela.songbook.layout.dialog.InputDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomListLayoutController(
    bluetoothService: LazyInject<BluetoothService> = appFactory.bluetoothService,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    roomLobby: LazyInject<RoomLobby> = appFactory.roomLobby,
) : InflatedLayout(
    _layoutResourceId = R.layout.screen_room_list
) {
    private val bluetoothService by LazyExtractor(bluetoothService)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val roomLobby by LazyExtractor(roomLobby)

    private var joinRoomListView: JoinRoomListView? = null
    private var myNameEditText: EditText? = null
    private var showScanning = true
    private var scanJob: Job? = null

    override fun showLayout(layout: View) {
        super.showLayout(layout)

        if (roomLobby.isActive()) {
            GlobalScope.launch(Dispatchers.Main) {
                layoutController.showLayout(RoomLobbyLayoutController::class, disableReturn = true)
            }
            return
        }

        layout.findViewById<ImageButton>(R.id.moreActionsButton)
            ?.setOnClickListener(SafeClickListener {
                showMoreActions()
            })

        myNameEditText = layout.findViewById<EditText>(R.id.myNameEditText)?.also {
            it.setText(bluetoothService.deviceName())
        }

        layout.findViewById<Button>(R.id.hostNewRoomButton)?.setOnClickListener {
            hostRoom()
        }

        joinRoomListView = layout.findViewById<JoinRoomListView>(R.id.itemsListView)?.also {
            it.init()
            it.onClickCallback = { room ->
                joinRoomKnock(room)
            }
            it.items = listOf()
            it.enableNestedScrolling()
            it.emptyView = layout.findViewById(R.id.emptyRoomListTextView)
            it.alignListViewHeight()
        }

        layout.findViewById<Button>(R.id.scanRoomsButtton)?.setOnClickListener {
            scanRooms()
        }
    }

    private fun hostRoom() {
        InputDialogBuilder().input(R.string.screen_share_set_room_password, null) { password ->
            GlobalScope.launch {
                val username = myNameEditText?.text?.toString().orEmpty()
                roomLobby.hostRoomAsync(username, password).await().fold(onSuccess = {
                    uiInfoService.showInfo(R.string.room_room_created)
                    layoutController.showLayout(RoomLobbyLayoutController::class)
                }, onFailure = { e ->
                    UiErrorHandler().handleError(e, R.string.error_communication_breakdown)
                })
            }
        }
    }

    private fun joinRoomKnock(room: Room) {
        showScanning = false
        roomLobby.onRoomLobbyIntroduced = ::onRoomLobbyIntroduced
        GlobalScope.launch {
            uiInfoService.showInfo(R.string.room_joining_room, room.name)
            val username = myNameEditText?.text?.toString().orEmpty()
            roomLobby.joinRoomKnockAsync(username, room).await().fold(onSuccess = {

            }, onFailure = { e ->
                UiErrorHandler().handleError(e, R.string.error_communication_breakdown)
            })
        }
    }

    private fun onRoomLobbyIntroduced(roomName: String, withPassword: Boolean) {
        val username = myNameEditText?.text?.toString().orEmpty()
        roomLobby.onRoomWrongPassword = ::onRoomWrongPassword
        roomLobby.onRoomWelcomedSuccessfully = ::onRoomWelcomedSuccessfully
        if (withPassword) {
            InputDialogBuilder().input(
                R.string.screen_share_enter_room_password,
                null
            ) { password ->
                roomLobby.enterRoom(username, password)
            }
        } else {
            uiInfoService.showInfo(R.string.room_joining_room, roomName)
            roomLobby.enterRoom(username, "")
        }
    }

    private fun onRoomWrongPassword() {
        uiInfoService.showInfo(R.string.room_wrong_password)
    }

    private fun onRoomWelcomedSuccessfully() {
        uiInfoService.showInfo(R.string.room_joined_to_room)
        GlobalScope.launch(Dispatchers.Main) {
            layoutController.showLayout(RoomLobbyLayoutController::class)
        }
    }

    private fun showMoreActions() {
        ContextMenuBuilder().showContextMenu(
            mutableListOf(
                ContextMenuBuilder.Action(R.string.screen_share_scan_rooms) {
                    scanRooms()
                },
            )
        )
    }

    private fun scanRooms() {
        showScanning = false
        joinRoomListView?.items = emptyList()
        uiInfoService.showInfo(R.string.screen_share_scanning_devices, indefinite = true)

        scanJob?.cancel()
        scanJob = GlobalScope.launch(Dispatchers.IO) {
            bluetoothService.scanRoomsAsync().await().fold(onSuccess = { (roomCh, progressCh) ->
                showScanning = true
                GlobalScope.launch {
                    for (progress in progressCh) {
                        if (showScanning)
                            uiInfoService.showInfo(
                                R.string.room_scanning_progress,
                                "${progress.done.get()}/${progress.all.get()}",
                                indefinite = true
                            )
                    }
                }

                for (room in roomCh) {
                    withContext(Dispatchers.Main) {
                        joinRoomListView?.add(room)
                        joinRoomListView?.alignListViewHeight()
                    }
                }
                if (showScanning) {
                    val found = joinRoomListView?.items?.size ?: 0
                    if (found > 0) {
                        uiInfoService.showInfo(
                            R.string.room_scanning_completed_found,
                            found.toString()
                        )
                    } else {
                        uiInfoService.showInfo(R.string.room_scanning_completed_not_found)
                    }
                }
            }, onFailure = { e ->
                UiErrorHandler().handleError(e, R.string.error_communication_breakdown)
            })
        }
    }

}