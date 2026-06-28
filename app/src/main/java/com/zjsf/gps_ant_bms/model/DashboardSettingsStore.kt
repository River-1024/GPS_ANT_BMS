package com.zjsf.gps_ant_bms.model

import android.content.Context
import android.graphics.Color

data class DashboardSettings(
    val powerMode: String = DashboardSettingsStore.POWER_MODE_BAR,
    val maxPower: Double = 1000.0,
    val rangeCoefficient: Double = 1.2,
    val valueTextScale: Double = 1.0,
    val dischargePowerBase: Double = 6000.0,
    val chargePowerBase: Double = 2500.0,
    val powerYellowRatio: Double = 0.55,
    val powerRedRatio: Double = 0.9,
    val socRedUpper: Int = 10,
    val socYellowUpper: Int = 20,
    val voltageDiffGreenUpper: Int = 20,
    val voltageDiffYellowUpper: Int = 50,
    val tempGreenUpper: Double = 39.0,
    val tempYellowUpper: Double = 45.0,
    val tempDisplayMode: String = DashboardSettingsStore.TEMP_MODE_ALL,
    val brightnessMode: String = DashboardSettingsStore.BRIGHTNESS_SYSTEM,
    val orientationMode: String = DashboardSettingsStore.ORIENTATION_LANDSCAPE,
    val socColor: Int = Color.rgb(57, 227, 108),
    val remainingColor: Int = Color.rgb(57, 227, 108),
    val rangeColor: Int = Color.rgb(57, 227, 108),
    val powerColor: Int = Color.rgb(57, 227, 108),
    val voltageColor: Int = Color.rgb(243, 250, 247),
    val currentColor: Int = Color.rgb(89, 216, 245),
    val diffColor: Int = Color.rgb(244, 200, 90),
    val tempColor: Int = Color.rgb(244, 200, 90),
    val sohColor: Int = Color.rgb(243, 250, 247)
)

object DashboardSettingsStore {
    const val POWER_MODE_BAR = "bar"
    const val POWER_MODE_CHART = "chart"
    const val TEMP_MODE_ALL = "all"
    const val TEMP_MODE_MOS = "mos"
    const val TEMP_MODE_BALANCER = "balancer"
    const val TEMP_MODE_MAX = "max"
    const val BRIGHTNESS_SYSTEM = "system"
    const val BRIGHTNESS_50 = "50"
    const val BRIGHTNESS_75 = "75"
    const val BRIGHTNESS_100 = "100"
    const val ORIENTATION_LANDSCAPE = "landscape"
    const val ORIENTATION_SENSOR = "sensor"

    const val MIN_MAX_POWER = 100.0
    const val MAX_MAX_POWER = 20000.0
    const val MIN_RANGE_COEFFICIENT = 0.0
    const val MAX_RANGE_COEFFICIENT = 100.0
    const val MIN_VALUE_TEXT_SCALE = 0.85
    const val MAX_VALUE_TEXT_SCALE = 1.15
    const val MIN_POWER_BASE = 100.0
    const val MAX_POWER_BASE = 50000.0
    const val MIN_RATIO = 0.0
    const val MAX_RATIO = 1.0

    private const val PREFS_NAME = "DashboardPrefs"
    private const val PREF_POWER_MODE = "power_mode"
    private const val PREF_MAX_POWER = "max_power"
    private const val PREF_RANGE_COEFFICIENT = "range_coefficient"
    private const val PREF_VALUE_TEXT_SCALE = "value_text_scale"
    private const val PREF_DISCHARGE_POWER_BASE = "discharge_power_base"
    private const val PREF_CHARGE_POWER_BASE = "charge_power_base"
    private const val PREF_POWER_YELLOW_RATIO = "power_yellow_ratio"
    private const val PREF_POWER_RED_RATIO = "power_red_ratio"
    private const val PREF_SOC_RED_UPPER = "soc_red_upper"
    private const val PREF_SOC_YELLOW_UPPER = "soc_yellow_upper"
    private const val PREF_VOLTAGE_DIFF_GREEN_UPPER = "voltage_diff_green_upper"
    private const val PREF_VOLTAGE_DIFF_YELLOW_UPPER = "voltage_diff_yellow_upper"
    private const val PREF_TEMP_GREEN_UPPER = "temp_green_upper"
    private const val PREF_TEMP_YELLOW_UPPER = "temp_yellow_upper"
    private const val PREF_TEMP_DISPLAY_MODE = "temp_display_mode"
    private const val PREF_BRIGHTNESS_MODE = "brightness_mode"
    private const val PREF_ORIENTATION_MODE = "orientation_mode"
    private const val PREF_SOC_COLOR = "soc_color"
    private const val PREF_REMAINING_COLOR = "remaining_color"
    private const val PREF_RANGE_COLOR = "range_color"
    private const val PREF_POWER_COLOR = "power_color"
    private const val PREF_VOLTAGE_COLOR = "voltage_color"
    private const val PREF_CURRENT_COLOR = "current_color"
    private const val PREF_DIFF_COLOR = "diff_color"
    private const val PREF_TEMP_COLOR = "temp_color"
    private const val PREF_SOH_COLOR = "soh_color"

