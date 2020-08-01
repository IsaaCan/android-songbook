package igrek.songbook.room

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import igrek.songbook.info.logger.LoggerFactory.logger
import igrek.songbook.room.protocol.GtrProtocol.Companion.BT_APP_UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
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

    init {
        looperJob = looperScope.launch {
            run()
        }
    }

    suspend fun run() {
        logger.debug("hosting BT room...")

        try {
            try {
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Songbook", BT_APP_UUID)
                logger.debug("sending success to init channel")
                initChannel.sendBlocking(Result.success(Unit))
                logger.debug("initChannel: ${initChannel.isEmpty}")
            } catch (e: Throwable) {
                logger.error("creating Rfcomm server socket", e)
                initChannel.sendBlocking(Result.failure(e))
                throw e
            }

            logger.debug("listening for Bluetooth connections")
            while (true) {
                val socket: BluetoothSocket = serverSocket!!.accept() // This will block until there is a connection

                try {
                    val macAddress = socket.remoteDevice.address
                    logger.debug("socket accepted for $macAddress")

                    val receivedClientMsgCh = Channel<String>(Channel.UNLIMITED)
                    val clientStream = PeerStream(socket, receivedClientMsgCh)
                    looperScope.launch {
                        newSlaveChannel.send(clientStream)
                    }

                } catch (connectException: IOException) {
                    logger.error("Failed to start a Bluetooth connection as a server", connectException)
                    socket.close()
                }
            }

        } catch (e: Throwable) {
            logger.error("Server socket error", e)
        }

        GlobalScope.launch(Dispatchers.IO) {
            close()
        }
    }

    fun isInitialized(): Deferred<Result<Unit>> {
        return GlobalScope.async {
            initChannel.receiveOrNull()
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

        serverSocket?.close()
        initChannel.close()
    }
}