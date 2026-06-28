package com.zjsf.gps_ant_bms.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.roundToInt

enum class PowerDisplayMode {
    POWER_BAR,
    CURVE
}

data class DashboardSettings(
    val mileageFactorKmPerAh: Double = 1.2000000476837158,
    val valueScale: Double = 1.0,
    val defaultPowerDisplayMode: PowerDisplayMode = PowerDisplayMode.POWER_BAR,
    val bluetoothRefreshIntervalMs: Long = 1000L,
    val temperatureSource: String = "自动",
    val dischargeBaseW: Double = 6000.0,
    val chargeBaseW: Double = 2500.0,
    val powerYellowStart: Double = 0.55,
    val powerRedStart: Double = 0.90,
    val socRedUpperPercent: Double = 10.0,
    val socYellowUpperPercent: Double = 20.0,
    val diffGreenUpperMv: Double = 20.0,
    val diffYellowUpperMv: Double = 50.0,
    val tempGreenUpperC: Double = 39.0,
    val tempYellowUpperC: Double = 45.0,
    val socColor: String = "#39E36C",
    val remainingAhColor: String = "#39E36C",
    val mileageColor: String = "#39E36C",
    val powerColor: String = "#39E36C",
    val voltageColor: String = "#F3FAF7",
    val currentColor: String = "#59D8F5",
    val diffColor: String = "#F4C85A",
    val temperatureColor: String = "#F4C85A",
    val sohColor: String = "#F3FAF7"
)

data class DashboardUiState(
    val gpsSpeedKmh: Double = 0.0,
    val bmsData: BmsData? = null,
    val connected: Boolean = false,
    val previewMode: Boolean = true,
    val statusMessage: String = "预览模式  GPS预览  未连接保护板",
    val powerHistory: List<Double> = emptyList(),
    val bluetoothRssi: Int? = -70,
    val floatingWindowEnabled: Boolean = false,
    val hideFromRecentsEnabled: Boolean = false,
    val lastDeviceAddress: String? = null,
    val errorMessage: String? = null
)

object DashboardPrefs {
    const val PREFS_NAME = "BmsPrefs"
    const val PREF_FLOATING_WINDOW = "floating_window_enabled"
    const val PREF_HIDE_FROM_RECENTS = "hide_from_recents"
    const val PREF_LAST_DEVICE_ADDRESS = "last_device_address"

