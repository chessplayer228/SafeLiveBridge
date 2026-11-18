package com.safelivebridge.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.safelivebridge.app.bridge.NowBarDispatcher
import com.safelivebridge.app.data.ActiveNotificationRepository
import com.safelivebridge.app.data.BridgePreferences
import com.safelivebridge.app.model.ActiveNotificationItem
import com.safelivebridge.app.model.NotificationActionItem
import com.safelivebridge.app.service.KeepAliveForegroundService
import com.safelivebridge.app.service.LiveBridgeNotificationListenerService
import com.safelivebridge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeLiveBridgeTheme {
                MainScreen()
            }
        }
    }
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { BridgePreferences.getInstance(context) }

    val isBridgeEnabled by prefs.isBridgeEnabled.collectAsState()
    val isOtpEnabled by prefs.isOtpEnabled.collectAsState()
    val isExpandOnClick by prefs.isExpandOnClick.collectAsState()
    val isMediaEnabled by prefs.isMediaEnabled.collectAsState()
    val isKeepAliveEnabled by prefs.isKeepAliveEnabled.collectAsState()
    val isAllAppsEnabled by prefs.isAllAppsEnabled.collectAsState()
    val whitelist by prefs.whitelist.collectAsState()
    val blacklist by prefs.blacklist.collectAsState()

    // Проверка разрешений
    var isNotificationAccessGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isBatteryOptIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }

    // Запрос POST_NOTIFICATIONS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (isKeepAliveEnabled && isBridgeEnabled) {
            KeepAliveForegroundService.start(context)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Главная, 1: Приложения, 2: Тест Now Bar

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Панель") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text("Приложения") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.PlayCircleOutline, contentDescription = null) },
                    label = { Text("Тест Now Bar") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    isBridgeEnabled = isBridgeEnabled,
                    isNotificationAccessGranted = isNotificationAccessGranted,
                    isBatteryOptIgnored = isBatteryOptIgnored,
                    isOtpEnabled = isOtpEnabled,
                    isExpandOnClick = isExpandOnClick,
                    isMediaEnabled = isMediaEnabled,
                    isKeepAliveEnabled = isKeepAliveEnabled,
                    onToggleBridge = {
                        prefs.setBridgeEnabled(it)
                        if (it && isKeepAliveEnabled) KeepAliveForegroundService.start(context)
                        else KeepAliveForegroundService.stop(context)
                    },
                    onToggleOtp = { prefs.setOtpEnabled(it) },
                    onToggleExpandOnClick = { prefs.setExpandOnClick(it) },
                    onToggleMedia = { prefs.setMediaEnabled(it) },
                    onToggleKeepAlive = {
                        prefs.setKeepAliveEnabled(it)
                        if (it && isBridgeEnabled) KeepAliveForegroundService.start(context)
                        else KeepAliveForegroundService.stop(context)
                    },
                    onRefreshPermissions = {
                        isNotificationAccessGranted = isNotificationServiceEnabled(context)
                        isBatteryOptIgnored = isBatteryOptimizationIgnored(context)
                    }
                )
                1 -> AppsManagerTab(
                    isAllAppsEnabled = isAllAppsEnabled,
                    whitelist = whitelist,
                    blacklist = blacklist,
                    onToggleAllApps = { prefs.setAllAppsEnabled(it) },
                    onToggleApp = { pkg, allowed ->
                        if (isAllAppsEnabled) {
                            prefs.toggleAppBlacklist(pkg, !allowed)
                        } else {
                            prefs.toggleAppWhitelist(pkg, allowed)
                        }
                    }
                )
                2 -> NowBarTestTab()
            }
        }
    }
}

