package com.zjsf.gps_ant_bms

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zjsf.gps_ant_bms.model.DashboardSettings
import com.zjsf.gps_ant_bms.model.DashboardSettingsStore

class DashboardSettingsActivity : AppCompatActivity() {

    private lateinit var editMaxPower: EditText
    private lateinit var editRangeCoefficient: EditText
    private lateinit var editValueTextScale: EditText
    private lateinit var editDischargePowerBase: EditText
    private lateinit var editChargePowerBase: EditText
    private lateinit var editPowerYellowRatio: EditText
    private lateinit var editPowerRedRatio: EditText
    private lateinit var editSocRedUpper: EditText
    private lateinit var editSocYellowUpper: EditText
    private lateinit var editVoltageDiffGreenUpper: EditText
    private lateinit var editVoltageDiffYellowUpper: EditText
    private lateinit var editTempGreenUpper: EditText
    private lateinit var editTempYellowUpper: EditText
    private lateinit var radioPowerMode: RadioGroup
    private lateinit var radioPowerBar: RadioButton
    private lateinit var radioPowerChart: RadioButton
    private lateinit var spinnerTempDisplayMode: Spinner
    private lateinit var spinnerBrightness: Spinner
    private lateinit var spinnerOrientation: Spinner
    private lateinit var editSocColor: EditText
    private lateinit var editRemainingColor: EditText
    private lateinit var editRangeColor: EditText
    private lateinit var editPowerColor: EditText
    private lateinit var editVoltageColor: EditText
    private lateinit var editCurrentColor: EditText
    private lateinit var editDiffColor: EditText
    private lateinit var editTempColor: EditText
    private lateinit var editSohColor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_settings)
        initViews()
        bindSpinners()
        bindSettings(DashboardSettingsStore.load(this))
        findViewById<Button>(R.id.buttonSave).setOnClickListener { saveSettings() }
        findViewById<Button>(R.id.buttonCancel).setOnClickListener { finish() }
    }

    private fun initViews() {
        editMaxPower = findViewById(R.id.editMaxPower)
        editRangeCoefficient = findViewById(R.id.editRangeCoefficient)
        editValueTextScale = findViewById(R.id.editValueTextScale)
        editDischargePowerBase = findViewById(R.id.editDischargePowerBase)
        editChargePowerBase = findViewById(R.id.editChargePowerBase)
        editPowerYellowRatio = findViewById(R.id.editPowerYellowRatio)
        editPowerRedRatio = findViewById(R.id.editPowerRedRatio)
        editSocRedUpper = findViewById(R.id.editSocRedUpper)
        editSocYellowUpper = findViewById(R.id.editSocYellowUpper)
        editVoltageDiffGreenUpper = findViewById(R.id.editVoltageDiffGreenUpper)
        editVoltageDiffYellowUpper = findViewById(R.id.editVoltageDiffYellowUpper)
        editTempGreenUpper = findViewById(R.id.editTempGreenUpper)
        editTempYellowUpper = findViewById(R.id.editTempYellowUpper)
        radioPowerMode = findViewById(R.id.radioPowerMode)
        radioPowerBar = findViewById(R.id.radioPowerBar)
        radioPowerChart = findViewById(R.id.radioPowerChart)
        spinnerTempDisplayMode = findViewById(R.id.spinnerTempDisplayMode)
        spinnerBrightness = findViewById(R.id.spinnerBrightness)
        spinnerOrientation = findViewById(R.id.spinnerOrientation)
        editSocColor = findViewById(R.id.editSocColor)
        editRemainingColor = findViewById(R.id.editRemainingColor)
        editRangeColor = findViewById(R.id.editRangeColor)
        editPowerColor = findViewById(R.id.editPowerColor)
        editVoltageColor = findViewById(R.id.editVoltageColor)
        editCurrentColor = findViewById(R.id.editCurrentColor)
        editDiffColor = findViewById(R.id.editDiffColor)
        editTempColor = findViewById(R.id.editTempColor)
        editSohColor = findViewById(R.id.editSohColor)
    }

    private fun bindSpinners() {
        spinnerTempDisplayMode.adapter = spinnerAdapter(TEMP_MODE_OPTIONS.map { it.label })
        spinnerBrightness.adapter = spinnerAdapter(BRIGHTNESS_OPTIONS.map { it.label })
        spinnerOrientation.adapter = spinnerAdapter(ORIENTATION_OPTIONS.map { it.label })
    }

    private fun bindSettings(settings: DashboardSettings) {
        editMaxPower.setText(settings.maxPower.toInt().toString())
        editRangeCoefficient.setText(settings.rangeCoefficient.toString())
        editValueTextScale.setText("%.2f".format(settings.valueTextScale))
        editDischargePowerBase.setText(settings.dischargePowerBase.toInt().toString())
        editChargePowerBase.setText(settings.chargePowerBase.toInt().toString())
        editPowerYellowRatio.setText("%.2f".format(settings.powerYellowRatio))
        editPowerRedRatio.setText("%.2f".format(settings.powerRedRatio))
        editSocRedUpper.setText(settings.socRedUpper.toString())
        editSocYellowUpper.setText(settings.socYellowUpper.toString())
        editVoltageDiffGreenUpper.setText(settings.voltageDiffGreenUpper.toString())
        editVoltageDiffYellowUpper.setText(settings.voltageDiffYellowUpper.toString())
        editTempGreenUpper.setText("%.1f".format(settings.tempGreenUpper))
        editTempYellowUpper.setText("%.1f".format(settings.tempYellowUpper))
        spinnerTempDisplayMode.setSelection(indexOfValue(TEMP_MODE_OPTIONS, settings.tempDisplayMode))
        spinnerBrightness.setSelection(indexOfValue(BRIGHTNESS_OPTIONS, settings.brightnessMode))
        spinnerOrientation.setSelection(indexOfValue(ORIENTATION_OPTIONS, settings.orientationMode))
        radioPowerMode.check(
            if (settings.powerMode == DashboardSettingsStore.POWER_MODE_CHART) {
                R.id.radioPowerChart
            } else {
                R.id.radioPowerBar
            }
        )
        editSocColor.setText(formatColor(settings.socColor))
        editRemainingColor.setText(formatColor(settings.remainingColor))
        editRangeColor.setText(formatColor(settings.rangeColor))
        editPowerColor.setText(formatColor(settings.powerColor))
        editVoltageColor.setText(formatColor(settings.voltageColor))
        editCurrentColor.setText(formatColor(settings.currentColor))
        editDiffColor.setText(formatColor(settings.diffColor))
        editTempColor.setText(formatColor(settings.tempColor))
        editSohColor.setText(formatColor(settings.sohColor))
    }

    private fun saveSettings() {
        val maxPower = editMaxPower.text.toString().toDoubleOrNull()
        if (maxPower == null || maxPower < DashboardSettingsStore.MIN_MAX_POWER || maxPower > DashboardSettingsStore.MAX_MAX_POWER) {
            toast("功率满量程范围 ${DashboardSettingsStore.MIN_MAX_POWER.toInt()}-${DashboardSettingsStore.MAX_MAX_POWER.toInt()}W")
            return
        }

        val rangeCoefficient = editRangeCoefficient.text.toString().toDoubleOrNull()
        if (rangeCoefficient == null || rangeCoefficient < DashboardSettingsStore.MIN_RANGE_COEFFICIENT || rangeCoefficient > DashboardSettingsStore.MAX_RANGE_COEFFICIENT) {
            toast("续航系数范围 ${DashboardSettingsStore.MIN_RANGE_COEFFICIENT}-${DashboardSettingsStore.MAX_RANGE_COEFFICIENT}")
            return
        }

        val valueTextScale = editValueTextScale.text.toString().toDoubleOrNull()
        if (
            valueTextScale == null ||
            valueTextScale < DashboardSettingsStore.MIN_VALUE_TEXT_SCALE ||
            valueTextScale > DashboardSettingsStore.MAX_VALUE_TEXT_SCALE
        ) {
            toast("数值大小比例范围 ${DashboardSettingsStore.MIN_VALUE_TEXT_SCALE}-${DashboardSettingsStore.MAX_VALUE_TEXT_SCALE}")
            return
        }

        val dischargePowerBase = parseDoubleInRange(
            editDischargePowerBase,
            DashboardSettingsStore.MIN_POWER_BASE,
            DashboardSettingsStore.MAX_POWER_BASE,
            "放电功率条基数"
        ) ?: return
        val chargePowerBase = parseDoubleInRange(
            editChargePowerBase,
            DashboardSettingsStore.MIN_POWER_BASE,
            DashboardSettingsStore.MAX_POWER_BASE,
            "充电功率条基数"
        ) ?: return
        val powerYellowRatio = parseDoubleInRange(
            editPowerYellowRatio,
            DashboardSettingsStore.MIN_RATIO,
            DashboardSettingsStore.MAX_RATIO,
            "功率条黄色起点"
        ) ?: return
        val powerRedRatio = parseDoubleInRange(
            editPowerRedRatio,
            DashboardSettingsStore.MIN_RATIO,
            DashboardSettingsStore.MAX_RATIO,
            "功率条红色起点"
        ) ?: return
        if (powerYellowRatio >= powerRedRatio) {
            toast("功率条黄色起点必须小于红色起点")
            return
        }

        val socRedUpper = parseIntInRange(editSocRedUpper, 0, 100, "电量红色上限") ?: return
        val socYellowUpper = parseIntInRange(editSocYellowUpper, 0, 100, "电量黄色上限") ?: return
        if (socRedUpper >= socYellowUpper) {
            toast("电量红色上限必须小于黄色上限")
            return
        }

        val voltageDiffGreenUpper = parseIntInRange(editVoltageDiffGreenUpper, 0, 1000, "压差绿色上限") ?: return
        val voltageDiffYellowUpper = parseIntInRange(editVoltageDiffYellowUpper, 0, 1000, "压差黄色上限") ?: return
        if (voltageDiffGreenUpper >= voltageDiffYellowUpper) {
            toast("压差绿色上限必须小于黄色上限")
            return
        }

        val tempGreenUpper = parseDoubleInRange(editTempGreenUpper, -40.0, 120.0, "温度绿色上限") ?: return
        val tempYellowUpper = parseDoubleInRange(editTempYellowUpper, -40.0, 120.0, "温度黄色上限") ?: return
        if (tempGreenUpper >= tempYellowUpper) {
            toast("温度绿色上限必须小于黄色上限")
            return
        }

        val settings = try {
            DashboardSettings(
                powerMode = if (radioPowerChart.isChecked) DashboardSettingsStore.POWER_MODE_CHART else DashboardSettingsStore.POWER_MODE_BAR,
                maxPower = maxPower,
                rangeCoefficient = rangeCoefficient,
                valueTextScale = valueTextScale,
                dischargePowerBase = dischargePowerBase,
                chargePowerBase = chargePowerBase,
                powerYellowRatio = powerYellowRatio,
                powerRedRatio = powerRedRatio,
                socRedUpper = socRedUpper,
                socYellowUpper = socYellowUpper,
                voltageDiffGreenUpper = voltageDiffGreenUpper,
                voltageDiffYellowUpper = voltageDiffYellowUpper,
                tempGreenUpper = tempGreenUpper,
                tempYellowUpper = tempYellowUpper,
                tempDisplayMode = selectedValue(TEMP_MODE_OPTIONS, spinnerTempDisplayMode),
                brightnessMode = selectedValue(BRIGHTNESS_OPTIONS, spinnerBrightness),
                orientationMode = selectedValue(ORIENTATION_OPTIONS, spinnerOrientation),
                socColor = parseColor(editSocColor),
                remainingColor = parseColor(editRemainingColor),
                rangeColor = parseColor(editRangeColor),
                powerColor = parseColor(editPowerColor),
                voltageColor = parseColor(editVoltageColor),
                currentColor = parseColor(editCurrentColor),
                diffColor = parseColor(editDiffColor),
                tempColor = parseColor(editTempColor),
                sohColor = parseColor(editSohColor)
            )
        } catch (e: IllegalArgumentException) {
            toast("颜色格式必须是 #RRGGBB")
            return
        }

        DashboardSettingsStore.save(this, settings)
        toast("已保存")
        finish()
    }

    private fun parseColor(editText: EditText): Int {
        val raw = editText.text.toString().trim()
        if (!Regex("^#[0-9a-fA-F]{6}$").matches(raw)) {
            throw IllegalArgumentException("Invalid color")
        }
        return Color.parseColor(raw)
    }

    private fun formatColor(color: Int): String = "#%06X".format(0xFFFFFF and color)

    private fun spinnerAdapter(labels: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun parseDoubleInRange(editText: EditText, min: Double, max: Double, label: String): Double? {
        val value = editText.text.toString().toDoubleOrNull()
        if (value == null || value < min || value > max) {
            toast("$label 范围 $min-$max")
            return null
        }
        return value
    }

    private fun parseIntInRange(editText: EditText, min: Int, max: Int, label: String): Int? {
        val value = editText.text.toString().toIntOrNull()
        if (value == null || value < min || value > max) {
            toast("$label 范围 $min-$max")
            return null
        }
        return value
    }

    private fun indexOfValue(options: List<Option>, value: String): Int {
        return options.indexOfFirst { it.value == value }.takeIf { it >= 0 } ?: 0
    }

    private fun selectedValue(options: List<Option>, spinner: Spinner): String {
        return options[spinner.selectedItemPosition.coerceIn(options.indices)].value
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private data class Option(val label: String, val value: String)

    companion object {
        private val TEMP_MODE_OPTIONS = listOf(
            Option("全部温度", DashboardSettingsStore.TEMP_MODE_ALL),
            Option("MOS 温度", DashboardSettingsStore.TEMP_MODE_MOS),
            Option("均衡温度", DashboardSettingsStore.TEMP_MODE_BALANCER),
            Option("最高温度", DashboardSettingsStore.TEMP_MODE_MAX)
        )
        private val BRIGHTNESS_OPTIONS = listOf(
            Option("跟随系统", DashboardSettingsStore.BRIGHTNESS_SYSTEM),
            Option("50%", DashboardSettingsStore.BRIGHTNESS_50),
            Option("75%", DashboardSettingsStore.BRIGHTNESS_75),
            Option("100%", DashboardSettingsStore.BRIGHTNESS_100)
        )
        private val ORIENTATION_OPTIONS = listOf(
            Option("横屏固定", DashboardSettingsStore.ORIENTATION_LANDSCAPE),
            Option("跟随系统", DashboardSettingsStore.ORIENTATION_SENSOR)
        )
    }
}
