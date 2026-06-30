package com.zjsf.gps_ant_bms.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zjsf.gps_ant_bms.model.BmsData
import com.zjsf.gps_ant_bms.model.DashboardSettings
import com.zjsf.gps_ant_bms.model.DashboardUiState
import com.zjsf.gps_ant_bms.model.PowerDisplayMode
import com.zjsf.gps_ant_bms.model.bestTemperature
import com.zjsf.gps_ant_bms.model.displayPower
import com.zjsf.gps_ant_bms.model.isValidColor
import com.zjsf.gps_ant_bms.model.normalized
import com.zjsf.gps_ant_bms.model.powerLoadRatio
import com.zjsf.gps_ant_bms.model.previewBmsData
import com.zjsf.gps_ant_bms.model.remainingMileageKm
import com.zjsf.gps_ant_bms.model.toAndroidColor
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private val OledBlack = Color(0xFF06100D)
private val PanelDark = Color(0xFF10201B)
private val PanelDeep = Color(0xFF0A1714)
private val NeonGreen = Color(0xFF39E36C)
private val Mint = Color(0xFF9EFFA8)
private val Cyan = Color(0xFF59D8F5)
private val Amber = Color(0xFFF4C85A)
private val TextSoft = Color(0xFFE9F8F2)
private val TextMuted = Color(0xFF91A9A0)
private val StrokeBlue = Color(0xFF9DCDD3)

enum class DashboardRoute {
    DASHBOARD,
    SETTINGS,
    DETAILS
}

@Composable
fun BmsDashboardApp(
    uiState: DashboardUiState,
    settings: DashboardSettings,
    currentRoute: DashboardRoute,
    selectedPowerMode: PowerDisplayMode,
    onRouteChange: (DashboardRoute) -> Unit,
    onPowerModeChange: (PowerDisplayMode) -> Unit,
    onSettingsSave: (DashboardSettings) -> Unit,
    onScanClick: () -> Unit,
    onFloatingWindowChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialTheme {
        when (currentRoute) {
            DashboardRoute.DASHBOARD -> LandscapeDashboardScreen(
                uiState = uiState,
                settings = settings,
                selectedPowerMode = selectedPowerMode,
                onPowerModeChange = onPowerModeChange,
                onSettingsClick = { onRouteChange(DashboardRoute.SETTINGS) },
                onDetailsClick = { onRouteChange(DashboardRoute.DETAILS) },
                modifier = modifier
            )
            DashboardRoute.SETTINGS -> DashboardSettingsScreen(
                uiState = uiState,
                settings = settings,
                onSave = onSettingsSave,
                onBack = { onRouteChange(DashboardRoute.DASHBOARD) },
                onDetailsClick = { onRouteChange(DashboardRoute.DETAILS) },
                onScanClick = onScanClick,
                onFloatingWindowChange = onFloatingWindowChange,
                onHideFromRecentsChange = onHideFromRecentsChange,
                onReconnectClick = onReconnectClick,
                modifier = modifier
            )
            DashboardRoute.DETAILS -> DashboardDetailsScreen(
                uiState = uiState,
                settings = settings,
                onBack = { onRouteChange(DashboardRoute.DASHBOARD) },
                onScanClick = onScanClick,
                onReconnectClick = onReconnectClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LandscapeDashboardScreen(
    uiState: DashboardUiState,
    settings: DashboardSettings,
    selectedPowerMode: PowerDisplayMode,
    onPowerModeChange: (PowerDisplayMode) -> Unit,
    onSettingsClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data = uiState.bmsData ?: previewBmsData()
    val scale = settings.valueScale.toFloat().coerceIn(0.5f, 2f)
    val screen = LocalConfiguration.current
    val compactHeight = screen.screenHeightDp < 430

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        DashboardBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = if (compactHeight) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 12.dp)
        ) {
            TopMetricGrid(
                uiState = uiState,
                data = data,
                settings = settings,
                scale = scale,
                modifier = Modifier
                    .weight(0.47f)
                    .fillMaxWidth()
            )
            PowerPanel(
                data = data,
                uiState = uiState,
                settings = settings,
                selectedPowerMode = selectedPowerMode,
                onPowerModeChange = onPowerModeChange,
                onSettingsClick = onSettingsClick,
                onDetailsClick = onDetailsClick,
                scale = scale,
                modifier = Modifier
                    .weight(0.53f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DashboardBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3329FF74), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 0.20f),
                radius = size.width * 0.45f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2259D8F5), Color.Transparent),
                center = Offset(size.width * 0.24f, size.height * 0.26f),
                radius = size.width * 0.24f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x26F4C85A), Color.Transparent),
                center = Offset(size.width * 0.84f, size.height * 0.34f),
                radius = size.width * 0.24f
            )
        )
    }
}

