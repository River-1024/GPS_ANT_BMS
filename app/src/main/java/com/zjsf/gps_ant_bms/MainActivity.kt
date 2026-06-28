package com.zjsf.gps_ant_bms

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjsf.gps_ant_bms.bluetooth.BleScanner
import com.zjsf.gps_ant_bms.bluetooth.BmsBluetoothManager
import com.zjsf.gps_ant_bms.location.LocationHelper
import com.zjsf.gps_ant_bms.model.BleDevice
import com.zjsf.gps_ant_bms.model.BmsData
import com.zjsf.gps_ant_bms.model.DashboardPrefs
import com.zjsf.gps_ant_bms.model.DashboardSettings
import com.zjsf.gps_ant_bms.model.DashboardUiState
import com.zjsf.gps_ant_bms.model.PowerDisplayMode
import com.zjsf.gps_ant_bms.model.displayPower
import com.zjsf.gps_ant_bms.model.dynamicPreviewBmsData
import com.zjsf.gps_ant_bms.model.dynamicPreviewRssi
import com.zjsf.gps_ant_bms.model.previewPowerHistory
import com.zjsf.gps_ant_bms.protocol.AntProtocol
import com.zjsf.gps_ant_bms.ui.BleDeviceAdapter
import com.zjsf.gps_ant_bms.ui.BmsDashboardApp
import com.zjsf.gps_ant_bms.ui.DashboardRoute

