package com.zjsf.gps_ant_bms.model

import java.util.ArrayDeque

data class DashboardData(
    val speed: Double = 0.0,
    val voltage: Double = 0.0,
    val current: Double = 0.0,
    val voltageDiff: Int = 0,
    val soc: Int = 0,
    val power: Double = 0.0,
    val capacity: Double = 0.0,
    val remainingCharge: Double = 0.0,
    val mosTemp: Int = 0,
    val balancerTemp: Int = 0,
    val sensorTemps: List<Int> = emptyList(),
    val soh: Int = 0,
    val lastUpdatedMillis: Long = 0L,
    val isBmsConnected: Boolean = false,
    val hasBmsData: Boolean = false,
    val hasGpsData: Boolean = false
)

object BmsLiveDataStore {
    private const val MAX_POWER_POINTS = 120

    private val lock = Any()
    private val powerHistory = ArrayDeque<Double>()
    private var data = DashboardData()

    fun updateSpeed(speed: Double) {
        synchronized(lock) {
            data = data.copy(
                speed = speed,
                hasGpsData = true,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        }
    }

    fun updateBmsData(bmsData: BmsData) {
        synchronized(lock) {
            data = data.copy(
                voltage = bmsData.totalVoltage,
                current = bmsData.current,
                voltageDiff = bmsData.voltageDiff,
                soc = bmsData.soc,
                power = bmsData.power,
                capacity = bmsData.capacity,
                remainingCharge = bmsData.remainingCharge,
                mosTemp = bmsData.mosTemp,
                balancerTemp = bmsData.balancerTemp,
                sensorTemps = bmsData.temperatures,
                soh = bmsData.soh,
                isBmsConnected = true,
                hasBmsData = true,
                lastUpdatedMillis = System.currentTimeMillis()
            )

            powerHistory.addLast(bmsData.power)
            while (powerHistory.size > MAX_POWER_POINTS) {
                powerHistory.removeFirst()
            }
        }
    }

    fun updateBmsConnectionState(isConnected: Boolean) {
        synchronized(lock) {
            data = data.copy(
                isBmsConnected = isConnected,
                hasBmsData = if (isConnected) data.hasBmsData else false
            )
            if (!isConnected) {
                powerHistory.clear()
            }
        }
    }

    fun snapshot(): DashboardData = synchronized(lock) { data }

    fun powerHistorySnapshot(): List<Double> = synchronized(lock) { powerHistory.toList() }
}
