# GPS ANT BMS

GPS ANT BMS 是一个 Android 仪表盘应用，用来连接蚂蚁保护板 ANT BMS，读取蓝牙数据，并把电池状态、GPS 速度和功率变化显示在横屏仪表盘上。

项目的核心链路是：

1. 扫描附近 BLE 设备，选择 ANT BMS 蓝牙设备。
2. 连接保护板的 `FFE0/FFE1` GATT 通道。
3. 周期性写入 ANT BMS 查询命令。
4. 接收保护板通过 Notify 返回的分片数据。
5. 合并完整帧后解析电压、电流、SOC、温度、容量、均衡状态等字段。
6. 在 Compose 仪表盘和悬浮窗中展示实时数据。

## 主要功能

- 蓝牙扫描与连接：发现附近 BLE 设备，按 RSSI 排序，选择后连接保护板。
- 自动重连：保存上次连接设备地址，启动后尝试恢复连接。
- BMS 实时读取：周期查询保护板数据，解析电池组总压、电流、SOC、SOH、容量、单体电压、温度、压差和功率。
- GPS 速度显示：读取手机定位速度，换算为 km/h 后显示在仪表盘。
- 横屏仪表盘：用 Jetpack Compose 展示电池状态、功率条、曲线、蓝牙信号和运行状态。
- 悬浮窗：可选择在前台服务中显示速度、电压、电流、压差、SOC 等关键数据。
- 预览模式：未连接保护板时使用模拟数据，方便调试界面。
- 参数设置：支持刷新间隔、功率显示方式、颜色、SOC/压差/温度阈值等配置。

## 项目结构

| 路径 | 作用 |
| --- | --- |
| `app/src/main/java/com/zjsf/gps_ant_bms/MainActivity.kt` | 主界面、权限、GPS、蓝牙连接、数据流转 |
| `app/src/main/java/com/zjsf/gps_ant_bms/bluetooth/BleScanner.kt` | BLE 扫描 |
| `app/src/main/java/com/zjsf/gps_ant_bms/bluetooth/BmsBluetoothManager.kt` | ANT BMS GATT 连接、Notify、轮询、RSSI |
| `app/src/main/java/com/zjsf/gps_ant_bms/protocol/AntProtocol.kt` | ANT BMS 响应帧解析 |
| `app/src/main/java/com/zjsf/gps_ant_bms/model/Models.kt` | BLE 设备与 BMS 数据模型 |
| `app/src/main/java/com/zjsf/gps_ant_bms/model/DashboardModels.kt` | 仪表盘状态、设置、预览数据 |
| `app/src/main/java/com/zjsf/gps_ant_bms/ui/DashboardScreens.kt` | Compose 仪表盘界面 |
| `app/src/main/java/com/zjsf/gps_ant_bms/FloatingWindowService.kt` | 悬浮窗前台服务 |
| `message.txt` | NRF Connect 抓取的 ANT BMS 蓝牙通信样本 |
| `example.py` | 早期 Python 协议解析参考 |

## 使用步骤

### 1. 手机准备

1. 使用 Android 10 或更高版本手机。
2. 打开蓝牙和定位。
3. 给应用授予定位、蓝牙扫描、蓝牙连接权限。
4. 如果要使用悬浮窗，在系统设置中允许应用显示在其他应用上层。

### 2. 安装 APK

可以从 GitHub Actions 构建产物下载 debug APK，也可以本地构建。

GitHub Actions 路径：

1. 推送代码到 `main` 或 `master`，或者在 Actions 页面手动运行 `Build APK`。
2. 构建完成后，在 workflow artifact 中下载 `GPS_ANT_BMS-debug-apk`。
3. 将 APK 安装到手机。

