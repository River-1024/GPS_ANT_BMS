package com.zjsf.gps_ant_bms.protocol

import com.zjsf.gps_ant_bms.model.BmsData

object AntProtocol {
    /**
     * 解析 BMS 状态帧数据 (基于 Python 示例 logic)
     * 起始 7E A1, 结束 AA 55
     */
    fun processAntData(data: ByteArray): BmsData? {
        // 数据长度检查 (Python 脚本要求 100 字节以上，根据 message.txt 约 140+ 字节)
        if (data.size < 100) return null
        
        // 帧类型检查: 第三字节通常是 0x11
        if (data[2] != 0x11.toByte()) return null

        try {
            // 小端序解析辅助函数
            fun u16(i: Int) = (data[i].toInt() and 0xFF) or ((data[i + 1].toInt() and 0xFF) shl 8)
            fun i16(i: Int) = u16(i).toShort().toInt()
            fun u32(i: Int) = (u16(i).toLong() and 0xFFFFL) or 
                             ((u16(i + 2).toLong() and 0xFFFFL) shl 16)
            fun i32(i: Int) = u32(i).toInt()
            fun hasBytes(offset: Int, count: Int) = offset >= 0 && offset + count <= data.size

            // 1. 读取基本配置信息
            val numTemp = data[8].toInt() and 0xFF
            val numCell = data[9].toInt() and 0xFF
            
            // 2. 解析各单体电压 (从 34 字节开始)
            var offset = 34
            val cellVoltages = mutableListOf<Int>()
            for (i in 0 until numCell) {
                cellVoltages.add(u16(offset)) // 保持 mV
                offset += 2
            }

            // 计算总电压 (mV 转 V) 和 电压差 (mV)
            val calculatedTotalVoltage = cellVoltages.sum().toDouble() / 1000.0
            val maxV = if (cellVoltages.isNotEmpty()) cellVoltages.maxOrNull() ?: 0 else 0
            val minV = if (cellVoltages.isNotEmpty()) cellVoltages.minOrNull() ?: 0 else 0
            val voltageDiff = maxV - minV
            val calculatedMaxCellIndex = cellVoltages.indexOf(maxV).takeIf { it >= 0 }?.plus(1)
            val calculatedMinCellIndex = cellVoltages.indexOf(minV).takeIf { it >= 0 }?.plus(1)
            val calculatedAverageCellVoltage =
                if (cellVoltages.isNotEmpty()) (cellVoltages.average()).toInt() else null

            // 3. 解析传感器温度
            val temperatures = mutableListOf<Int>()
            for (i in 0 until numTemp) {
                temperatures.add(i16(offset))
                offset += 2
            }

            // 4. MOSFET 和均衡器温度
            val mosTemp = i16(offset)
            val balancerTemp = i16(offset + 2)
            offset += 4

            // 5. 总电参数解析 (跳过原始总电压字段，使用计算出的值)
            // val totalVoltage = u16(offset) * 0.01 // V
            offset += 2

            val current = i16(offset) * 0.1 // A
            offset += 2

            val soc = u16(offset)
            offset += 2

            // 6. SOH、MOS 状态、均衡状态 (如果长度允许)
            var soh = 0
            var chargeMosOn: Boolean? = null
            var dischargeMosOn: Boolean? = null
            var balanceStatus: Int? = null
            if (hasBytes(offset, 6)) {
                soh = u16(offset)
                offset += 2
                chargeMosOn = (data[offset].toInt() and 0xFF) != 0
                offset += 1
                dischargeMosOn = (data[offset].toInt() and 0xFF) != 0
                offset += 1
                balanceStatus = data[offset].toInt() and 0xFF
                offset += 1
                offset += 1 // 保留字节
            }

            // 7. 容量与运行时间 (如果长度允许)
            var capacity = 0.0
            var remainingCharge = 0.0
            var cycleCapacity = 0.0
            var power = 0.0
            var runtime = 0L
            var balanceMask = 0L
            var protocolMaxCellVoltage: Int? = null
            var protocolMaxCellIndex: Int? = null
            var protocolMinCellVoltage: Int? = null
            var protocolMinCellIndex: Int? = null
            var protocolVoltageDiff: Int? = null
            var protocolAverageCellVoltage: Int? = null
            var totalDischargeCapacity = 0.0
            var totalChargeCapacity = 0.0
            var totalDischargeTime = 0L
            var totalChargeTime = 0L

            if (hasBytes(offset, 20)) {
                capacity = u32(offset) * 0.000001
                offset += 4
                remainingCharge = u32(offset) * 0.000001
                offset += 4
                
                cycleCapacity = u32(offset) * 0.001
                offset += 4
                
                // BMS 反馈的当前功率
                val pRaw = i32(offset)
                power = pRaw.toDouble()
                offset += 4
                
                // 累计运行时间
                runtime = u32(offset)
                offset += 4
            }

            if (hasBytes(offset, 4)) {
                balanceMask = u32(offset)
                offset += 4
            }

            if (hasBytes(offset, 12)) {
                protocolMaxCellVoltage = u16(offset)
                offset += 2
                protocolMaxCellIndex = u16(offset).takeIf { it > 0 }
                offset += 2
                protocolMinCellVoltage = u16(offset)
                offset += 2
                protocolMinCellIndex = u16(offset).takeIf { it > 0 }
                offset += 2
                protocolVoltageDiff = u16(offset)
                offset += 2
                protocolAverageCellVoltage = u16(offset)
                offset += 2
            }

            // 放 MOS Vds、放 MOS 驱动电压、充 MOS 驱动电压、通讯状态、电池类型。
            if (hasBytes(offset, 10)) {
                offset += 10
            }

            if (hasBytes(offset, 16)) {
                totalDischargeCapacity = u32(offset) * 0.001
                offset += 4
                totalChargeCapacity = u32(offset) * 0.001
                offset += 4
                totalDischargeTime = u32(offset)
                offset += 4
                totalChargeTime = u32(offset)
                offset += 4
            }

            val dataLength = data.getOrNull(5)?.toInt()?.and(0xFF)
            val dataEnd = dataLength?.let { 6 + it }?.coerceAtMost(data.size) ?: data.size
            val unparsedBytes = (dataEnd - offset).coerceAtLeast(0)
            val statusCode = data.getOrNull(7)?.toInt()?.and(0xFF)
            val statusText = when (statusCode) {
                1 -> "待机"
                2 -> "充电中"
                3 -> "放电中"
                4 -> "休眠"
                5 -> "错误"
                else -> "未知"
            }

            return BmsData(
                totalVoltage = calculatedTotalVoltage,
                current = current,
                soc = soc,
                capacity = capacity,
                remainingCharge = remainingCharge,
                mosTemp = mosTemp,
                balancerTemp = balancerTemp,
                cellVoltages = cellVoltages,
                temperatures = temperatures,
                soh = soh,
                power = power,
                runtime = runtime,
                voltageDiff = protocolVoltageDiff ?: voltageDiff,
                chargeMosOn = chargeMosOn,
                dischargeMosOn = dischargeMosOn,
                balanceStatus = balanceStatus,
                balanceMask = balanceMask,
                maxCellVoltage = protocolMaxCellVoltage ?: maxV.takeIf { it > 0 },
                maxCellIndex = protocolMaxCellIndex ?: calculatedMaxCellIndex,
                minCellVoltage = protocolMinCellVoltage ?: minV.takeIf { it > 0 },
                minCellIndex = protocolMinCellIndex ?: calculatedMinCellIndex,
                averageCellVoltage = protocolAverageCellVoltage ?: calculatedAverageCellVoltage,
                cycleCapacity = cycleCapacity,
                totalDischargeCapacity = totalDischargeCapacity,
                totalChargeCapacity = totalChargeCapacity,
                totalDischargeTime = totalDischargeTime,
                totalChargeTime = totalChargeTime,
                bmsStatusCode = statusCode,
                bmsStatusText = statusText,
                frameLength = data.size,
                unparsedBytes = unparsedBytes
            )
        } catch (e: Exception) {
            return null
        }
    }
}
