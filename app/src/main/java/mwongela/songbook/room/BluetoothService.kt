package mwongela.songbook.room

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import mwongela.songbook.R
import mwongela.songbook.activity.ActivityResultDispatcher
import mwongela.songbook.info.errorcheck.ContextError
import mwongela.songbook.info.errorcheck.LocalizedError
import mwongela.songbook.info.logger.LoggerFactory.logger
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.room.protocol.GtrProtocol.Companion.BT_APP_UUID
import mwongela.songbook.util.waitUntilCondition
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap


@SuppressLint("MissingPermission")
class BluetoothService(
    appCompatActivity: LazyInject<AppCompatActivity> = appFactory.appCompatActivity,
    activityResultDispatcher: LazyInject<ActivityResultDispatcher> = appFactory.activityResultDispatcher,
) {
    private val activity by LazyExtractor(appCompatActivity)
    private val activityResultDispatcher by LazyExtractor(activityResultDispatcher)

    companion object {
        private const val REQUEST_ENABLE_BT = 20
    }

    @Suppress("DEPRECATION")
    var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val discoveredDevices: ConcurrentHashMap<String, BluetoothDevice> = ConcurrentHashMap()
    private var discoveredRoomsChannel: Channel<Room> = Channel()
    private var discoveryProgressChannel: Channel<DiscoveryProgress> = Channel()
    private var discoveryProgress = DiscoveryProgress()
    private var discoveryJobs: MutableList<Job> = mutableListOf()
    private val reusableSockets: MutableMap<String, BluetoothSocket> = mutableMapOf()

    fun deviceName(): String {
        return try {
            bluetoothAdapter?.name.orEmpty()
        } catch (e: SecurityException) {
            enableBluetoothPermissions()
            ""
        }
    }

    fun scanRoomsAsync(): Deferred<Result<Pair<Channel<Room>, Channel<DiscoveryProgress>>>> {
        return GlobalScope.async {
            return@async runCatching {
                ensureBluetoothEnabled()

                discoveryJobs.forEach { it.cancel() }
                discoveryJobs.clear()
                bluetoothAdapter?.let { bluetoothAdapter -> startDiscovery(bluetoothAdapter) }
                discoveredRoomsChannel.close()
                discoveredRoomsChannel = Channel(16)
                discoveredDevices.clear()
                discoveryProgressChannel.close()
                discoveryProgressChannel = Channel(Channel.UNLIMITED)
                discoveryProgress = DiscoveryProgress()

                return@runCatching discoveredRoomsChannel to discoveryProgressChannel
            }
        }
    }

    private fun startDiscovery(bluetoothAdapter: BluetoothAdapter) {
        if (!bluetoothAdapter.isDiscovering) {
            val result = bluetoothAdapter.startDiscovery()
            if (!result) {
                logger.error("Starting Bluetooth discovery failed")
            }
        }

        activity.registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        activity.registerReceiver(
            discoveryReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        )
        activity.registerReceiver(
            discoveryReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        )
    }

    private val discoveryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    onDeviceDiscovered(intent)
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    logger.debug("BT Discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    logger.debug("BT Discovery finished")
                    onDiscoveryFinished()
                }
            }
        }
    }

    private fun onDiscoveryFinished() {
        GlobalScope.launch(Dispatchers.IO) {
            for (job in discoveryJobs.toList()) {
                job.join()
            }
            discoveredRoomsChannel.close()
        }
    }

    @Suppress("DEPRECATION")
    private fun onDeviceDiscovered(intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            ?: return
        logger.debug("BT device discovered: ${device.name} (${device.address})")

        if (discoveredDevices.containsKey(device.address)) {
            logger.debug("BT device already discovered (${device.address})")
            return
        }

        discoveredDevices[device.address] = device

        detectRoomOnDevice(device)
    }

    private fun detectRoomOnDevice(device: BluetoothDevice) {
        val job = GlobalScope.launch(Dispatchers.IO) {
            try {
                discoveryProgress.all.incrementAndGet()
                try {
                    discoveryProgressChannel.send(discoveryProgress)
                } catch (e: ClosedSendChannelException) {
                    // channel closed
                }

                try {
                    detectDeviceSocket(device.address)
                } catch (e: Throwable) {
                    logger.warn("room is unavailable on device ${device.address}: ${e.message}")
                    return@launch
                }

                try {
                    val room = Room(
                        name = device.name.orEmpty(),
                        hostAddress = device.address,
                    )
                    discoveredRoomsChannel.send(room)
                } catch (e: ClosedSendChannelException) {
                    // channel closed
                }

            } finally {
                discoveryProgress.done.incrementAndGet()
                try {
                    discoveryProgressChannel.send(discoveryProgress)
                } catch (e: ClosedSendChannelException) {
                    // channel closed
                }
            }
        }
        discoveryJobs.add(job)
    }

    private fun detectDeviceSocket(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
            ?: throw RuntimeException("no bluetooth adapter")
        reuseBluetoothSocket(address)
        logger.debug("Room found on ${device.name} ($address)")
    }

    @Suppress("DEPRECATION")
    fun ensureBluetoothEnabled() {
        // Coarse Location permission required to discover devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ), 1
                )
            }
        }
        enableBluetoothPermissions()

        if (bluetoothAdapter?.isEnabled == true)
            return

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        val turnOnResult = waitUntilCondition(retries = 20, delayMs = 500) {
            bluetoothAdapter?.isEnabled == true
        }
        if (!turnOnResult)
            throw LocalizedError(R.string.error_bluetooth_not_enabled)
    }

    private fun enableBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  // Android 12
            ActivityCompat.requestPermissions(
                activity, arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ), 2
            )
        }
    }

    fun connectToRoomSocketAsync(room: Room): Deferred<Result<BluetoothSocket>> {
        bluetoothAdapter?.cancelDiscovery()

        return GlobalScope.async(Dispatchers.IO) {
            runCatching {
                reuseBluetoothSocket(room.hostAddress.orEmpty())
            }
        }
    }

    private fun reuseBluetoothSocket(btAddress: String): BluetoothSocket {
        val reusable = reusableSockets[btAddress]
        if (reusable != null && reusable.isConnected) {
            logger.debug("reusing open socket on $btAddress")
            return reusable
        }
        val newSocket = connectBluetoothSocket(btAddress)
        reusableSockets[btAddress] = newSocket
        return newSocket
    }

    private fun connectBluetoothSocket(btAddress: String?): BluetoothSocket {
        val btSocket: BluetoothSocket
        val device = bluetoothAdapter?.getRemoteDevice(btAddress)
            ?: throw RuntimeException("No Bluetooth adapter")
        logger.debug("Detecting BT Room socket on ${device.name} (${btAddress})")
        try {
            btSocket = createBluetoothSocket(device)
        } catch (e: IOException) {
            throw ContextError("Socket creation failed to ${device.name}", e)
        }

        try {
            btSocket.connect()
            logger.debug("socket connected to ${device.name}")
        } catch (e: IOException) {
            btSocket.close()
            throw ContextError("socket connection failed to ${device.name}", e)
        }
        return btSocket
    }

    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        return try {
            device.createInsecureRfcommSocketToServiceRecord(BT_APP_UUID)
        } catch (e: Exception) {
            logger.error("Could not create Insecure RFComm Connection", e)
            device.createRfcommSocketToServiceRecord(BT_APP_UUID)
        }
    }

    fun cancelDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        discoveryProgressChannel.close()
    }

    fun makeDiscoverable() {
        if (bluetoothAdapter == null) {
            logger.error("no bluetooth adapter")
            return
        }

        if (bluetoothAdapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            logger.debug("already discoverable")
            return
        }
        logger.debug("asking for discoverability")
        val discoverableIntent: Intent =
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }

        activityResultDispatcher.startActivityForResult(discoverableIntent) { resultCode: Int, _: Intent? ->
            if (resultCode == Activity.RESULT_OK) {
                logger.debug("discoverability prolonged successfully")
            }
        }
    }

}