@Composable
private fun TopMetricGrid(
    uiState: DashboardUiState,
    data: BmsData,
    settings: DashboardSettings,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(2.1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    value = "%.2fAh".format(data.remainingCharge),
                    color = colorFrom(settings.remainingAhColor, Mint),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
                MetricCard(
                    value = "%.1fkm".format(data.remainingMileageKm(settings)),
                    color = colorFrom(settings.mileageColor, Mint),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    value = "%.0fW".format(data.displayPower()),
                    color = powerAccentColor(data, settings),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
                MetricCard(
                    value = "%.1fA".format(data.current),
                    color = colorFrom(settings.currentColor, Cyan),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
            }
        }

        SocHeroCard(
            soc = data.soc,
            color = colorFrom(settings.socColor, NeonGreen),
            modifier = Modifier
                .weight(0.82f)
                .fillMaxHeight(),
            textScale = scale
        )

        Column(
            modifier = Modifier.weight(2.1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    value = "%.2fV".format(data.totalVoltage),
                    color = colorFrom(settings.voltageColor, TextSoft),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
                TemperatureGridCard(
                    values = temperatureValues(data),
                    color = tempAccentColor(data, settings),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    value = formatRssi(uiState.bluetoothRssi),
                    color = colorFrom(settings.sohColor, TextSoft),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
                MetricCard(
                    value = "${data.voltageDiff}mV",
                    color = diffAccentColor(data, settings),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textScale = scale
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    textScale: Float = 1f
) {
    GlowPanel(
        modifier = modifier,
        borderColor = StrokeBlue,
        glowColor = color.copy(alpha = 0.45f),
        corner = 10.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val compactFactor = min(maxWidth.value / 190f, maxHeight.value / 84f).coerceIn(0.58f, 1f)
            Text(
                text = value,
                color = color,
                fontSize = (34 * textScale * compactFactor).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun TemperatureGridCard(
    values: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    textScale: Float = 1f
) {
    GlowPanel(
        modifier = modifier,
        borderColor = StrokeBlue,
        glowColor = color.copy(alpha = 0.35f),
        corner = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            values.chunked(2).take(2).forEach { row ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { temp ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0x334B4E33)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = temp,
                                color = color,
                                fontSize = (22 * textScale).sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocHeroCard(
    soc: Int,
    color: Color,
    modifier: Modifier = Modifier,
    textScale: Float = 1f
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = 12.dp.toPx()
            val fillRatio = (soc / 100f).coerceIn(0f, 1f)
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    setShadowLayer(26.dp.toPx(), 0f, 10.dp.toPx(), android.graphics.Color.argb(190, 57, 227, 108))
                    this.color = android.graphics.Color.TRANSPARENT
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF17352C), Color(0xFF10251F))),
                cornerRadius = CornerRadius(radius, radius)
            )
            val fillTop = size.height * (1f - fillRatio)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF98FFB4), color, Color(0xFFB8FFD0)),
                    startY = fillTop,
                    endY = size.height
                ),
                topLeft = Offset(0f, fillTop),
                size = Size(size.width, size.height - fillTop),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.22f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.75f),
                style = Stroke(width = 1.4.dp.toPx()),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val digits = soc.toString().length.coerceAtLeast(1)
            val widthLimited = maxWidth.value / (digits * 0.58f)
            val heightLimited = maxHeight.value * 0.60f
            val fontSize = min(widthLimited, heightLimited).coerceIn(44f, 92f) * textScale
            Text(
                text = soc.toString(),
                color = Color(0xFF020504),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun PowerPanel(
    data: BmsData,
    uiState: DashboardUiState,
    settings: DashboardSettings,
    selectedPowerMode: PowerDisplayMode,
    onPowerModeChange: (PowerDisplayMode) -> Unit,
    onSettingsClick: () -> Unit,
    onDetailsClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier
) {
    GlowPanel(
        modifier = modifier,
        borderColor = StrokeBlue.copy(alpha = 0.75f),
        glowColor = NeonGreen.copy(alpha = 0.18f),
        corner = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.0fW".format(data.displayPower()),
                    color = powerAccentColor(data, settings),
                    fontSize = (30 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.9f),
                    maxLines = 1
                )
                Text(
                    text = uiState.statusMessage,
                    color = TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.weight(1.45f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardButton(
                        text = "功率条",
                        selected = selectedPowerMode == PowerDisplayMode.POWER_BAR,
                        onClick = { onPowerModeChange(PowerDisplayMode.POWER_BAR) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardButton(
                        text = "曲线图",
                        selected = selectedPowerMode == PowerDisplayMode.CURVE,
                        onClick = { onPowerModeChange(PowerDisplayMode.CURVE) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardButton(
                        text = "设置",
                        selected = false,
                        onClick = onSettingsClick,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardButton(
                        text = "详情",
                        selected = false,
                        onClick = onDetailsClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xAA07110F))
                    .padding(if (selectedPowerMode == PowerDisplayMode.CURVE) 2.dp else 12.dp)
            ) {
                if (selectedPowerMode == PowerDisplayMode.POWER_BAR) {
                    PowerBarVisualization(data, settings, modifier = Modifier.fillMaxSize())
                } else {
                    PowerCurveVisualization(
                        history = uiState.powerHistory.ifEmpty { listOf(data.displayPower()) },
                        settings = settings,
                        color = powerAccentColor(data, settings),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerBarVisualization(
    data: BmsData,
    settings: DashboardSettings,
    modifier: Modifier = Modifier
) {
    val targetRatio = data.powerLoadRatio(settings).coerceIn(0.0, 1.0).toFloat()
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 650),
        label = "power-bar-ratio"
    )
    val percent = (targetRatio * 100).roundToInt()
    val powerValue = data.displayPower()
    val isCharging = powerValue < 0.0
    val color = powerAccentColor(data, settings)
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val marginX = 24.dp.toPx()
            val barHeight = (size.height * 0.62f).coerceAtLeast(54.dp.toPx())
            val top = (size.height - barHeight) / 2f
            val panel = Rect(marginX, top, size.width - marginX, top + barHeight)
            val radius = barHeight / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF142522), Color(0xFF07110F))),
                topLeft = panel.topLeft,
                size = panel.size,
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = StrokeBlue.copy(alpha = 0.22f),
                topLeft = panel.topLeft,
                size = panel.size,
                style = Stroke(width = 1.2.dp.toPx()),
                cornerRadius = CornerRadius(radius, radius)
            )
            val fillWidth = (panel.width * animatedRatio)
                .coerceAtLeast(if (animatedRatio > 0.01f) radius else 0f)
                .coerceAtMost(panel.width)
            if (fillWidth > 0f) {
                val fillRect = if (isCharging) {
                    Rect(panel.right - fillWidth, panel.top, panel.right, panel.bottom)
                } else {
                    Rect(panel.left, panel.top, panel.left + fillWidth, panel.bottom)
                }
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        powerBarGradientColors(data, settings, isCharging),
                        startX = fillRect.left,
                        endX = fillRect.right
                    ),
                    topLeft = fillRect.topLeft,
                    size = fillRect.size,
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawRoundRect(
                    color = color.copy(alpha = 0.28f),
                    topLeft = Offset(fillRect.left, fillRect.top - 6.dp.toPx()),
                    size = Size(fillRect.width, fillRect.height + 12.dp.toPx()),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)),
                topLeft = Offset(panel.left + radius * 0.42f, panel.top + barHeight * 0.16f),
                size = Size((panel.width - radius * 0.84f).coerceAtLeast(0f), barHeight * 0.22f),
                cornerRadius = CornerRadius(barHeight * 0.11f, barHeight * 0.11f)
            )
        }
        Text(
            text = "$percent%",
            color = TextSoft,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun PowerCurveVisualization(
    history: List<Double>,
    settings: DashboardSettings,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dischargeBase = settings.dischargeBaseW.coerceAtLeast(1.0)
            val chargeBase = settings.chargeBaseW.coerceAtLeast(1.0)
            val left = 6.dp.toPx()
            val right = size.width - 6.dp.toPx()
            val top = 6.dp.toPx()
            val bottom = size.height - 6.dp.toPx()
            val chartHeight = (bottom - top).coerceAtLeast(1f)
            val dischargeHeight = (chartHeight * (dischargeBase / (dischargeBase + chargeBase))).toFloat()
            val chargeHeight = chartHeight - dischargeHeight
            val zeroY = top + dischargeHeight
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0x33404F4B), Color(0x18000000))),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawLine(Color(0x6689A69D), Offset(left, zeroY), Offset(right, zeroY), strokeWidth = 1.4.dp.toPx())
            repeat(8) { i ->
                val x = left + (right - left) * i / 7f
                drawLine(Color(0x2489A69D), Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
            }
            val points = history.takeLast(120)
            if (points.size >= 2) {
                val path = Path()
                points.forEachIndexed { index, value ->
                    val x = left + (right - left) * index / (points.lastIndex.toFloat())
                    val y = if (value >= 0.0) {
                        val ratio = (value / dischargeBase).coerceIn(0.0, 1.0).toFloat()
                        zeroY - dischargeHeight * ratio
                    } else {
                        val ratio = (abs(value) / chargeBase).coerceIn(0.0, 1.0).toFloat()
                        zeroY + chargeHeight * ratio
                    }
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color.copy(alpha = 0.30f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, color, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun DashboardButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) NeonGreen else Color(0xCC172724))
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.8f) else StrokeBlue.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TextSoft,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlowPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = StrokeBlue,
    glowColor: Color = NeonGreen.copy(alpha = 0.35f),
    corner: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = corner.toPx()
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(
                        (glowColor.alpha * 255).roundToInt(),
                        (glowColor.red * 255).roundToInt(),
                        (glowColor.green * 255).roundToInt(),
                        (glowColor.blue * 255).roundToInt()
                    )
                    setShadowLayer(18.dp.toPx(), 0f, 9.dp.toPx(), color)
                }
                canvas.nativeCanvas.drawRoundRect(2f, 2f, size.width - 2f, size.height - 2f, radius, radius, paint)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xDD172522), Color(0xDD0D1816))),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = borderColor.copy(alpha = 0.85f),
                style = Stroke(width = 1.2.dp.toPx()),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.22f), Color.Transparent), radius = size.maxDimension * 0.7f),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
        content()
    }
}

