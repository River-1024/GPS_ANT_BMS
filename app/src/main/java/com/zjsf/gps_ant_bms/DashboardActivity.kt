package com.zjsf.gps_ant_bms

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zjsf.gps_ant_bms.model.BmsLiveDataStore
import com.zjsf.gps_ant_bms.model.DashboardData
import com.zjsf.gps_ant_bms.model.DashboardSettings
import com.zjsf.gps_ant_bms.model.DashboardSettingsStore
import com.zjsf.gps_ant_bms.ui.PowerBarView
import com.zjsf.gps_ant_bms.ui.PowerChartView
import com.zjsf.gps_ant_bms.ui.SocFillView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class DashboardActivity : AppCompatActivity() {

    private lateinit var socFillView: SocFillView
    private lateinit var textSoc: TextView
    private lateinit var textRemaining: TextView
    private lateinit var textRange: TextView
    private lateinit var textPowerMetric: TextView
    private lateinit var textVoltage: TextView
    private lateinit var textCurrent: TextView
    private lateinit var textVoltageDiff: TextView
    private lateinit var textTempMos: TextView
    private lateinit var textTempBalancer: TextView
    private lateinit var textTempSensor1: TextView
    private lateinit var textTempSensor2: TextView
    private lateinit var textSoh: TextView
    private lateinit var textPower: TextView
    private lateinit var textBleStatus: TextView
    private lateinit var textGpsStatus: TextView
    private lateinit var textUpdateTime: TextView
    private lateinit var buttonPowerBar: TextView
    private lateinit var buttonPowerChart: TextView
    private lateinit var buttonBack: TextView
    private lateinit var powerBarView: PowerBarView
    private lateinit var powerChartView: PowerChartView
    private lateinit var metricValueViews: List<TextView>
    private lateinit var longMetricValueViews: List<TextView>
    private lateinit var tempValueViews: List<TextView>

    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var settings = DashboardSettings()
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
            hideSystemBars()
            initViews()
            bindEvents()
            reloadSettings()
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
        hideSystemBars()
        reloadSettings()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun initViews() {
        socFillView = findViewById(R.id.socFillView)
        textSoc = findViewById(R.id.textSoc)
        textRemaining = findViewById(R.id.textRemaining)
        textRange = findViewById(R.id.textRange)
        textPowerMetric = findViewById(R.id.textPowerMetric)
        textVoltage = findViewById(R.id.textVoltage)
        textCurrent = findViewById(R.id.textCurrent)
        textVoltageDiff = findViewById(R.id.textVoltageDiff)
        textTempMos = findViewById(R.id.textTempMos)
        textTempBalancer = findViewById(R.id.textTempBalancer)
        textTempSensor1 = findViewById(R.id.textTempSensor1)
        textTempSensor2 = findViewById(R.id.textTempSensor2)
        textSoh = findViewById(R.id.textSoh)
        textPower = findViewById(R.id.textPower)
        textBleStatus = findViewById(R.id.textBleStatus)
        textGpsStatus = findViewById(R.id.textGpsStatus)
        textUpdateTime = findViewById(R.id.textUpdateTime)
        buttonPowerBar = findViewById(R.id.buttonPowerBar)
        buttonPowerChart = findViewById(R.id.buttonPowerChart)
        buttonBack = findViewById(R.id.buttonBack)
        powerBarView = findViewById(R.id.powerBarView)
        powerChartView = findViewById(R.id.powerChartView)
        metricValueViews = listOf(
            textRemaining,
            textRange,
            textPowerMetric,
            textVoltage,
            textCurrent,
            textVoltageDiff,
            textSoh
        )
        longMetricValueViews = listOf(textRemaining, textRange, textVoltage)
        tempValueViews = listOf(textTempMos, textTempBalancer, textTempSensor1, textTempSensor2)
    }

    private fun bindEvents() {
        buttonPowerBar.setOnClickListener {
            settings = settings.copy(powerMode = DashboardSettingsStore.POWER_MODE_BAR)
            DashboardSettingsStore.save(this, settings)
            applyPowerMode()
        }
        buttonPowerChart.setOnClickListener {
            settings = settings.copy(powerMode = DashboardSettingsStore.POWER_MODE_CHART)
            DashboardSettingsStore.save(this, settings)
            applyPowerMode()
        }
        buttonBack.setOnClickListener { finish() }
    }

    private fun reloadSettings() {
        settings = DashboardSettingsStore.load(this)
        applyValueTextScale()
        applyWindowSettings()
        applyPowerMode()
    }

    private fun applyPowerMode() {
        val chartMode = settings.powerMode == DashboardSettingsStore.POWER_MODE_CHART
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

        val remainingRange = data.remainingCharge * settings.rangeCoefficient

        val socColor = socColor(data.soc)
        socFillView.setSoc(data.soc, socColor)
        textSoc.text = data.soc.toString()
        textRemaining.text = "%.2fAh".format(data.remainingCharge)
        textRange.text = "%.1fkm".format(remainingRange)
        textPowerMetric.text = "%.0fW".format(data.power)
        textVoltage.text = "%.2fV".format(data.voltage)
        textCurrent.text = "%.1fA".format(data.current)
        textVoltageDiff.text = "${data.voltageDiff}mV"
        val displayedTemps = displayedTemperatures(data)
        textTempMos.text = "%d°".format(displayedTemps[0])
        textTempBalancer.text = displayedTemps.getOrNull(1)?.let { "%d°".format(it) } ?: ""
        textTempSensor1.text = displayedTemps.getOrNull(2)?.let { "%d°".format(it) } ?: ""
        textTempSensor2.text = displayedTemps.getOrNull(3)?.let { "%d°".format(it) } ?: ""
        textSoh.text = "${data.soh}%"

        textRemaining.setTextColor(settings.remainingColor)
        textRange.setTextColor(settings.rangeColor)
        textPowerMetric.setTextColor(powerColor(data.power))
        textVoltage.setTextColor(settings.voltageColor)
        textCurrent.setTextColor(settings.currentColor)
        textVoltageDiff.setTextColor(voltageDiffColor(data.voltageDiff))
        tempValueViews.forEach {
            it.setTextColor(tempColor(it.text.toString().filter(Char::isDigit).toIntOrNull() ?: data.mosTemp))
        }
        textSoh.setTextColor(settings.sohColor)

        textPower.text = "%.0fW".format(data.power)
        textPower.setTextColor(powerColor(data.power))

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

        val powerBase = if (data.power < 0.0) settings.chargePowerBase else settings.dischargePowerBase
        powerBarView.setPower(
            data.power,
            powerBase,
            settings.powerColor,
            settings.powerYellowRatio,
            settings.powerRedRatio
        )
        powerChartView.setData(history, powerBase, settings.powerColor)
    }

    private fun applyWindowSettings() {
        requestedOrientation = when (settings.orientationMode) {
            DashboardSettingsStore.ORIENTATION_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val brightness = when (settings.brightnessMode) {
            DashboardSettingsStore.BRIGHTNESS_50 -> 0.5f
            DashboardSettingsStore.BRIGHTNESS_75 -> 0.75f
            DashboardSettingsStore.BRIGHTNESS_100 -> 1.0f
            else -> -1f
        }
        window.attributes = window.attributes.apply {
            screenBrightness = brightness
        }
    }

    private fun applyValueTextScale() {
        val scale = settings.valueTextScale.toFloat()
        textSoc.textSize = 76f * scale
        textPower.textSize = 34f * scale

        metricValueViews.forEach { view ->
            view.textSize = 32f * scale
            view.setHorizontallyScrolling(false)
            view.setAutoSizeTextTypeUniformWithConfiguration(
                18,
                (32f * scale).toInt().coerceAtLeast(18),
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
        longMetricValueViews.forEach { view ->
            view.textSize = 28f * scale
            view.setAutoSizeTextTypeUniformWithConfiguration(
                16,
                (28f * scale).toInt().coerceAtLeast(16),
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
        tempValueViews.forEach { view ->
            view.textSize = 20f * scale
            view.setHorizontallyScrolling(false)
            view.setAutoSizeTextTypeUniformWithConfiguration(
                14,
                (20f * scale).toInt().coerceAtLeast(14),
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
    }

    private fun powerColor(power: Double): Int {
        val base = if (power < 0.0) settings.chargePowerBase else settings.dischargePowerBase
        val ratio = (abs(power) / base).coerceIn(0.0, 1.0)
        return when {
            power < 0.0 -> 0xFF62A8FF.toInt()
            ratio >= settings.powerRedRatio -> 0xFFFF706F.toInt()
            ratio >= settings.powerYellowRatio -> 0xFFF4C85A.toInt()
            else -> settings.powerColor
        }
    }

    private fun socColor(soc: Int): Int {
        return when {
            soc <= settings.socRedUpper -> 0xFFFF706F.toInt()
            soc <= settings.socYellowUpper -> 0xFFF4C85A.toInt()
            else -> settings.socColor
        }
    }

    private fun voltageDiffColor(diff: Int): Int {
        return when {
            diff <= settings.voltageDiffGreenUpper -> settings.diffColor
            diff <= settings.voltageDiffYellowUpper -> 0xFFF4C85A.toInt()
            else -> 0xFFFF706F.toInt()
        }
    }

    private fun tempColor(temp: Int): Int {
        return when {
            temp <= settings.tempGreenUpper -> settings.tempColor
            temp <= settings.tempYellowUpper -> 0xFFF4C85A.toInt()
            else -> 0xFFFF706F.toInt()
        }
    }

    private fun DashboardData.sensorTempOrDefault(index: Int): Int {
        return sensorTemps.getOrNull(index) ?: mosTemp
    }

    private fun displayedTemperatures(data: DashboardData): List<Int> {
        val all = listOf(
            data.mosTemp,
            data.balancerTemp,
            data.sensorTempOrDefault(0),
            data.sensorTempOrDefault(1)
        )
        return when (settings.tempDisplayMode) {
            DashboardSettingsStore.TEMP_MODE_MOS -> listOf(data.mosTemp)
            DashboardSettingsStore.TEMP_MODE_BALANCER -> listOf(data.balancerTemp)
            DashboardSettingsStore.TEMP_MODE_MAX -> listOf(all.maxOrNull() ?: data.mosTemp)
            else -> all
        }
    }

    private fun hideSystemBars() {
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

        private val PREVIEW_DATA = DashboardData(
            speed = 31.8,
            voltage = 81.18,
            current = 0.0,
            voltageDiff = 7,
            soc = 92,
            power = 8.0,
            capacity = 134.0,
            remainingCharge = 121.4,
            mosTemp = 35,
            balancerTemp = 37,
            sensorTemps = listOf(34, 34),
            soh = 100
        )
        private val PREVIEW_POWER_HISTORY = listOf(
            2.0, 4.0, 6.0, 5.0, 8.0, 9.0, 7.0, 12.0,
            10.0, 8.0, 6.0, 7.0, 8.0, 9.0, 8.0, 8.0
        )
    }
}