    private const val KEY_MILEAGE_FACTOR = "dashboard_mileage_factor_km_per_ah"
    private const val KEY_VALUE_SCALE = "dashboard_value_scale"
    private const val KEY_DEFAULT_MODE = "dashboard_default_power_display_mode"
    private const val KEY_BLUETOOTH_REFRESH = "dashboard_bluetooth_refresh_interval_ms"
    private const val KEY_TEMPERATURE_SOURCE = "dashboard_temperature_source"
    private const val KEY_DISCHARGE_BASE = "dashboard_discharge_base_w"
    private const val KEY_CHARGE_BASE = "dashboard_charge_base_w"
    private const val KEY_POWER_YELLOW = "dashboard_power_yellow_start"
    private const val KEY_POWER_RED = "dashboard_power_red_start"
    private const val KEY_SOC_RED = "dashboard_soc_red_upper_percent"
    private const val KEY_SOC_YELLOW = "dashboard_soc_yellow_upper_percent"
    private const val KEY_DIFF_GREEN = "dashboard_diff_green_upper_mv"
    private const val KEY_DIFF_YELLOW = "dashboard_diff_yellow_upper_mv"
    private const val KEY_TEMP_GREEN = "dashboard_temp_green_upper_c"
    private const val KEY_TEMP_YELLOW = "dashboard_temp_yellow_upper_c"
    private const val KEY_SOC_COLOR = "dashboard_soc_color"
    private const val KEY_AH_COLOR = "dashboard_remaining_ah_color"
    private const val KEY_MILEAGE_COLOR = "dashboard_mileage_color"
    private const val KEY_POWER_COLOR = "dashboard_power_color"
    private const val KEY_VOLTAGE_COLOR = "dashboard_voltage_color"
    private const val KEY_CURRENT_COLOR = "dashboard_current_color"
    private const val KEY_DIFF_COLOR = "dashboard_diff_color"
    private const val KEY_TEMP_COLOR = "dashboard_temperature_color"
    private const val KEY_SOH_COLOR = "dashboard_soh_color"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): DashboardSettings = load(prefs(context))

    fun load(prefs: SharedPreferences): DashboardSettings {
        val defaults = DashboardSettings()
        return DashboardSettings(
            mileageFactorKmPerAh = prefs.getDouble(KEY_MILEAGE_FACTOR, defaults.mileageFactorKmPerAh),
            valueScale = prefs.getDouble(KEY_VALUE_SCALE, defaults.valueScale),
            defaultPowerDisplayMode = runCatching {
                PowerDisplayMode.valueOf(prefs.getString(KEY_DEFAULT_MODE, defaults.defaultPowerDisplayMode.name) ?: defaults.defaultPowerDisplayMode.name)
            }.getOrDefault(defaults.defaultPowerDisplayMode),
            bluetoothRefreshIntervalMs = prefs.getLongValue(KEY_BLUETOOTH_REFRESH, defaults.bluetoothRefreshIntervalMs),
            temperatureSource = prefs.getString(KEY_TEMPERATURE_SOURCE, defaults.temperatureSource) ?: defaults.temperatureSource,
            dischargeBaseW = prefs.getDouble(KEY_DISCHARGE_BASE, defaults.dischargeBaseW),
            chargeBaseW = prefs.getDouble(KEY_CHARGE_BASE, defaults.chargeBaseW),
            powerYellowStart = prefs.getDouble(KEY_POWER_YELLOW, defaults.powerYellowStart),
            powerRedStart = prefs.getDouble(KEY_POWER_RED, defaults.powerRedStart),
            socRedUpperPercent = prefs.getDouble(KEY_SOC_RED, defaults.socRedUpperPercent),
            socYellowUpperPercent = prefs.getDouble(KEY_SOC_YELLOW, defaults.socYellowUpperPercent),
            diffGreenUpperMv = prefs.getDouble(KEY_DIFF_GREEN, defaults.diffGreenUpperMv),
            diffYellowUpperMv = prefs.getDouble(KEY_DIFF_YELLOW, defaults.diffYellowUpperMv),
            tempGreenUpperC = prefs.getDouble(KEY_TEMP_GREEN, defaults.tempGreenUpperC),
            tempYellowUpperC = prefs.getDouble(KEY_TEMP_YELLOW, defaults.tempYellowUpperC),
            socColor = prefs.getColorString(KEY_SOC_COLOR, defaults.socColor),
            remainingAhColor = prefs.getColorString(KEY_AH_COLOR, defaults.remainingAhColor),
            mileageColor = prefs.getColorString(KEY_MILEAGE_COLOR, defaults.mileageColor),
            powerColor = prefs.getColorString(KEY_POWER_COLOR, defaults.powerColor),
            voltageColor = prefs.getColorString(KEY_VOLTAGE_COLOR, defaults.voltageColor),
            currentColor = prefs.getColorString(KEY_CURRENT_COLOR, defaults.currentColor),
            diffColor = prefs.getColorString(KEY_DIFF_COLOR, defaults.diffColor),
            temperatureColor = prefs.getColorString(KEY_TEMP_COLOR, defaults.temperatureColor),
            sohColor = prefs.getColorString(KEY_SOH_COLOR, defaults.sohColor)
        ).normalized()
    }

    fun save(context: Context, settings: DashboardSettings) {
        save(prefs(context), settings)
    }

    fun save(prefs: SharedPreferences, settings: DashboardSettings) {
        val safe = settings.normalized()
        prefs.edit()
            .putString(KEY_MILEAGE_FACTOR, safe.mileageFactorKmPerAh.toString())
            .putString(KEY_VALUE_SCALE, safe.valueScale.toString())
            .putString(KEY_DEFAULT_MODE, safe.defaultPowerDisplayMode.name)
            .putString(KEY_BLUETOOTH_REFRESH, safe.bluetoothRefreshIntervalMs.toString())
            .putString(KEY_TEMPERATURE_SOURCE, safe.temperatureSource)
            .putString(KEY_DISCHARGE_BASE, safe.dischargeBaseW.toString())
            .putString(KEY_CHARGE_BASE, safe.chargeBaseW.toString())
            .putString(KEY_POWER_YELLOW, safe.powerYellowStart.toString())
            .putString(KEY_POWER_RED, safe.powerRedStart.toString())
            .putString(KEY_SOC_RED, safe.socRedUpperPercent.toString())
            .putString(KEY_SOC_YELLOW, safe.socYellowUpperPercent.toString())
            .putString(KEY_DIFF_GREEN, safe.diffGreenUpperMv.toString())
            .putString(KEY_DIFF_YELLOW, safe.diffYellowUpperMv.toString())
            .putString(KEY_TEMP_GREEN, safe.tempGreenUpperC.toString())
            .putString(KEY_TEMP_YELLOW, safe.tempYellowUpperC.toString())
            .putString(KEY_SOC_COLOR, safe.socColor)
            .putString(KEY_AH_COLOR, safe.remainingAhColor)
            .putString(KEY_MILEAGE_COLOR, safe.mileageColor)
            .putString(KEY_POWER_COLOR, safe.powerColor)
            .putString(KEY_VOLTAGE_COLOR, safe.voltageColor)
            .putString(KEY_CURRENT_COLOR, safe.currentColor)
            .putString(KEY_DIFF_COLOR, safe.diffColor)
            .putString(KEY_TEMP_COLOR, safe.temperatureColor)
            .putString(KEY_SOH_COLOR, safe.sohColor)
            .apply()
    }

    private fun SharedPreferences.getDouble(key: String, defaultValue: Double): Double =
        getString(key, null)?.toDoubleOrNull() ?: defaultValue

    private fun SharedPreferences.getLongValue(key: String, defaultValue: Long): Long =
        getString(key, null)?.toLongOrNull() ?: defaultValue

    private fun SharedPreferences.getColorString(key: String, defaultValue: String): String {
        val value = getString(key, defaultValue) ?: defaultValue
        return if (isValidColor(value)) value.uppercase() else defaultValue
    }
}

