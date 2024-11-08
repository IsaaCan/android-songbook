package mwongela.songbook.room

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import mwongela.songbook.info.logger.LoggerFactory.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PeerStream(
    private val remoteSocket: BluetoothSocket,
    val receivedMsgCh: Channel<String>,
) {
    private var inStream: InputStream = remoteSocket.inputStream
    private var outStream: OutputStream = remoteSocket.outputStream
    private var inBuffer = StringBuffer()
    private val writeMutex = Mutex()
    private val readMutex = Mutex()
    val disconnectedCh = Channel<Throwable?>(Channel.CONFLATED)
    private val looperJob: Job
    private var open = true
    private val maxBuffer = 2048
    private var lastError: Throwable? = null

    init {
        looperJob = GlobalScope.launch(Dispatchers.IO) {
            receiveLooper()
        }
    }

    private suspend fun receiveLooper() {
        while (remoteSocket.isConnected) {
            try {
                withContext(Dispatchers.IO) {
                    delay(100L) //pause and wait for rest of data.
                    var availableBytes = inStream.available()
                    if (availableBytes > 0) {
                        val buffer = ByteArray(maxBuffer)
                        if (availableBytes > maxBuffer)
                            availableBytes = maxBuffer
                        val actualBytes = inStream.read(buffer, 0, availableBytes)

                        val str = String(buffer, 0, actualBytes)

                        readMutex.withLock {
                            inBuffer.append(str)
                            findCompleteMessage()
                        }
                    }
                }
            } catch (e: Throwable) {
                logger.error("reading error, disconnecting peer", e)
                lastError = RuntimeException("reading error: ${e.message}", e)
                break
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            close()
        }
    }

    fun write(input: String) {
        if (!open)
            throw RuntimeException("peer disconnected")

        runBlocking {
            writeMutex.withLock {
                try {
                    outStream.write(input.toByteArray())
                    outStream.write(0)
                    outStream.flush()
                } catch (e: Throwable) {
                    logger.error("sending error, disconnecting peer", e)
                    close()
                }
            }
        }
    }

    fun close() {
        if (!open)
            return
        open = false

        if (looperJob.isActive) {
            looperJob.cancel()
        }

        try {
            remoteSocket.close()
        } catch (e: IOException) {
            logger.error(e)
        }

        GlobalScope.launch {
            disconnectedCh.send(lastError)
            disconnectedCh.close()
        }
    }

    private fun findCompleteMessage() {
        val firstZero = inBuffer.indexOf(0.toChar())
        if (firstZero == -1)
            return

        val message = inBuffer.take(firstZero)
        inBuffer.delete(0, firstZero + 1)

        processMessage(message.toString())

        findCompleteMessage()
    }

    private fun processMessage(message: String) {
        GlobalScope.launch {
            receivedMsgCh.send(message)
        }
    }

    fun remoteAddress(): String {
        return remoteSocket.remoteDevice.address
    }

    @SuppressLint("MissingPermission")
    fun remoteName(): String {
        return remoteSocket.remoteDevice.name
    }

}