本机 PowerShell 构建：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-26.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
.\gradlew.bat assembleDebug
```

生成位置通常为：

```text
app/build/outputs/apk/debug/
```

### 3. 连接保护板

1. 打开应用。
2. 点击扫描蓝牙设备。
3. 在列表中选择 ANT BMS 设备。
4. 连接成功后，应用会开启 Notify 并开始周期轮询。
5. 仪表盘从预览模式切换为实时模式。

### 4. 常用设置

- 蓝牙刷新间隔：默认 1000 ms，代码中限制范围为 200 ms 到 60000 ms。
- 功率显示：支持功率条和曲线。
- 阈值颜色：可设置 SOC、压差、温度、功率区间颜色。
- 悬浮窗：开启后通过前台服务显示关键数据。
- 从最近任务隐藏：可选择让应用不显示在最近任务列表中。

## ANT BMS 蓝牙协议说明

下面的协议说明基于当前 Android 代码、`message.txt` 中的 NRF Connect 通信记录，以及 `example.py` 的解析逻辑整理。不同固件版本可能存在字段扩展或偏移差异，新增字段应先用真实抓包验证。

### 保护板广播与基础信息

样本设备名：

```text
ANT@BLE24CBUB-8FQR
```

样本 MAC：

```text
5A:D8:29:5F:DD:15
```

NRF Connect 样本中发现的主要服务和特征如下：

| UUID | 类型 | 权限 | 当前项目用途 |
| --- | --- | --- | --- |
| `0000ffe0-0000-1000-8000-00805f9b34fb` | Service | Unknown Service | ANT BMS 主服务 |
| `0000ffe1-0000-1000-8000-00805f9b34fb` | Characteristic | Notify / Read / Write / Write No Response | 当前项目的读写和通知通道 |
| `00002902-0000-1000-8000-00805f9b34fb` | Descriptor | Client Characteristic Configuration | 写入 `01 00` 开启 Notify |
| `0000fff1-0000-1000-8000-00805f9b34fb` | Characteristic | Write / Write No Response | 样本中存在，当前项目未使用 |
| `0000fff2-0000-1000-8000-00805f9b34fb` | Characteristic | Notify / Read / Write / Write No Response | 样本中存在，当前项目未使用 |
| `0000fff3-0000-1000-8000-00805f9b34fb` | Characteristic | Write / Write No Response | 样本中存在，当前项目未使用 |
| `0000fff4-0000-1000-8000-00805f9b34fb` | Characteristic | Notify / Read / Write / Write No Response | 样本中存在，当前项目未使用 |

当前 Android 实现只依赖 `FFE0` 服务和 `FFE1` 特征。

### BLE 连接参数与 GATT 配置

连接方式：

```kotlin
device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
```

连接成功后的操作顺序：

1. 调用 `discoverServices()` 发现服务。
2. 获取 `FFE0` 服务。
3. 获取 `FFE1` 特征。
4. 调用 `setCharacteristicNotification(characteristic, true)`。
5. 向 `00002902-0000-1000-8000-00805f9b34fb` 写入 `01 00`。
6. Notify 开启成功后启动周期轮询。

NRF Connect 样本中连接参数曾更新为：

| 参数 | 样本值 |
| --- | --- |
| connection interval | 7.5 ms、30.0 ms、25.0 ms |
| latency | 0 或 2 |
| supervision timeout | 5000 ms 或 2000 ms |

这些连接参数由 Android 蓝牙栈和设备协商，项目代码没有手动设置。

### App 与保护板的交互顺序

一次完整读取流程如下：

```text
手机连接 ANT BMS
  -> 发现服务 FFE0
  -> 找到特征 FFE1
  -> 写 CCCD 01 00 开启通知
  -> 写入查询命令 7E A1 01 00 00 BE 18 55 AA 55
  -> 保护板通过 FFE1 Notify 返回多段数据
  -> App 按帧头和帧尾合并完整响应
  -> AntProtocol.processAntData() 解析为 BmsData
  -> UI 刷新仪表盘和悬浮窗