class MainActivity : AppCompatActivity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var bleScanner: BleScanner
    private lateinit var bmsBluetoothManager: BmsBluetoothManager

    private var uiState by mutableStateOf(DashboardUiState(powerHistory = previewPowerHistory()))
    private var dashboardSettings by mutableStateOf(DashboardSettings())
    private var currentRoute by mutableStateOf(DashboardRoute.DASHBOARD)
    private var selectedPowerMode by mutableStateOf(PowerDisplayMode.POWER_BAR)

    private var currentSpeed: Double = 0.0
    private var currentVoltage: Double = 0.000
    private var currentCurrent: Double = 0.0
    private var currentVoltageDiff: Int = 0
    private var currentSoc: Int = 0
    private var lastSelectedDeviceRssi: Int? = -70
    private var previewTick: Long = 0L
    private val previewHandler = Handler(Looper.getMainLooper())
    private val previewRunnable = object : Runnable {
        override fun run() {
            if (!uiState.connected) {
                previewTick += 1
                val preview = dynamicPreviewBmsData(previewTick)
                uiState = uiState.copy(
                    bmsData = preview,
                    previewMode = true,
                    bluetoothRssi = dynamicPreviewRssi(previewTick),
                    statusMessage = buildStatusMessage(connected = false, gpsLive = currentSpeed > 0.0),
                    powerHistory = (uiState.powerHistory + preview.displayPower()).takeLast(maxPowerHistory)
                )
            }
            previewHandler.postDelayed(this, 1000L)
        }
    }

    private lateinit var bleDeviceAdapter: BleDeviceAdapter
    private val discoveredDevices = mutableListOf<BleDevice>()
    private var scanDialog: androidx.appcompat.app.AlertDialog? = null

    private val overlayPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (android.provider.Settings.canDrawOverlays(this)) {
                if (isFloatingWindowEnabled()) {
                    startFloatingWindowService()
                }
            } else {
                setFloatingWindowEnabled(false)
            }
        }
    }

    private val permissionRequestCode = 100
    private val scanPeriod: Long = 5000
    private val maxPowerHistory = 180

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        dashboardSettings = DashboardPrefs.load(this)
        selectedPowerMode = dashboardSettings.defaultPowerDisplayMode
        refreshStoredUiState()
        initModules()
        bmsBluetoothManager.setPollingInterval(dashboardSettings.bluetoothRefreshIntervalMs)
        startPreviewUpdates()
        checkPermissions()

        if (isFloatingWindowEnabled()) {
            checkOverlayPermission()
        }
        applyHideFromRecents(isHideFromRecentsEnabled())

        setContent {
            BmsDashboardApp(
                uiState = uiState,
                settings = dashboardSettings,
                currentRoute = currentRoute,
                selectedPowerMode = selectedPowerMode,
                onRouteChange = { navigateTo(it) },
                onPowerModeChange = { selectedPowerMode = it },
                onSettingsSave = { saveDashboardSettings(it) },
                onScanClick = { showScanDialog() },
                onFloatingWindowChange = { setFloatingWindowEnabled(it) },
                onHideFromRecentsChange = { setHideFromRecentsEnabled(it) },
                onReconnectClick = { reconnectLastDevice() }
            )
        }
    }

    private fun refreshStoredUiState() {
        uiState = uiState.copy(
            floatingWindowEnabled = isFloatingWindowEnabled(),
            hideFromRecentsEnabled = isHideFromRecentsEnabled(),
            lastDeviceAddress = getLastDeviceAddress()
        )
    }

    private fun isFloatingWindowEnabled(): Boolean {
        val prefs = DashboardPrefs.prefs(this)
        return prefs.getBoolean(DashboardPrefs.PREF_FLOATING_WINDOW, false)
    }

    private fun setFloatingWindowEnabled(enabled: Boolean) {
        val prefs = DashboardPrefs.prefs(this)
        prefs.edit().putBoolean(DashboardPrefs.PREF_FLOATING_WINDOW, enabled).apply()
        uiState = uiState.copy(floatingWindowEnabled = enabled)
        if (enabled) {
            checkOverlayPermission()
        } else {
            stopFloatingWindowService()
        }
    }

    private fun isHideFromRecentsEnabled(): Boolean {
        val prefs = DashboardPrefs.prefs(this)
        return prefs.getBoolean(DashboardPrefs.PREF_HIDE_FROM_RECENTS, false)
    }

    private fun setHideFromRecentsEnabled(enabled: Boolean) {
        val prefs = DashboardPrefs.prefs(this)
        prefs.edit().putBoolean(DashboardPrefs.PREF_HIDE_FROM_RECENTS, enabled).apply()
        uiState = uiState.copy(hideFromRecentsEnabled = enabled)
        applyHideFromRecents(enabled)
    }

    private fun applyHideFromRecents(exclude: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.appTasks
            if (tasks.isNotEmpty()) {
                tasks[0].setExcludeFromRecents(exclude)
            }
        }
    }

    private fun saveLastDeviceAddress(address: String) {
        val prefs = DashboardPrefs.prefs(this)
        prefs.edit().putString(DashboardPrefs.PREF_LAST_DEVICE_ADDRESS, address).apply()
        uiState = uiState.copy(lastDeviceAddress = address)
    }

    private fun getLastDeviceAddress(): String? {
        val prefs = DashboardPrefs.prefs(this)
        return prefs.getString(DashboardPrefs.PREF_LAST_DEVICE_ADDRESS, null)
    }

    private fun saveDashboardSettings(settings: DashboardSettings) {
        DashboardPrefs.save(this, settings)
        dashboardSettings = settings
        bmsBluetoothManager.setPollingInterval(settings.bluetoothRefreshIntervalMs)
        navigateTo(DashboardRoute.DASHBOARD)
    }

    private fun navigateTo(route: DashboardRoute) {
        currentRoute = route
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } else {
                startFloatingWindowService()
            }
        } else {
            startFloatingWindowService()
        }
    }

    private fun startFloatingWindowService() {
        val intent = android.content.Intent(this, FloatingWindowService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopFloatingWindowService() {
        val intent = android.content.Intent(this, FloatingWindowService::class.java)
        stopService(intent)
    }

    private fun initModules() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        locationHelper = LocationHelper(this) { location ->
            currentSpeed = location.speed * 3.6
            uiState = uiState.copy(
                gpsSpeedKmh = currentSpeed,
                statusMessage = buildStatusMessage(uiState.connected, gpsLive = true)
            )
            FloatingWindowService.updateData(currentSpeed, currentVoltage, currentCurrent, currentVoltageDiff, currentSoc)
        }

        bleScanner = BleScanner(
            this,
            bluetoothAdapter,
            onDeviceFound = { result ->
                val deviceName = try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        result.device.name ?: "Unknown Device"
                    } else {
                        "Unknown (No Permission)"
                    }
                } catch (e: SecurityException) {
                    "Unknown Device (No Permission)"
                }
                val deviceAddress = result.device.address
                val rssi = result.rssi
                if (getLastDeviceAddress() == deviceAddress) {
                    lastSelectedDeviceRssi = rssi
                    uiState = uiState.copy(bluetoothRssi = rssi)
                }

                val existingIndex = discoveredDevices.indexOfFirst { it.address == deviceAddress }
                if (existingIndex != -1) {
                    discoveredDevices[existingIndex] = discoveredDevices[existingIndex].copy(rssi = rssi)
                } else {
                    discoveredDevices.add(BleDevice(deviceName, deviceAddress, rssi))
                }
                discoveredDevices.sortByDescending { it.rssi }
                runOnUiThread { bleDeviceAdapter.notifyDataSetChanged() }
            },
            onScanStarted = {
                discoveredDevices.clear()
                uiState = uiState.copy(statusMessage = "正在扫描保护板")
                runOnUiThread {
                    bleDeviceAdapter.notifyDataSetChanged()
                    scanDialog?.findViewById<android.widget.ProgressBar>(R.id.progressBarScanning)?.visibility = View.VISIBLE
                }
            },
            onScanStopped = {
                if (!uiState.connected) {
                    uiState = uiState.copy(statusMessage = buildStatusMessage(connected = false, gpsLive = currentSpeed > 0.0))
                }
                runOnUiThread {
                    scanDialog?.findViewById<android.widget.ProgressBar>(R.id.progressBarScanning)?.visibility = View.GONE
                }
            }
        )

        bmsBluetoothManager = BmsBluetoothManager(
            this,
            onDataReceived = { data ->
                val bmsData = AntProtocol.processAntData(data)
                bmsData?.let { updateBmsUi(it) }
            },
            onConnectionStateChanged = { newState ->
                runOnUiThread {
                    val connected = newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED
                    uiState = uiState.copy(
                        connected = connected,
                        previewMode = !connected,
                        bluetoothRssi = if (connected) lastSelectedDeviceRssi ?: uiState.bluetoothRssi else uiState.bluetoothRssi,
                        statusMessage = buildStatusMessage(connected = connected, gpsLive = currentSpeed > 0.0)
                    )
                }
            },
            onRssiRead = { rssi ->
                runOnUiThread {
                    lastSelectedDeviceRssi = rssi
                    uiState = uiState.copy(bluetoothRssi = rssi)
                }
            }
        )

        bleDeviceAdapter = BleDeviceAdapter(this, discoveredDevices) { device ->
            saveLastDeviceAddress(device.address)
            lastSelectedDeviceRssi = device.rssi
            uiState = uiState.copy(bluetoothRssi = device.rssi)
            val deviceObj = bluetoothManager.adapter.getRemoteDevice(device.address)
            bmsBluetoothManager.connect(deviceObj)
            scanDialog?.dismiss()
        }
    }

    private fun reconnectLastDevice() {
        val lastAddress = getLastDeviceAddress()
        if (lastAddress != null) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                try {
                    val device = bluetoothAdapter.getRemoteDevice(lastAddress)
                    android.util.Log.i("MainActivity", "自动连接上次设备: $lastAddress")
                    uiState = uiState.copy(statusMessage = "正在重连保护板")
                    bmsBluetoothManager.connect(device)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "自动连接失败: ${e.message}")
                    uiState = uiState.copy(errorMessage = e.message, statusMessage = "重连失败")
                }
            }
        }
    }

    private fun updateBmsUi(data: BmsData) {
        currentVoltage = data.totalVoltage
        currentCurrent = data.current
        currentVoltageDiff = data.voltageDiff
        currentSoc = data.soc
        FloatingWindowService.updateData(currentSpeed, currentVoltage, currentCurrent, currentVoltageDiff, currentSoc)

        runOnUiThread {
            val nextHistory = (uiState.powerHistory + data.displayPower()).takeLast(maxPowerHistory)
            uiState = uiState.copy(
                bmsData = data,
                connected = true,
                previewMode = false,
                statusMessage = buildStatusMessage(connected = true, gpsLive = currentSpeed > 0.0),
                powerHistory = nextHistory,
                errorMessage = null
            )
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), permissionRequestCode)
        } else {
            locationHelper.startLocationUpdates()
            reconnectLastDevice()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                locationHelper.startLocationUpdates()
                reconnectLastDevice()
            } else {
                uiState = uiState.copy(statusMessage = "权限不足  预览模式")
            }
        }
    }

    private fun showScanDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_ble_scan, null)
        builder.setView(dialogView)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewBleDevicesDialog)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = bleDeviceAdapter

        builder.setNegativeButton("Cancel") { dialog, _ ->
            bleScanner.stopScan()
            dialog.dismiss()
        }

        scanDialog = builder.create()
        scanDialog?.setOnDismissListener { bleScanner.stopScan() }
        scanDialog?.show()

        bleScanner.startScan(scanPeriod)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        refreshStoredUiState()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationHelper.startLocationUpdates()
        }
        if (isFloatingWindowEnabled()) {
            checkOverlayPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        locationHelper.stopLocationUpdates()
        bleScanner.stopScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        previewHandler.removeCallbacks(previewRunnable)
        locationHelper.stopLocationUpdates()
        bleScanner.stopScan()
        bmsBluetoothManager.disconnect()
    }

    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    private fun buildStatusMessage(connected: Boolean, gpsLive: Boolean): String {
        val gps = if (gpsLive) "GPS实时" else "GPS预览"
        val bms = if (connected) "已连接保护板" else "未连接保护板"
        return if (connected) "$gps  $bms" else "预览模式  $gps  $bms"
    }

    private fun startPreviewUpdates() {
        previewHandler.removeCallbacks(previewRunnable)
        previewHandler.post(previewRunnable)
    }
}