fun DashboardSettings.normalized(): DashboardSettings =
    copy(
        mileageFactorKmPerAh = mileageFactorKmPerAh.coerceIn(0.0, 1000.0),
        valueScale = valueScale.coerceIn(0.5, 2.0),
        bluetoothRefreshIntervalMs = bluetoothRefreshIntervalMs.coerceIn(200L, 60_000L),
        dischargeBaseW = dischargeBaseW.coerceAtLeast(1.0),
        chargeBaseW = chargeBaseW.coerceAtLeast(1.0),
        powerYellowStart = powerYellowStart.coerceIn(0.0, 1.0),
        powerRedStart = powerRedStart.coerceIn(powerYellowStart.coerceIn(0.0, 1.0), 1.0),
        socRedUpperPercent = socRedUpperPercent.coerceIn(0.0, 100.0),
        socYellowUpperPercent = socYellowUpperPercent.coerceIn(socRedUpperPercent.coerceIn(0.0, 100.0), 100.0),
        diffGreenUpperMv = diffGreenUpperMv.coerceAtLeast(0.0),
        diffYellowUpperMv = diffYellowUpperMv.coerceAtLeast(diffGreenUpperMv.coerceAtLeast(0.0)),
        tempGreenUpperC = tempGreenUpperC.coerceIn(-50.0, 150.0),
        tempYellowUpperC = tempYellowUpperC.coerceAtLeast(tempGreenUpperC).coerceAtMost(150.0),
        socColor = socColor.normalizedColor("#39E36C"),
        remainingAhColor = remainingAhColor.normalizedColor("#39E36C"),
        mileageColor = mileageColor.normalizedColor("#39E36C"),
        powerColor = powerColor.normalizedColor("#39E36C"),
        voltageColor = voltageColor.normalizedColor("#F3FAF7"),
        currentColor = currentColor.normalizedColor("#59D8F5"),
        diffColor = diffColor.normalizedColor("#F4C85A"),
        temperatureColor = temperatureColor.normalizedColor("#F4C85A"),
        sohColor = sohColor.normalizedColor("#F3FAF7")
    )

