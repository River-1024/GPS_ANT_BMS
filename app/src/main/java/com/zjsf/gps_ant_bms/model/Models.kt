package com.zjsf.gps_ant_bms.model

data class BleDevice(val name: String, val address: String, val rssi: Int)

data class BmsData(
    val totalVoltage: Double,
    val current: Double,
    val soc: Int,
    val capacity: Double,
    val remainingCharge: Double,
    val mosTemp: Int,
    val balancerTemp: Int,
    val cellVoltages: List<Int>,
    val temperatures: List<Int>,
    val soh: Int = 0,
    val power: Double = 0.0,
    val runtime: Long = 0L,
    val voltageDiff: Int = 0,
    val chargeMosOn: Boolean? = null,
    val dischargeMosOn: Boolean? = null,
    val balanceStatus: Int? = null,
    val balanceMask: Long = 0L,
    val maxCellVoltage: Int? = null,
    val maxCellIndex: Int? = null,
    val minCellVoltage: Int? = null,
    val minCellIndex: Int? = null,
    val averageCellVoltage: Int? = null,
    val cycleCapacity: Double = 0.0,
    val totalDischargeCapacity: Double = 0.0,
    val totalChargeCapacity: Double = 0.0,
    val totalDischargeTime: Long = 0L,
    val totalChargeTime: Long = 0L,
    val bmsStatusCode: Int? = null,
    val bmsStatusText: String = "未知",
    val frameLength: Int = 0,
    val unparsedBytes: Int = 0
)
