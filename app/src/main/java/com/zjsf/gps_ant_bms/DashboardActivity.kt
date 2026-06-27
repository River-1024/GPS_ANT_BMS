package com.zjsf.gps_ant_bms

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zjsf.gps_ant_bms.model.DashboardData
import com.zjsf.gps_ant_bms.model.BmsLiveDataStore
import com.zjsf.gps_ant_bms.ui.PowerBarView
import com.zjsf.gps_ant_bms.ui.PowerChartView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class DashboardActivity : AppCompatActivity() {

    private lateinit var textSoc: TextView
    private lateinit var textVoltage: TextView
    private lateinit var textCurrent: TextView
    private lateinit var textSpeed: TextView
    private lateinit var textTemp: TextView
    private lateinit var textVoltageDiff: TextView
    private lateinit var textCapacity: TextView
    private lateinit var textRemaining: TextView
    private lateinit var textSoh: TextView
    private lateinit var textPower: TextView
    private lateinit var textBleStatus: TextView
    private lateinit var textGpsStatus: TextView
    private lateinit var textUpdateTime: TextView
    private lateinit var buttonPowerBar: Button
    private lateinit var buttonPowerChart: Button
    private lateinit var buttonBack: Button
    private lateinit var editMaxPower: EditText
    private lateinit var powerBarView: PowerBarView
    private lateinit var powerChartView: PowerChartView

    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var powerMode = POWER_MODE_BAR
    private var maxPower = DEFAULT_MAX_POWER
    private var lastLoggedPreviewMode: Boolean? = null

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshDashboard()
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i(TAG, "onCreate start")
        try {
            setContentView(R.layout.activity_dashboard)
            AppLogger.d(TAG, "content view set")
            hideSystemBars()
            AppLogger.d(TAG, "system bars hidden after content view")
            initViews()
            AppLogger.d(TAG, "views initialized")
            loadPreferences()
            AppLogger.d(TAG, "preferences loaded, powerMode=$powerMode, maxPower=$maxPower")
            bindEvents()
            applyPowerMode()
            refreshDashboard()
            AppLogger.i(TAG, "onCreate complete")
        } catch (e: Exception) {
            AppLogger.e(TAG, "onCreate failed", e)
            Toast.makeText(this, "仪表盘加载失败，日志已记录", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        AppLogger.d(TAG, "onResume")
        hideSystemBars()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        AppLogger.d(TAG, "onPause")
        handler.removeCallbacks(refreshRunnable)
        saveMaxPowerFromInput(showToast = false)
    }

    private fun initViews() {
        textSoc = findViewById(R.id.textSoc)
        textVoltage = findViewById(R.id.textVoltage)
        textCurrent = findViewById(R.id.textCurrent)
        textSpeed = findViewById(R.id.textSpeed)
        textTemp = findViewById(R.id.textTemp)
        textVoltageDiff = findViewById(R.id.textVoltageDiff)
        textCapacity = findViewById(R.id.textCapacity)
        textRemaining = findViewById(R.id.textRemaining)
        textSoh = findViewById(R.id.textSoh)
        textPower = findViewById(R.id.textPower)
        textBleStatus = findViewById(R.id.textBleStatus)
        textGpsStatus = findViewById(R.id.textGpsStatus)
        textUpdateTime = findViewById(R.id.textUpdateTime)
        buttonPowerBar = findViewById(R.id.buttonPowerBar)
        buttonPowerChart = findViewById(R.id.buttonPowerChart)
        buttonBack = findViewById(R.id.buttonBack)
        editMaxPower = findViewById(R.id.editMaxPower)
        powerBarView = findViewById(R.id.powerBarView)
        powerChartView = findViewById(R.id.powerChartView)
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        powerMode = prefs.getString(PREF_POWER_MODE, POWER_MODE_BAR) ?: POWER_MODE_BAR
        maxPower = prefs.getFloat(PREF_MAX_POWER, DEFAULT_MAX_POWER.toFloat()).toDouble()
        editMaxPower.setText(maxPower.toInt().toString())
    }

    private fun bindEvents() {
        buttonPowerBar.setOnClickListener {
            powerMode = POWER_MODE_BAR
            savePowerMode()
            applyPowerMode()
        }
        buttonPowerChart.setOnClickListener {
            powerMode = POWER_MODE_CHART
            savePowerMode()
            applyPowerMode()
        }
        editMaxPower.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveMaxPowerFromInput(showToast = true)
        }
        editMaxPower.setOnEditorActionListener { _, _, _ ->
            saveMaxPowerFromInput(showToast = true)
            editMaxPower.clearFocus()
            true
        }
        buttonBack.setOnClickListener { finish() }
    }

    private fun applyPowerMode() {
        val chartMode = powerMode == POWER_MODE_CHART
        buttonPowerBar.isSelected = !chartMode
        buttonPowerChart.isSelected = chartMode
        powerBarView.visibility = if (chartMode) View.GONE else View.VISIBLE
        powerChartView.visibility = if (chartMode) View.VISIBLE else View.GONE
    }

    private fun refreshDashboard() {
        val liveData = BmsLiveDataStore.snapshot()
        val previewMode = !liveData.isBmsConnected
        val data = if (previewMode) PREVIEW_DATA else liveData
        val history = if (previewMode) PREVIEW_POWER_HISTORY else BmsLiveDataStore.powerHistorySnapshot()
        if (lastLoggedPreviewMode != previewMode) {
            AppLogger.i(
                TAG,
                "refresh mode changed: preview=$previewMode, connected=${liveData.isBmsConnected}, hasBmsData=${liveData.hasBmsData}, history=${history.size}"
            )
            lastLoggedPreviewMode = previewMode
        }

        textSoc.text = "${data.soc}%"
        textSoc.setTextColor(
            when {
                data.soc < 20 -> 0xFFFF706F.toInt()
                data.soc < 60 -> 0xFFFFD86B.toInt()
                else -> 0xFF77F06F.toInt()
            }
        )
        textVoltage.text = "%.2fV".format(data.voltage)
        textCurrent.text = "%.1fA".format(data.current)
        textSpeed.text = "%.1fkm/h".format(data.speed)
        textTemp.text = "%d℃".format(data.mosTemp)
        textVoltageDiff.text = "${data.voltageDiff}mV"
        textCapacity.text = "%.0fAh".format(data.capacity)
        textRemaining.text = "%.0fAh".format(data.remainingCharge)
        textSoh.text = "${data.soh}%"

        textPower.text = "%.0fW".format(data.power)
        textPower.setTextColor(
            when {
                data.power < 0.0 -> 0xFF62A8FF.toInt()
                abs(data.power) >= maxPower * 0.9 -> 0xFFFFD86B.toInt()
                else -> 0xFF77F06F.toInt()
            }
        )

        textBleStatus.text = when {
            previewMode -> "预览模式"
            data.hasBmsData -> "蓝牙已连接"
            else -> "蓝牙已连接，等待数据"
        }
        textGpsStatus.text = if (previewMode) {
            "GPS 预览"
        } else if (data.hasGpsData) {
            "GPS 正常"
        } else {
            "GPS 等待定位"
        }
        textUpdateTime.text = when {
            previewMode -> "未连接保护板"
            data.lastUpdatedMillis > 0L -> "更新时间 ${timeFormat.format(Date(data.lastUpdatedMillis))}"
            else -> "更新时间 --:--:--"
        }

        powerBarView.setPower(data.power, maxPower)
        powerChartView.setData(history, maxPower)
    }

    private fun savePowerMode() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_POWER_MODE, powerMode)
            .apply()
    }

    private fun saveMaxPowerFromInput(showToast: Boolean) {
        val parsed = editMaxPower.text.toString().toDoubleOrNull()
        if (parsed == null || parsed < MIN_MAX_POWER || parsed > MAX_MAX_POWER) {
            editMaxPower.setText(maxPower.toInt().toString())
            if (showToast) {
                Toast.makeText(this, "满量程范围 ${MIN_MAX_POWER.toInt()}-${MAX_MAX_POWER.toInt()}W", Toast.LENGTH_SHORT).show()
            }
            return
        }

        maxPower = parsed
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(PREF_MAX_POWER, maxPower.toFloat())
            .apply()
        refreshDashboard()
    }

    private fun hideSystemBars() {
        AppLogger.d(TAG, "hideSystemBars sdk=${android.os.Build.VERSION.SDK_INT}")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val hidden = runCatching {
                val controller = window.decorView.windowInsetsController
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    true
                } else {
                    false
                }
            }.onFailure {
                AppLogger.w(TAG, "hideSystemBars failed, using legacy flags", it)
            }.getOrDefault(false)

            if (!hidden) {
                applyLegacySystemUiFlags()
            }
        } else {
            applyLegacySystemUiFlags()
        }
    }

    private fun applyLegacySystemUiFlags() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    companion object {
        private const val TAG = "DashboardActivity"
        private const val PREFS_NAME = "DashboardPrefs"
        private const val PREF_POWER_MODE = "power_mode"
        private const val PREF_MAX_POWER = "max_power"
        private const val POWER_MODE_BAR = "bar"
        private const val POWER_MODE_CHART = "chart"
        private const val DEFAULT_MAX_POWER = 1000.0
        private const val MIN_MAX_POWER = 100.0
        private const val MAX_MAX_POWER = 20000.0

        private val PREVIEW_DATA = DashboardData(
            speed = 31.8,
            voltage = 100.0,
            current = 2.0,
            voltageDiff = 32,
            soc = 80,
            power = 200.0,
            capacity = 400.0,
            remainingCharge = 320.0,
            mosTemp = 31,
            soh = 96
        )
        private val PREVIEW_POWER_HISTORY = listOf(
            80.0, 120.0, 160.0, 130.0, 210.0, 260.0, 230.0, 310.0,
            280.0, 350.0, 320.0, 390.0, 430.0, 380.0, 250.0, 200.0
        )
    }
}