fun isValidColor(value: String): Boolean =
    Regex("^#[0-9A-Fa-f]{6}$").matches(value)

fun String.normalizedColor(fallback: String): String =
    if (isValidColor(this)) uppercase() else fallback

fun String.toAndroidColor(fallback: String = "#39E36C"): Int =
    runCatching { Color.parseColor(normalizedColor(fallback)) }.getOrDefault(Color.parseColor(fallback))

fun BmsData.displayPower(): Double =
    if (power != 0.0) power else totalVoltage * current

fun BmsData.bestTemperature(settings: DashboardSettings): Int {
    val sensorIndex = settings.temperatureSource.filter { it.isDigit() }.toIntOrNull()?.minus(1)
    if (sensorIndex != null && sensorIndex in temperatures.indices) return temperatures[sensorIndex]
    return temperatures.maxOrNull() ?: maxOf(mosTemp, balancerTemp)
}

fun BmsData.remainingMileageKm(settings: DashboardSettings): Double =
    remainingCharge * settings.mileageFactorKmPerAh

fun BmsData.powerLoadRatio(settings: DashboardSettings): Double {
    val powerValue = displayPower()
    val base = if (powerValue < 0) settings.chargeBaseW else settings.dischargeBaseW
    return abs(powerValue) / base.coerceAtLeast(1.0)
}

fun previewBmsData(): BmsData = BmsData(
    totalVoltage = 81.18,
    current = 0.0,
    soc = 92,
    capacity = 132.0,
    remainingCharge = 121.40,
    mosTemp = 35,
    balancerTemp = 37,
    cellVoltages = List(24) { 3383 + (it % 3) },
    temperatures = listOf(35, 37, 34, 34),
    soh = 100,
    power = 8.0,
    runtime = 0,
    voltageDiff = 7
)

fun dynamicPreviewBmsData(tick: Long): BmsData {
    val wave = sin(tick / 3.0)
    val fastWave = sin(tick / 1.7)
    val soc = (88 + (wave * 7)).roundToInt().coerceIn(1, 100)
    val current = (fastWave * 18.0)
    val voltage = 80.5 + wave * 1.4
    val power = voltage * current
    val remaining = 118.0 + wave * 4.5
    val tempBase = 35 + (fastWave * 4).roundToInt()
    val diff = (8 + abs(wave) * 34).roundToInt()
    return BmsData(
        totalVoltage = voltage,
        current = current,
        soc = soc,
        capacity = 132.0,
        remainingCharge = remaining,
        mosTemp = tempBase,
        balancerTemp = tempBase + 2,
        cellVoltages = List(24) { 3352 + ((it * 5 + tick.toInt()) % 41) },
        temperatures = listOf(tempBase, tempBase + 2, tempBase - 1, tempBase + 1),
        soh = 100,
        power = power,
        runtime = tick,
        voltageDiff = diff
    )
}

fun dynamicPreviewRssi(tick: Long): Int =
    (-70 + (sin(tick / 2.5) * 8)).roundToInt().coerceIn(-95, -35)

fun previewPowerHistory(): List<Double> =
    List(80) { index ->
        if (index < 24) 8.0 else (8.0 + ((index % 7) - 3) * 0.35).roundToInt().toDouble()
    }