@Composable
private fun DashboardSettingsScreen(
    uiState: DashboardUiState,
    settings: DashboardSettings,
    onSave: (DashboardSettings) -> Unit,
    onBack: () -> Unit,
    onDetailsClick: () -> Unit,
    onScanClick: () -> Unit,
    onFloatingWindowChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var form by remember(settings) { mutableStateOf(SettingsForm.from(settings)) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "仪表盘设置",
            color = TextSoft,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                SettingsGroup {
                    NeonTextField("续航系数 km/Ah", form.mileageFactorKmPerAh) { form = form.copy(mileageFactorKmPerAh = it) }
                    NeonTextField("数值大小比例", form.valueScale) { form = form.copy(valueScale = it) }
                    NeonTextField("蓝牙刷新时间 ms", form.bluetoothRefreshIntervalMs) { form = form.copy(bluetoothRefreshIntervalMs = it) }
                    Text("默认功率显示模式", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ModeRadio("功率条", form.defaultPowerDisplayMode == PowerDisplayMode.POWER_BAR) {
                            form = form.copy(defaultPowerDisplayMode = PowerDisplayMode.POWER_BAR)
                        }
                        ModeRadio("曲线图", form.defaultPowerDisplayMode == PowerDisplayMode.CURVE) {
                            form = form.copy(defaultPowerDisplayMode = PowerDisplayMode.CURVE)
                        }
                    }
                    NeonTextField("温度显示来源", form.temperatureSource, keyboardType = KeyboardType.Text) { form = form.copy(temperatureSource = it) }
                }
            }

            item {
                SettingsGroup(title = "阈值用于仪表盘颜色和功率条颜色切换。") {
                    NeonTextField("放电功率条基数 W", form.dischargeBaseW) { form = form.copy(dischargeBaseW = it) }
                    NeonTextField("充电功率条基数 W", form.chargeBaseW) { form = form.copy(chargeBaseW = it) }
                    NeonTextField("功率条黄色起点 0-1", form.powerYellowStart) { form = form.copy(powerYellowStart = it) }
                    NeonTextField("功率条红色起点 0-1", form.powerRedStart) { form = form.copy(powerRedStart = it) }
                    NeonTextField("电量红色上限 %", form.socRedUpperPercent) { form = form.copy(socRedUpperPercent = it) }
                    NeonTextField("电量黄色上限 %", form.socYellowUpperPercent) { form = form.copy(socYellowUpperPercent = it) }
                    NeonTextField("压差绿色上限 mV", form.diffGreenUpperMv) { form = form.copy(diffGreenUpperMv = it) }
                    NeonTextField("压差黄色上限 mV", form.diffYellowUpperMv) { form = form.copy(diffYellowUpperMv = it) }
                    NeonTextField("温度绿色上限 °C", form.tempGreenUpperC) { form = form.copy(tempGreenUpperC = it) }
                    NeonTextField("温度黄色上限 °C", form.tempYellowUpperC) { form = form.copy(tempYellowUpperC = it) }
                }
            }

            item {
                SettingsGroup(title = "颜色格式为 #RRGGBB，例如 #39E36C") {
                    NeonTextField("SOC 颜色", form.socColor, KeyboardType.Text) { form = form.copy(socColor = it) }
                    NeonTextField("剩余 Ah 颜色", form.remainingAhColor, KeyboardType.Text) { form = form.copy(remainingAhColor = it) }
                    NeonTextField("剩余续航颜色", form.mileageColor, KeyboardType.Text) { form = form.copy(mileageColor = it) }
                    NeonTextField("功率颜色", form.powerColor, KeyboardType.Text) { form = form.copy(powerColor = it) }
                    NeonTextField("电压颜色", form.voltageColor, KeyboardType.Text) { form = form.copy(voltageColor = it) }
                    NeonTextField("电流颜色", form.currentColor, KeyboardType.Text) { form = form.copy(currentColor = it) }
                    NeonTextField("压差颜色", form.diffColor, KeyboardType.Text) { form = form.copy(diffColor = it) }
                    NeonTextField("温度颜色", form.temperatureColor, KeyboardType.Text) { form = form.copy(temperatureColor = it) }
                    NeonTextField("SOH 颜色", form.sohColor, KeyboardType.Text) { form = form.copy(sohColor = it) }
                }
            }

            item {
                SettingsGroup(title = "连接与系统功能") {
                    StatusRow("状态", uiState.statusMessage)
                    StatusRow("上次设备", uiState.lastDeviceAddress ?: "无")
                    ToggleRow("启用悬浮窗", uiState.floatingWindowEnabled, onFloatingWindowChange)
                    ToggleRow("隐藏最近任务", uiState.hideFromRecentsEnabled, onHideFromRecentsChange)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        PrimaryAction("扫描设备", onScanClick, Modifier.weight(1f))
                        PrimaryAction("重连", onReconnectClick, Modifier.weight(1f))
                        PrimaryAction("查看详情", onDetailsClick, Modifier.weight(1f))
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        text = error.orEmpty(),
                        color = Color(0xFFFF7777),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryAction(
                text = "保存",
                onClick = {
                    val parsed = form.toSettingsOrError()
                    if (parsed.settings != null) {
                        error = null
                        onSave(parsed.settings)
                    } else {
                        error = parsed.error
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryAction("返回", onBack, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10201B))
            .border(1.dp, Color(0xFF203B34), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title != null) {
            Text(title, color = TextMuted, fontSize = 13.sp)
        }
        content()
    }
}

@Composable
private fun NeonTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextMuted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(color = TextSoft, fontSize = 14.sp),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ModeRadio(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = NeonGreen, unselectedColor = TextMuted)
        )
        Text(text, color = TextSoft, fontSize = 14.sp)
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = TextSoft, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = Color(0x6639E36C))
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextMuted, fontSize = 14.sp, modifier = Modifier.width(82.dp))
        Text(value, color = TextSoft, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06E468), contentColor = Color.White)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun DashboardDetailsScreen(
    uiState: DashboardUiState,
    settings: DashboardSettings,
    onBack: () -> Unit,
    onScanClick: () -> Unit,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data = uiState.bmsData ?: previewBmsData()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("连接与 BMS 详情", color = TextSoft, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("返回", color = NeonGreen, fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryAction("扫描设备", onScanClick, Modifier.weight(1f))
            PrimaryAction("重连", onReconnectClick, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SettingsGroup {
                    StatusRow("连接", if (uiState.connected) "已连接" else "未连接")
                    StatusRow("蓝牙信号", formatRssi(uiState.bluetoothRssi))
                    StatusRow("GPS", "%.2f km/h".format(uiState.gpsSpeedKmh))
                    StatusRow("状态", uiState.statusMessage)
                    StatusRow("保护板状态", data.bmsStatusText)
                    StatusRow("上次设备", uiState.lastDeviceAddress ?: "无")
                }
            }
            item {
                SettingsGroup {
                    StatusRow("总电压", "%.2f V".format(data.totalVoltage))
                    StatusRow("压差", "${data.voltageDiff} mV")
                    StatusRow("电流", "%.1f A".format(data.current))
                    StatusRow("功率", "%.1f W".format(data.displayPower()))
                    StatusRow("SOC", "${data.soc} %")
                    StatusRow("SOH", "${data.soh} %")
                    StatusRow("容量", "%.2f Ah".format(data.capacity))
                    StatusRow("剩余", "%.2f Ah".format(data.remainingCharge))
                    StatusRow("续航", "%.1f km".format(data.remainingMileageKm(settings)))
                    StatusRow("MOS 温度", "${data.mosTemp} °C")
                    StatusRow("均衡温度", "${data.balancerTemp} °C")
                    StatusRow("传感器温度", data.temperatures.joinToString(", ").ifBlank { "无" })
                    StatusRow("运行时间", formatRuntime(data.runtime))
                }
            }
            item {
                SettingsGroup(title = "电芯统计") {
                    StatusRow("串数", "${data.cellVoltages.size} S")
                    StatusRow("平均电压", formatCellVoltage(data.averageCellVoltage))
                    StatusRow("最高电芯", formatCellExtreme(data.maxCellIndex, data.maxCellVoltage))
                    StatusRow("最低电芯", formatCellExtreme(data.minCellIndex, data.minCellVoltage))
                    StatusRow("协议压差", "${data.voltageDiff} mV")
                }
            }
            item {
                SettingsGroup(title = "MOS 与均衡") {
                    StatusRow("充电 MOS", formatSwitchState(data.chargeMosOn))
                    StatusRow("放电 MOS", formatSwitchState(data.dischargeMosOn))
                    StatusRow("均衡状态", formatBalanceStatus(data.balanceStatus))
                    StatusRow("均衡电芯", formatBalancedCells(data.balanceMask, data.cellVoltages.size))
                    StatusRow("均衡掩码", formatHexMask(data.balanceMask))
                }
            }
            item {
                SettingsGroup(title = "容量统计") {
                    StatusRow("循环容量", formatAh(data.cycleCapacity))
                    StatusRow("累计充电", formatAh(data.totalChargeCapacity))
                    StatusRow("累计放电", formatAh(data.totalDischargeCapacity))
                    StatusRow("累计充电时间", formatRuntimeOrNone(data.totalChargeTime))
                    StatusRow("累计放电时间", formatRuntimeOrNone(data.totalDischargeTime))
                }
            }
            item {
                SettingsGroup(title = "协议诊断") {
                    StatusRow("状态码", data.bmsStatusCode?.toString() ?: "未知")
                    StatusRow("帧长度", if (data.frameLength > 0) "${data.frameLength} bytes" else "未知")
                    StatusRow("未解析字节", "${data.unparsedBytes} bytes")
                }
            }
            item {
                Text("单体电压", color = TextSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            itemsIndexed(data.cellVoltages) { index, voltage ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10201B))
                        .border(1.dp, Color(0xFF203B34), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cell %02d".format(index + 1), color = TextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("$voltage mV", color = TextSoft, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private data class SettingsForm(
    val mileageFactorKmPerAh: String,
    val valueScale: String,
    val defaultPowerDisplayMode: PowerDisplayMode,
    val bluetoothRefreshIntervalMs: String,
    val temperatureSource: String,
    val dischargeBaseW: String,
    val chargeBaseW: String,
    val powerYellowStart: String,
    val powerRedStart: String,
    val socRedUpperPercent: String,
    val socYellowUpperPercent: String,
    val diffGreenUpperMv: String,
    val diffYellowUpperMv: String,
    val tempGreenUpperC: String,
    val tempYellowUpperC: String,
    val socColor: String,
    val remainingAhColor: String,
    val mileageColor: String,
    val powerColor: String,
    val voltageColor: String,
    val currentColor: String,
    val diffColor: String,
    val temperatureColor: String,
    val sohColor: String
) {
    companion object {
        fun from(settings: DashboardSettings): SettingsForm = SettingsForm(
            mileageFactorKmPerAh = settings.mileageFactorKmPerAh.toString(),
            valueScale = settings.valueScale.toString(),
            defaultPowerDisplayMode = settings.defaultPowerDisplayMode,
            bluetoothRefreshIntervalMs = settings.bluetoothRefreshIntervalMs.toString(),
            temperatureSource = settings.temperatureSource,
            dischargeBaseW = settings.dischargeBaseW.trimDouble(),
            chargeBaseW = settings.chargeBaseW.trimDouble(),
            powerYellowStart = settings.powerYellowStart.toString(),
            powerRedStart = settings.powerRedStart.toString(),
            socRedUpperPercent = settings.socRedUpperPercent.trimDouble(),
            socYellowUpperPercent = settings.socYellowUpperPercent.trimDouble(),
            diffGreenUpperMv = settings.diffGreenUpperMv.trimDouble(),
            diffYellowUpperMv = settings.diffYellowUpperMv.trimDouble(),
            tempGreenUpperC = settings.tempGreenUpperC.toString(),
            tempYellowUpperC = settings.tempYellowUpperC.toString(),
            socColor = settings.socColor,
            remainingAhColor = settings.remainingAhColor,
            mileageColor = settings.mileageColor,
            powerColor = settings.powerColor,
            voltageColor = settings.voltageColor,
            currentColor = settings.currentColor,
            diffColor = settings.diffColor,
            temperatureColor = settings.temperatureColor,
            sohColor = settings.sohColor
        )
    }

    fun toSettingsOrError(): ParseResult {
        val colors = listOf(socColor, remainingAhColor, mileageColor, powerColor, voltageColor, currentColor, diffColor, temperatureColor, sohColor)
        if (colors.any { !isValidColor(it) }) {
            return ParseResult(null, "颜色必须是 #RRGGBB 格式。")
        }
        val refreshMs = bluetoothRefreshIntervalMs.toLongOrNull() ?: return ParseResult(null, "蓝牙刷新时间必须是整数毫秒。")
        if (refreshMs !in 200L..60_000L) return ParseResult(null, "蓝牙刷新时间范围为 200-60000ms。")
        val parsed = DashboardSettings(
            mileageFactorKmPerAh = mileageFactorKmPerAh.toDoubleOrNull() ?: return ParseResult(null, "续航系数必须是数字。"),
            valueScale = valueScale.toDoubleOrNull() ?: return ParseResult(null, "数值大小比例必须是数字。"),
            defaultPowerDisplayMode = defaultPowerDisplayMode,
            bluetoothRefreshIntervalMs = refreshMs,
            temperatureSource = temperatureSource.ifBlank { "自动" },
            dischargeBaseW = dischargeBaseW.toDoubleOrNull() ?: return ParseResult(null, "放电功率条基数必须是数字。"),
            chargeBaseW = chargeBaseW.toDoubleOrNull() ?: return ParseResult(null, "充电功率条基数必须是数字。"),
            powerYellowStart = powerYellowStart.toDoubleOrNull() ?: return ParseResult(null, "功率条黄色起点必须是数字。"),
            powerRedStart = powerRedStart.toDoubleOrNull() ?: return ParseResult(null, "功率条红色起点必须是数字。"),
            socRedUpperPercent = socRedUpperPercent.toDoubleOrNull() ?: return ParseResult(null, "电量红色上限必须是数字。"),
            socYellowUpperPercent = socYellowUpperPercent.toDoubleOrNull() ?: return ParseResult(null, "电量黄色上限必须是数字。"),
            diffGreenUpperMv = diffGreenUpperMv.toDoubleOrNull() ?: return ParseResult(null, "压差绿色上限必须是数字。"),
            diffYellowUpperMv = diffYellowUpperMv.toDoubleOrNull() ?: return ParseResult(null, "压差黄色上限必须是数字。"),
            tempGreenUpperC = tempGreenUpperC.toDoubleOrNull() ?: return ParseResult(null, "温度绿色上限必须是数字。"),
            tempYellowUpperC = tempYellowUpperC.toDoubleOrNull() ?: return ParseResult(null, "温度黄色上限必须是数字。"),
            socColor = socColor,
            remainingAhColor = remainingAhColor,
            mileageColor = mileageColor,
            powerColor = powerColor,
            voltageColor = voltageColor,
            currentColor = currentColor,
            diffColor = diffColor,
            temperatureColor = temperatureColor,
            sohColor = sohColor
        ).normalized()
        if (parsed.powerYellowStart > parsed.powerRedStart) return ParseResult(null, "功率条黄色起点不能大于红色起点。")
        return ParseResult(parsed, null)
    }
}

private data class ParseResult(val settings: DashboardSettings?, val error: String?)

private fun Double.trimDouble(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun colorFrom(value: String, fallback: Color): Color =
    Color(value.toAndroidColor("#%06X".format(0xFFFFFF and fallback.toAndroidArgbCompat())))

private fun Color.toAndroidArgbCompat(): Int =
    android.graphics.Color.argb((alpha * 255).roundToInt(), (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())

private fun powerAccentColor(data: BmsData, settings: DashboardSettings): Color {
    return powerGradientColor(
        ratio = data.powerLoadRatio(settings),
        settings = settings,
        green = colorFrom(settings.powerColor, NeonGreen)
    )
}

private fun powerBarGradientColors(
    data: BmsData,
    settings: DashboardSettings,
    isCharging: Boolean
): List<Color> {
    val ratio = data.powerLoadRatio(settings).coerceIn(0.0, 1.0)
    val green = colorFrom(settings.powerColor, NeonGreen)
    val endColor = powerGradientColor(ratio, settings, green)
    val yellowStart = settings.powerYellowStart.coerceIn(0.0, 1.0)
    val redStart = settings.powerRedStart.coerceIn(yellowStart, 1.0)
    val colors = when {
        ratio <= yellowStart -> listOf(
            green.copy(alpha = 0.95f),
            endColor,
            Color.White.copy(alpha = 0.34f)
        )
        ratio < redStart -> listOf(
            green.copy(alpha = 0.95f),
            endColor,
            Color.White.copy(alpha = 0.34f)
        )
        else -> listOf(
            green.copy(alpha = 0.95f),
            Amber,
            endColor,
            Color.White.copy(alpha = 0.34f)
        )
    }
    return if (isCharging) colors.reversed() else colors
}

private fun powerGradientColor(
    ratio: Double,
    settings: DashboardSettings,
    green: Color
): Color {
    val safeRatio = ratio.coerceIn(0.0, 1.0)
    val yellowStart = settings.powerYellowStart.coerceIn(0.0, 1.0)
    val redStart = settings.powerRedStart.coerceIn(yellowStart, 1.0)
    return when {
        safeRatio <= yellowStart -> green
        safeRatio < redStart -> {
            val progress = ((safeRatio - yellowStart) / (redStart - yellowStart).coerceAtLeast(0.0001)).toFloat()
            lerpColor(green, Amber, progress)
        }
        else -> {
            val progress = ((safeRatio - redStart) / (1.0 - redStart).coerceAtLeast(0.0001)).toFloat()
            lerpColor(Amber, Color(0xFFFF6565), progress)
        }
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}

private fun diffAccentColor(data: BmsData, settings: DashboardSettings): Color =
    when {
        data.voltageDiff <= settings.diffGreenUpperMv -> colorFrom(settings.diffColor, Amber)
        data.voltageDiff <= settings.diffYellowUpperMv -> Amber
        else -> Color(0xFFFF6565)
    }

private fun tempAccentColor(data: BmsData, settings: DashboardSettings): Color {
    val temp = data.bestTemperature(settings)
    return when {
        temp <= settings.tempGreenUpperC -> colorFrom(settings.temperatureColor, Amber)
        temp <= settings.tempYellowUpperC -> Amber
        else -> Color(0xFFFF6565)
    }
}

private fun temperatureValues(data: BmsData): List<String> =
    listOf(
        "${data.mosTemp}°",
        "${data.balancerTemp}°",
        data.temperatures.getOrNull(0)?.let { "$it°" } ?: "--",
        data.temperatures.getOrNull(1)?.let { "$it°" } ?: "--"
    )

private fun formatRssi(rssi: Int?): String =
    if (rssi == null) "--dBm" else "${rssi}dBm"

private fun formatRuntime(runtime: Long): String {
    val d = runtime / 86400
    val h = (runtime % 86400) / 3600
    val m = (runtime % 3600) / 60
    val s = runtime % 60
    return "%d天 %02d:%02d:%02d".format(d, h, m, s)
}

private fun formatRuntimeOrNone(runtime: Long): String =
    if (runtime <= 0L) "无" else formatRuntime(runtime)

private fun formatCellVoltage(voltageMv: Int?): String =
    voltageMv?.let { "%.3f V".format(it / 1000.0) } ?: "未知"

private fun formatCellExtreme(index: Int?, voltageMv: Int?): String =
    if (index != null && voltageMv != null) {
        "Cell %02d  %.3f V".format(index, voltageMv / 1000.0)
    } else {
        "未知"
    }

private fun formatSwitchState(value: Boolean?): String =
    when (value) {
        true -> "开"
        false -> "关"
        null -> "未知"
    }

private fun formatBalanceStatus(status: Int?): String =
    when (status) {
        null -> "未知"
        0 -> "未均衡"
        1 -> "均衡中"
        else -> "状态 $status"
    }

private fun formatBalancedCells(mask: Long, cellCount: Int): String {
    if (mask == 0L || cellCount <= 0) return "无"
    val cells = (0 until min(cellCount, 32))
        .filter { index -> ((mask shr index) and 0x1L) == 1L }
        .joinToString(", ") { index -> "Cell %02d".format(index + 1) }
    return cells.ifBlank { "无" }
}

private fun formatHexMask(mask: Long): String =
    if (mask == 0L) "0x00000000" else "0x%08X".format(mask)

private fun formatAh(value: Double): String =
    if (value <= 0.0) "无" else "%.2f Ah".format(value)