    fun load(context: Context): DashboardSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = DashboardSettings()
        return DashboardSettings(
            powerMode = prefs.getString(PREF_POWER_MODE, defaults.powerMode) ?: defaults.powerMode,
            maxPower = prefs.getFloat(PREF_MAX_POWER, defaults.maxPower.toFloat()).toDouble(),
            rangeCoefficient = prefs.getFloat(PREF_RANGE_COEFFICIENT, defaults.rangeCoefficient.toFloat()).toDouble(),
            valueTextScale = prefs.getFloat(PREF_VALUE_TEXT_SCALE, defaults.valueTextScale.toFloat()).toDouble(),
            dischargePowerBase = prefs.getFloat(PREF_DISCHARGE_POWER_BASE, defaults.dischargePowerBase.toFloat()).toDouble(),
            chargePowerBase = prefs.getFloat(PREF_CHARGE_POWER_BASE, defaults.chargePowerBase.toFloat()).toDouble(),
            powerYellowRatio = prefs.getFloat(PREF_POWER_YELLOW_RATIO, defaults.powerYellowRatio.toFloat()).toDouble(),
            powerRedRatio = prefs.getFloat(PREF_POWER_RED_RATIO, defaults.powerRedRatio.toFloat()).toDouble(),
            socRedUpper = prefs.getInt(PREF_SOC_RED_UPPER, defaults.socRedUpper),
            socYellowUpper = prefs.getInt(PREF_SOC_YELLOW_UPPER, defaults.socYellowUpper),
            voltageDiffGreenUpper = prefs.getInt(PREF_VOLTAGE_DIFF_GREEN_UPPER, defaults.voltageDiffGreenUpper),
            voltageDiffYellowUpper = prefs.getInt(PREF_VOLTAGE_DIFF_YELLOW_UPPER, defaults.voltageDiffYellowUpper),
            tempGreenUpper = prefs.getFloat(PREF_TEMP_GREEN_UPPER, defaults.tempGreenUpper.toFloat()).toDouble(),
            tempYellowUpper = prefs.getFloat(PREF_TEMP_YELLOW_UPPER, defaults.tempYellowUpper.toFloat()).toDouble(),
            tempDisplayMode = prefs.getString(PREF_TEMP_DISPLAY_MODE, defaults.tempDisplayMode) ?: defaults.tempDisplayMode,
            brightnessMode = prefs.getString(PREF_BRIGHTNESS_MODE, defaults.brightnessMode) ?: defaults.brightnessMode,
            orientationMode = prefs.getString(PREF_ORIENTATION_MODE, defaults.orientationMode) ?: defaults.orientationMode,
            socColor = prefs.getInt(PREF_SOC_COLOR, defaults.socColor),
            remainingColor = prefs.getInt(PREF_REMAINING_COLOR, defaults.remainingColor),
            rangeColor = prefs.getInt(PREF_RANGE_COLOR, defaults.rangeColor),
            powerColor = prefs.getInt(PREF_POWER_COLOR, defaults.powerColor),
            voltageColor = prefs.getInt(PREF_VOLTAGE_COLOR, defaults.voltageColor),
            currentColor = prefs.getInt(PREF_CURRENT_COLOR, defaults.currentColor),
            diffColor = prefs.getInt(PREF_DIFF_COLOR, defaults.diffColor),
            tempColor = prefs.getInt(PREF_TEMP_COLOR, defaults.tempColor),
            sohColor = prefs.getInt(PREF_SOH_COLOR, defaults.sohColor)
        )
    }

    fun save(context: Context, settings: DashboardSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_POWER_MODE, settings.powerMode)
            .putFloat(PREF_MAX_POWER, settings.maxPower.toFloat())
            .putFloat(PREF_RANGE_COEFFICIENT, settings.rangeCoefficient.toFloat())
            .putFloat(PREF_VALUE_TEXT_SCALE, settings.valueTextScale.toFloat())
            .putFloat(PREF_DISCHARGE_POWER_BASE, settings.dischargePowerBase.toFloat())
            .putFloat(PREF_CHARGE_POWER_BASE, settings.chargePowerBase.toFloat())
            .putFloat(PREF_POWER_YELLOW_RATIO, settings.powerYellowRatio.toFloat())
            .putFloat(PREF_POWER_RED_RATIO, settings.powerRedRatio.toFloat())
            .putInt(PREF_SOC_RED_UPPER, settings.socRedUpper)
            .putInt(PREF_SOC_YELLOW_UPPER, settings.socYellowUpper)
            .putInt(PREF_VOLTAGE_DIFF_GREEN_UPPER, settings.voltageDiffGreenUpper)
            .putInt(PREF_VOLTAGE_DIFF_YELLOW_UPPER, settings.voltageDiffYellowUpper)
            .putFloat(PREF_TEMP_GREEN_UPPER, settings.tempGreenUpper.toFloat())
            .putFloat(PREF_TEMP_YELLOW_UPPER, settings.tempYellowUpper.toFloat())
            .putString(PREF_TEMP_DISPLAY_MODE, settings.tempDisplayMode)
            .putString(PREF_BRIGHTNESS_MODE, settings.brightnessMode)
            .putString(PREF_ORIENTATION_MODE, settings.orientationMode)
            .putInt(PREF_SOC_COLOR, settings.socColor)
            .putInt(PREF_REMAINING_COLOR, settings.remainingColor)
            .putInt(PREF_RANGE_COLOR, settings.rangeColor)
            .putInt(PREF_POWER_COLOR, settings.powerColor)
            .putInt(PREF_VOLTAGE_COLOR, settings.voltageColor)
            .putInt(PREF_CURRENT_COLOR, settings.currentColor)
            .putInt(PREF_DIFF_COLOR, settings.diffColor)
            .putInt(PREF_TEMP_COLOR, settings.tempColor)
            .putInt(PREF_SOH_COLOR, settings.sohColor)
            .apply()
    }
}