@Composable
fun DashboardTab(
    isBridgeEnabled: Boolean,
    isNotificationAccessGranted: Boolean,
    isBatteryOptIgnored: Boolean,
    isOtpEnabled: Boolean,
    isExpandOnClick: Boolean,
    isMediaEnabled: Boolean,
    isKeepAliveEnabled: Boolean,
    onToggleBridge: (Boolean) -> Unit,
    onToggleOtp: (Boolean) -> Unit,
    onToggleExpandOnClick: (Boolean) -> Unit,
    onToggleMedia: (Boolean) -> Unit,
    onToggleKeepAlive: (Boolean) -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        // Шапка One UI 9
        item {
            Column {
                Text(
                    text = "LiveBridge",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                )
                Text(
                    text = "Адаптация для Samsung One UI 7 / 8 / 9",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Главная карточка статуса
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBridgeEnabled) SamsungBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isBridgeEnabled) SamsungBlue.copy(alpha = 0.4f) else Color.Transparent
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBridgeEnabled) "Мост Now Bar включен" else "Мост отключен",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isBridgeEnabled) SamsungBlue else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBridgeEnabled) "Уведомления транслируются на экран блокировки и в капсулу" else "Трансляция уведомлений приостановлена",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isBridgeEnabled,
                        onCheckedChange = onToggleBridge,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SamsungBlue
                        )
                    )
                }
            }
        }

        // Превью Now Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Превью Now Bar (Экран блокировки)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Симуляция капсулы Samsung Now Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF1E2129),
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                color = SamsungBlue
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Яндекс Go • 3 мин",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Белый Kia Rio • К 777 АА",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Все (3)",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Необходимые разрешения One UI
        item {
            Text(
                text = "Разрешения и интеграция One UI",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PermissionRow(
                        title = "Доступ к уведомлениям",
                        description = "Чтение входящих уведомлений для преобразования в Live Updates",
                        isGranted = isNotificationAccessGranted,
                        onAction = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            onRefreshPermissions()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    PermissionRow(
                        title = "Работа без ограничений батареи",
                        description = "Предотвращает выгрузку служб энергосбережением One UI",
                        isGranted = isBatteryOptIgnored,
                        onAction = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                            onRefreshPermissions()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    PermissionRow(
                        title = "Настройки живых уведомлений Samsung",
                        description = "Включение отображения Now Bar на экране блокировки",
                        isGranted = true,
                        actionLabel = "Открыть",
                        onAction = {
                            try {
                                context.startActivity(Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS"))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    )
                }
            }
        }

        // Настройки функций (Специальная фича!)
        item {
            Text(
                text = "Функции LiveBridge",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureToggleRow(
                        title = "Раскрывать все уведомления по тапу",
                        description = "При нажатии на Now Bar открывает список всех уведомлений от этого приложения с кнопками действий",
                        checked = isExpandOnClick,
                        onCheckedChange = onToggleExpandOnClick,
                        highlight = true
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    FeatureToggleRow(
                        title = "Умный перехват OTP-кодов",
                        description = "Распознавание кодов подтверждения из SMS и мессенджеров с кнопкой копирования в 1 клик",
                        checked = isOtpEnabled,
                        onCheckedChange = onToggleOtp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    FeatureToggleRow(
                        title = "Медиаплеер и таймеры",
                        description = "Показ треков и системных таймеров в стиле Now Bar",
                        checked = isMediaEnabled,
                        onCheckedChange = onToggleMedia
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    FeatureToggleRow(
                        title = "Фоновая устойчивость (Keep Alive)",
                        description = "Фоновый сервис предотвращает засыпание моста",
                        checked = isKeepAliveEnabled,
                        onCheckedChange = onToggleKeepAlive
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String = if (isGranted) "Включено" else "Настроить",
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onAction,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isGranted) SamsungTeal.copy(alpha = 0.18f) else SamsungBlue.copy(alpha = 0.18f),
                contentColor = if (isGranted) SamsungTeal else SamsungBlue
            )
        ) {
            Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FeatureToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) SamsungBlue else MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SamsungBlue
            )
        )
    }
}

@Composable
fun AppsManagerTab(
    isAllAppsEnabled: Boolean,
    whitelist: Set<String>,
    blacklist: Set<String>,
    onToggleAllApps: (Boolean) -> Unit,
    onToggleApp: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val apps = packages
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName.contains("telecom") || it.packageName.contains("media") }
                .map { appInfo ->
                    InstalledAppInfo(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo)
                    )
                }
                .sortedBy { it.appName.lowercase() }

            installedApps = apps
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Управление приложениями",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Выберите приложения, которые должны выводиться в Now Bar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Переключатель "Все приложения"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Транслировать все приложения", fontWeight = FontWeight.Bold)
                    Text("Все входящие уведомления адаптируются для Now Bar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Switch(
                    checked = isAllAppsEnabled,
                    onCheckedChange = onToggleAllApps,
                    colors = SwitchDefaults.colors(checkedTrackColor = SamsungBlue)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Строка поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск приложений...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SamsungBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = if (isAllAppsEnabled) {
                        !blacklist.contains(app.packageName)
                    } else {
                        whitelist.contains(app.packageName)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleApp(app.packageName, !isChecked) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon.toBitmap(80, 80).asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = app.appName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1)
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleApp(app.packageName, it) },
                                colors = CheckboxDefaults.colors(checkedColor = SamsungBlue)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NowBarTestTab() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Тестирование Now Bar",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Отправьте тестовые уведомления, заблокируйте экран Samsung и проверьте Now Bar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        item {
            TestCard(
                title = "Доставка еды (Прогресс 75%)",
                subtitle = "Курьер приближается к вашему дому",
                icon = Icons.Default.DeliveryDining,
                buttonLabel = "Отправить в Now Bar"
            ) {
                val item = ActiveNotificationItem(
                    key = "test_delivery_key",
                    packageName = "ru.yandex.eda",
                    appName = "Яндекс Еда",
                    title = "Заказ #3429",
                    text = "Курьер у вашего подъезда (доставка через 3 мин)",
                    progress = 75,
                    progressMax = 100,
                    isOngoing = true
                )
                ActiveNotificationRepository.addOrUpdate(item)
                NowBarDispatcher.updateNowBar(context, item)
            }
        }

        item {
            TestCard(
                title = "Поездка на такси (Ongoing)",
                subtitle = "Белый автомобиль ожидает на точке",
                icon = Icons.Default.LocalTaxi,
                buttonLabel = "Отправить в Now Bar"
            ) {
                val item = ActiveNotificationItem(
                    key = "test_taxi_key",
                    packageName = "ru.yandex.taxi",
                    appName = "Яндекс Go",
                    title = "Такси подъехало",
                    text = "Toyota Camry • О 555 ОО 777",
                    isOngoing = true
                )
                ActiveNotificationRepository.addOrUpdate(item)
                NowBarDispatcher.updateNowBar(context, item)
            }
        }

        item {
            TestCard(
                title = "Код подтверждения (OTP)",
                subtitle = "Проверка быстрого копирования пароля",
                icon = Icons.Default.Password,
                buttonLabel = "Отправить в Now Bar"
            ) {
                val item = ActiveNotificationItem(
                    key = "test_otp_key",
                    packageName = "org.telegram.messenger",
                    appName = "Telegram",
                    title = "Код подтверждения",
                    text = "Ваш проверочный код: 748192. Никому не сообщайте его.",
                    extractedOtp = "748192"
                )
                ActiveNotificationRepository.addOrUpdate(item)
                NowBarDispatcher.updateNowBar(context, item)
            }
        }

        item {
            Button(
                onClick = {
                    ActiveNotificationRepository.clearAll()
                    NowBarDispatcher.dismissNowBar(context, "ru.yandex.eda")
                    NowBarDispatcher.dismissNowBar(context, "ru.yandex.taxi")
                    NowBarDispatcher.dismissNowBar(context, "org.telegram.messenger")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Очистить все тестовые уведомления")
            }
        }
    }
}

@Composable
fun TestCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonLabel: String,
    onSend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = SamsungBlue.copy(alpha = 0.15f)
            ) {
                Icon(icon, contentDescription = null, tint = SamsungBlue, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onSend,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(buttonLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