```

当前代码默认每 1000 ms 查询一次，设置页可调整。`BmsBluetoothManager.setPollingInterval()` 会把间隔限制在 200 ms 到 60000 ms。

### 查询命令帧

当前项目发送的查询命令固定为：

```text
7E A1 01 00 00 BE 18 55 AA 55
```

按当前样本理解：

| 偏移 | 字节 | 含义 |
| --- | --- | --- |
| 0 | `7E` | 帧头第 1 字节 |
| 1 | `A1` | 帧头第 2 字节 |
| 2 | `01` | 查询命令类型 |
| 3-4 | `00 00` | 命令参数或保留位 |
| 5-6 | `BE 18` | 校验或命令附加字段，当前代码固定发送 |
| 7 | `55` | 命令分隔或结束标记的一部分 |
| 8-9 | `AA 55` | 帧尾 |

项目没有动态计算查询命令校验值，而是使用抓包确认可用的固定命令。

### 响应帧总体格式

保护板的状态响应可能通过多个 BLE Notify 分片返回。当前代码的合包规则是：

- 如果收到的数据以 `7E A1` 开头，清空旧缓存并开始新帧。
- 每个 Notify 分片追加到缓存。
- 如果缓存最后两个字节是 `AA 55`，认为完整帧结束。
- 将完整帧交给 `AntProtocol.processAntData()`。

状态响应帧的通用结构如下：

| 偏移 | 长度 | 示例 | 含义 |
| --- | --- | --- | --- |
| 0 | 1 | `7E` | 帧头 |
| 1 | 1 | `A1` | 帧头 |
| 2 | 1 | `11` | 状态响应类型，当前解析器只接受 `0x11` |
| 3-4 | 2 | `00 00` | 保留或序号字段 |
| 5 | 1 | `A8` | 数据区长度，样本为 168 字节 |
| 6 起 | N | 见下文 | 数据区 |
| 数据区后 | 2 | 样本 `C4 07` | 可能为校验字段，当前项目未校验 |
| 末尾 | 2 | `AA 55` | 帧尾 |

当前解析器要求响应长度至少 100 字节，并且第 3 个字节必须是 `0x11`。

### 状态响应的数据区字段

当前解析逻辑使用小端序读取多字节整数。

读取辅助规则：

| 类型 | 字节数 | Kotlin 逻辑 | 说明 |
| --- | --- | --- | --- |
| `u16` | 2 | 低字节在前 | 无符号 16 位 |
| `i16` | 2 | 低字节在前，转有符号 | 有符号 16 位 |
| `u32` | 4 | 两个 `u16` 组合 | 无符号 32 位 |
| `i32` | 4 | `u32` 转有符号 | 有符号 32 位 |

固定头部字段：

| 偏移 | 长度 | 解析 | 说明 |
| --- | --- | --- | --- |
| 7 | 1 | `statusCode` | BMS 状态码 |
| 8 | 1 | `numTemp` | 温度传感器数量 |
| 9 | 1 | `numCell` | 电芯串数 |
| 34 | `numCell * 2` | `u16` 列表 | 单体电压，单位 mV |

状态码映射：

| 值 | 当前显示 |
| --- | --- |
| 1 | 待机 |
| 2 | 充电中 |
| 3 | 放电中 |
| 4 | 休眠 |
| 5 | 错误 |
| 其他 | 未知 |

单体电压之后的字段按动态偏移解析：

| 顺序 | 长度 | 解析 | 单位/含义 |
| --- | --- | --- | --- |
| 温度探头 | `numTemp * 2` | `i16` 列表 | 摄氏度 |
| MOS 温度 | 2 | `i16` | 摄氏度 |
| 均衡器温度 | 2 | `i16` | 摄氏度 |
| 原始总电压 | 2 | `u16 * 0.01` | V，当前 Kotlin 最终使用单体电压求和作为总压 |
| 当前电流 | 2 | `i16 * 0.1` | A |
| SOC | 2 | `u16` | 百分比 |
| SOH | 2 | `u16` | 百分比 |
| 充电 MOS | 1 | 非 0 为开 | Boolean |
| 放电 MOS | 1 | 非 0 为开 | Boolean |
| 均衡状态 | 1 | 原始枚举值 | 0/1/2 等 |
| 保留 | 1 | 跳过 | 保留 |
| 设计容量 | 4 | `u32 * 0.000001` | Ah |
| 剩余容量 | 4 | `u32 * 0.000001` | Ah |
| 循环容量 | 4 | `u32 * 0.001` | 当前模型字段名为 `cycleCapacity` |
| 实时功率 | 4 | `i32` | W |
| 累计运行时间 | 4 | `u32` | 秒 |
| 均衡位图 | 4 | `u32` | 每 bit 可对应一个电芯均衡状态 |
| 最高单体电压 | 2 | `u16` | mV |
| 最高单体序号 | 2 | `u16` | 从 1 开始 |
| 最低单体电压 | 2 | `u16` | mV |
| 最低单体序号 | 2 | `u16` | 从 1 开始 |
| 最大压差 | 2 | `u16` | mV |
| 平均单体电压 | 2 | `u16` | mV |
| MOS/通信/电池类型扩展 | 10 | 跳过 | 放 MOS Vds、驱动电压、通信状态、电池类型等，当前未展开 |
| 累计放电容量 | 4 | `u32 * 0.001` | 当前模型字段名为 `totalDischargeCapacity` |
| 累计充电容量 | 4 | `u32 * 0.001` | 当前模型字段名为 `totalChargeCapacity` |
| 累计放电时间 | 4 | `u32` | 秒 |
| 累计充电时间 | 4 | `u32` | 秒 |

解析器还会计算或补齐：

- `totalVoltage`：当前 Kotlin 使用所有单体电压求和，单位 V。
- `voltageDiff`：优先使用协议字段，缺失时用最高单体减最低单体。
- `maxCellVoltage` / `minCellVoltage`：优先使用协议字段，缺失时由单体列表计算。
- `averageCellVoltage`：优先使用协议字段，缺失时由单体列表计算。
- `displayPower()`：如果协议功率不为 0，显示协议功率，否则用 `totalVoltage * current`。

### 帧类型与当前支持范围

| 类型字节 | 方向 | 当前理解 |
| --- | --- | --- |
| `0x01` | 手机到 BMS | 查询状态命令 |
| `0x11` | BMS 到手机 | 状态响应，当前项目完整解析 |

当前代码只实现状态查询和状态响应解析。`message.txt` 中还能看到 `FFF1`、`FFF2`、`FFF3`、`FFF4` 等特征，但本项目暂未使用这些通道。

### 字段解析示例

假设响应帧中单体电压从偏移 34 开始，样本的前几个电芯字节为：

```text
F0 0F  F0 0F  F0 0F  F1 0F
```

按小端序解析：

| 原始字节 | 十六进制值 | 十进制 | 电压 |
| --- | --- | --- | --- |
| `F0 0F` | `0x0FF0` | 4080 | 4.080 V |
| `F0 0F` | `0x0FF0` | 4080 | 4.080 V |
| `F0 0F` | `0x0FF0` | 4080 | 4.080 V |
| `F1 0F` | `0x0FF1` | 4081 | 4.081 V |

电流字段如果原始字节为：

```text
00 00
```

解析为：

```text
i16(00 00) * 0.1 = 0.0 A
```

温度字段如果原始字节为：

```text
D8 FF
```

按 `i16` 小端解析为 `-40`，表示 `-40 C`。这类值可能是未接传感器或设备默认占位值，需要结合硬件状态判断。

### 样本完整响应帧拆解

`message.txt` 中，手机写入查询命令后，保护板通过 9 段 Notify 返回了一条完整响应。合并后的完整十六进制帧为：

```text
7E A1 11 00 00 A8 01 01 04 14 00 00 00 00 00 00 00 00 00 00 80 01 00 00 00 00 00 00 00 00 00 00 00 00 F0 0F F0 0F F0 0F F1 0F F0 0F F0 0F F0 0F F0 0F EF 0F F0 0F F0 0F F0 0F F0 0F F0 0F F1 0F F0 0F F0 0F F0 0F F0 0F F1 0F 0F 00 0E 00 D8 FF D8 FF 0F 00 10 00 E0 1F 00 00 5D 00 64 00 01 01 00 00 80 FE 21 0A 7E DF 4E 09 E7 B6 14 00 00 00 00 00 85 FE B3 00 00 00 00 00 F1 0F 04 00 EF 0F 09 00 02 00 F0 0F 00 00 7D 00 79 00 AA 02 F1 FA 9E 2A 13 00 2F 43 16 00 2A 62 03 00 32 58 09 00 00 00 00 00 46 63 03 00 00 00 C4 07 00 00 A7 7F AA 55
```

关键解析结果：

| 项目 | 值 |
| --- | --- |
| 总长度 | 178 字节 |
| 帧头 | `7E A1` |
| 响应类型 | `0x11` |
| 数据区长度字节 | `0xA8`，十进制 168 |
| 帧尾 | `AA 55` |
| BMS 状态码 | `1`，待机 |
| 温度探头数量 | 4 |
| 电芯数量 | 20 |
| 单体电压范围 | 4079 mV 到 4081 mV |
| 单体求和总压 | 81.602 V |
| 协议原始总压 | 81.60 V |
| 当前电流 | 0.0 A |
| SOC | 93% |
| SOH | 100% |
| 充电 MOS | 开 |
| 放电 MOS | 开 |
| 均衡状态 | 0 |
| 设计容量 | 170.0 Ah |
| 剩余容量 | 156.163966 Ah |
| 实时功率 | 0 W |
| 累计运行时间 | 11796101 秒 |
| 协议最高单体 | 4081 mV，第 4 串 |
| 协议最低单体 | 4079 mV，第 9 串 |
| 协议最大压差 | 2 mV |
| 协议平均单体 | 4080 mV |

这一条样本可以验证当前 App 的三件事：

1. `FFE1` Notify 返回的是分片数据，不是一次完整大包。
2. `AA 55` 可以作为当前样本的完整帧结束标记。
3. 解析字段与 `AntProtocol.kt` 中的偏移逻辑一致。

## 开发与调试建议

- 用 NRF Connect 验证新设备时，先确认 `FFE0/FFE1` 是否存在。
- 开启 `FFE1` Notify 后，手动写入 `7E A1 01 00 00 BE 18 55 AA 55`，看是否返回 `7E A1 11 ... AA 55`。
- 如果返回帧长度、温度数量或电芯数量不同，应优先检查动态偏移是否仍然成立。
- 当前代码没有校验响应帧的校验字段，只依赖帧头、类型、长度下限和帧尾。需要更强可靠性时，应补充校验算法。
- 不同 ANT BMS 固件可能扩展后半段字段，新增字段建议先在 `AntProtocol.kt` 中用 `hasBytes()` 做长度保护。

## 构建环境

- Android Gradle Plugin 项目。
- Kotlin + Jetpack Compose。
- `compileSdk` 36，`minSdk` 29，`targetSdk` 36。
- 本机 debug APK 构建使用 JDK 26 和用户 Android SDK。
- GitHub Actions 使用 Temurin JDK 17 执行 `./gradlew assembleDebug`。

## 注意事项

- 本项目面向蚂蚁保护板 ANT BMS 蓝牙协议，不保证兼容其他品牌 BMS。
- Android 12 及以上需要 `BLUETOOTH_SCAN` 和 `BLUETOOTH_CONNECT` 权限。
- 蓝牙高频轮询会增加手机和保护板负载，低于 1000 ms 前建议先实测稳定性。
- 仪表盘中的预览数据不是保护板真实数据，只用于未连接时展示界面效果。
