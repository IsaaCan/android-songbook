package mwongela.songbook.room

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import mwongela.songbook.info.logger.LoggerFactory.logger
import mwongela.songbook.room.protocol.GtrProtocol.Companion.BT_APP_UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.trySendBlocking
import java.io.IOException

class NewSlaveListener(
    private val bluetoothAdapter: BluetoothAdapter,
    private val newSlaveChannel: SendChannel<PeerStream>,
) {

    private val initChannel = Channel<Result<Unit>>(1)
    private var serverSocket: BluetoothServerSocket? = null
    private val looperJob: Job
    private val looperScope: CoroutineScope = CoroutineScope(CoroutineName("newSlaveListener"))
    private var open = true
    private val openSockets = mutableListOf<BluetoothSocket>()

    init {
        looperJob = looperScope.launch {
            run()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun run() {
        logger.debug("hosting BT room...")

        try {
            withContext(Dispatchers.IO) {

                try {
                    serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        "Songbook",
                        BT_APP_UUID,
                    )
                    initChannel.trySendBlocking(Result.success(Unit))
                } catch (e: Throwable) {
                    logger.error("creating Rfcomm server socket", e)
                    initChannel.trySendBlocking(Result.failure(e))
                    throw e
                }

                logger.debug("listening for Bluetooth connections")
                while (true) {
                    // This will block until there is a connection
                    val socket: BluetoothSocket = serverSocket!!.accept()

                    try {
                        val macAddress = socket.remoteDevice.address
                        logger.debug("socket accepted for $macAddress")

                        val receivedClientMsgCh = Channel<String>(Channel.UNLIMITED)
                        val clientStream = PeerStream(socket, receivedClientMsgCh)
                        looperScope.launch {
                            newSlaveChannel.send(clientStream)
                        }
                        openSockets.add(socket)

                    } catch (connectException: IOException) {
                        logger.error(
                            "Failed to start a Bluetooth connection as a server",
                            connectException
                        )
                        socket.close()
                    }
                }

            }
        } catch (e: Throwable) {
            logger.error("Server socket error", e)
        }

        GlobalScope.launch(Dispatchers.IO) {
            close()
        }
    }

    fun isInitializedAsync(): Deferred<Result<Unit>> {
        return GlobalScope.async {
            initChannel.receiveCatching().getOrNull()
                ?: Result.failure(ClosedReceiveChannelException("init channel closed"))
        }
    }

    fun close() {
        if (!open)
            return
        open = false

        if (looperJob.isActive) {
            looperScope.cancel()
            looperJob.cancel()
        }

        openSockets.forEach { socket ->
            if (socket.isConnected)
                socket.close()
        }
        openSockets.clear()

        serverSocket?.close()
        initChannel.close()
    }
}