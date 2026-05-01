package dev.aether.manager.ui.appprofile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.aether.manager.data.*
import dev.aether.manager.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
// Additional imports for refresh rate detection
import android.content.Context
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppProfileScreen(vm: AppProfileViewModel) {
    val state by vm.state.collectAsState()
    val ctx   = LocalContext.current
    val snack by vm.snack.collectAsState()

    LaunchedEffect(snack) {
        if (snack != null) {
            android.widget.Toast.makeText(ctx, snack, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearSnack()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Box(
            Modifier
                .padding(bottom = pad.calculateBottomPadding())
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState    = state,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
                label          = "apps_state"
            ) { s ->
                when (s) {
                    is AppsUiState.Loading -> LoadingContent()
                    is AppsUiState.Error   -> ErrorContent(s.msg) { vm.load() }
                    is AppsUiState.Ready   -> ReadyContent(s, vm)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State screens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier    = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color       = MaterialTheme.colorScheme.primary
            )
            Text(
                s.appProfileLoading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(msg: String, onRetry: () -> Unit) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline, null,
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(msg, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onRetry) { Text(s.appProfileRetry) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main content
// ─────────────────────────────────────────────────────────────────────────────

private enum class ProfileFilter {
    ALL, PERFORMANCE, BALANCED, POWER_SAVE
}

private fun ProfileFilter.label(): String = when (this) {
    ProfileFilter.ALL         -> "All"
    ProfileFilter.PERFORMANCE -> "Performance"
    ProfileFilter.BALANCED    -> "Balanced"
    ProfileFilter.POWER_SAVE  -> "Power Save"
}

private fun ProfileFilter.icon(): ImageVector = when (this) {
    ProfileFilter.ALL         -> Icons.Outlined.Apps
    ProfileFilter.PERFORMANCE -> Icons.Outlined.Speed
    ProfileFilter.BALANCED    -> Icons.Outlined.Tune
    ProfileFilter.POWER_SAVE  -> Icons.Outlined.BatterySaver
}

private fun governorForFilter(filter: ProfileFilter): String = when (filter) {
    ProfileFilter.PERFORMANCE -> "performance"
    ProfileFilter.BALANCED    -> "schedutil"
    ProfileFilter.POWER_SAVE  -> "powersave"
    ProfileFilter.ALL         -> "default"
}

private fun filterForProfile(profile: AppProfile?): ProfileFilter {
    val governor = profile?.cpuGovernor?.lowercase()
    return when (governor) {
        "performance" -> ProfileFilter.PERFORMANCE
        "powersave", "power_save", "battery", "battery_saver" -> ProfileFilter.POWER_SAVE
        else -> ProfileFilter.BALANCED
    }
}

private fun profileModeLabel(profile: AppProfile?): String {
    if (profile == null) return "Not Set"
    return filterForProfile(profile).label()
}

private fun refreshRateLabel(value: String): String = when (value.lowercase()) {
    "default" -> "Default"
    "60", "60hz" -> "60Hz"
    "90", "90hz" -> "90Hz"
    else -> value
}

private fun defaultProfile(packageName: String): AppProfile = AppProfile(packageName = packageName)

private val profileFilters = listOf(
    ProfileFilter.ALL,
    ProfileFilter.PERFORMANCE,
    ProfileFilter.BALANCED,
    ProfileFilter.POWER_SAVE
)

private val profileOptions = listOf(
    ProfileFilter.PERFORMANCE,
    ProfileFilter.BALANCED,
    ProfileFilter.POWER_SAVE
)

/*
 * Global refresh‑rate options are no longer used.  The App list items now
 * determine the available refresh rates based on the device capabilities via
 * getMaxRefreshRate().  Leaving this constant around unused would be
 * confusing to future maintainers.  If you need a default, see the
 * dynamic construction inside AppListItem.
 */

@Composable
private fun ReadyContent(state: AppsUiState.Ready, vm: AppProfileViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ProfileFilter.ALL) }

    val editingProfile by vm.editingProfile.collectAsState()
    val savingPkg by vm.savingPkg.collectAsState()

    editingProfile?.let { profile ->
        val app = state.apps.find { it.packageName == profile.packageName }
        AppProfileEditor(
            profile   = profile,
            appLabel  = app?.label ?: profile.packageName,
            appIcon   = app?.icon,
            saving    = savingPkg == profile.packageName,
            onDismiss = { vm.closeEditor() },
            onSave    = { vm.saveProfile(it) }
        )
    }

    val activeCount = remember(state.profiles) {
        state.profiles.values.count { it.enabled }
    }

    val filtered = remember(state.apps, state.profiles, searchQuery, selectedFilter) {
        state.apps
            .filter { app ->
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
            .filter { app ->
                selectedFilter == ProfileFilter.ALL ||
                filterForProfile(state.profiles[app.packageName]) == selectedFilter
            }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            AppProfileStatusCard(
                enabled     = state.monitorRunning,
                activeCount = activeCount,
                totalApps   = state.apps.size,
                onToggle    = { vm.toggleMonitor(!state.monitorRunning) }
            )

            SearchFilterBar(
                query          = searchQuery,
                selectedFilter = selectedFilter,
                onQueryChange  = { searchQuery = it },
                onFilterChange = { selectedFilter = it }
            )
        }

        if (filtered.isEmpty()) {
            EmptyListHint(searchQuery.isNotEmpty())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = 12.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "apps_header") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Aplikasi",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${filtered.size} apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(filtered, key = { it.packageName }) { app ->
                    val profile = state.profiles[app.packageName]
                    AppListItem(
                        app       = app,
                        profile   = profile,
                        onClick   = { vm.openEditor(app) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monitor Pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonitorPill(running: Boolean, onToggle: () -> Unit) {
    val s = LocalStrings.current
    val bg by animateColorAsState(
        if (running) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "monitor_bg"
    )
    val fg by animateColorAsState(
        if (running) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "monitor_fg"
    )

    Surface(
        onClick = onToggle,
        shape   = RoundedCornerShape(50),
        color   = bg
    ) {
        Row(
            modifier              = Modifier.padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                if (running) s.appProfileMonitorOn else s.appProfileMonitorOff,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = fg
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchFilterBar(
    query: String,
    selectedFilter: ProfileFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ProfileFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SearchAppField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f)
        )

        FilterDropdown(
            selected = selectedFilter,
            onSelect = onFilterChange,
            modifier = Modifier
                .width(142.dp)
                .height(50.dp)
        )
    }
}

@Composable
private fun SearchAppField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    val borderWidth by animateDpAsState(
        if (focused) 1.4.dp else 0.dp,
        tween(180),
        label = "search_border"
    )
    val iconTint by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(180),
        label = "search_icon"
    )

    BasicTextField(
        value         = query,
        onValueChange = onQueryChange,
        singleLine    = true,
        textStyle     = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Surface(
                shape  = RoundedCornerShape(17.dp),
                color  = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    borderWidth,
                    MaterialTheme.colorScheme.primary.copy(alpha = if (focused) 1f else 0f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search App",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = scaleIn(tween(140)) + fadeIn(tween(140)),
                        exit = scaleOut(tween(120)) + fadeOut(tween(120))
                    ) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun FilterDropdown(
    selected: ProfileFilter,
    onSelect: (ProfileFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        if (expanded) 180f else 0f,
        tween(180),
        label = "filter_arrow"
    )

    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(17.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    Icons.Outlined.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = selected.label(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(arrowRotation)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(18.dp)
        ) {
            profileFilters.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            filter.label(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (filter == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            filter.icon(),
                            contentDescription = null,
                            tint = if (filter == selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (filter == selected) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(filter)
                    }
                )
            }
        }
    }
}

@Composable
private fun AppProfileStatusCard(
    enabled: Boolean,
    activeCount: Int,
    totalApps: Int,
    onToggle: () -> Unit
) {
    val bg by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(220),
        label = "profile_status_bg"
    )
    val accent by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220),
        label = "profile_status_accent"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) 0.20f else 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Apps,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "App Profile",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (enabled) "$activeCount active • $totalApps apps" else "Disabled • tap switch to enable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.86f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyListHint(isSearch: Boolean) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                if (isSearch) Icons.Outlined.SearchOff else Icons.Outlined.AppsOutage,
                null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
            Text(
                if (isSearch) s.appProfileNoResults else s.appProfileEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App List Item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppListItem(
    app: AppInfo,
    profile: AppProfile?,
    onClick: () -> Unit
) {
    val currentProfile = profile ?: defaultProfile(app.packageName)
    val isEnabled = currentProfile.enabled

    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { app.icon?.let { drawableToBitmap(it) } }.getOrNull()
        }
    }

    val cardBg by animateColorAsState(
        if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.13f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(220), label = "app_card_bg"
    )
    val cardBorder by animateColorAsState(
        if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.07f),
        tween(220), label = "app_card_border"
    )

    val summary = remember(isEnabled, profile) {
        if (profile != null && isEnabled) {
            val parts = mutableListOf<String>()
            parts += profileModeLabel(profile)
            val rateLabel = refreshRateLabel(currentProfile.refreshRate)
            if (rateLabel.lowercase() != "default") parts += rateLabel
            parts.joinToString(" • ")
        } else ""
    }

    Surface(
        shape  = RoundedCornerShape(22.dp),
        color  = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIconView(
                bitmap     = iconBitmap,
                label      = app.label,
                isEnabled  = isEnabled,
                size       = 46.dp,
                cornerSize = 14.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    app.label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    app.packageName,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (summary.isNotEmpty()) {
                    Text(
                        text     = summary,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// returns a conservative default of 60Hz.  The returned value is in Hertz.
private fun getMaxRefreshRate(context: Context): Int {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = context.display
            val modes = display?.supportedModes
            val maxRate = modes?.maxByOrNull { it.refreshRate }?.refreshRate
                ?: display?.refreshRate ?: 60f
            maxRate.toInt()
        } else {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            display?.refreshRate?.toInt() ?: 60
        }
    } catch (_: Exception) {
        60
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// App Chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppChip(label: String, icon: ImageVector, active: Boolean) {
    val bg  = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
              else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg  = if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant
    val brd = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
              else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Surface(
        shape    = RoundedCornerShape(50),
        color    = bg,
        border   = BorderStroke(0.6.dp, brd),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = fg)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App Icon View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppIconView(
    bitmap: Bitmap?, label: String,
    isEnabled: Boolean, size: Dp, cornerSize: Dp
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerSize)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                painter     = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = label,
                modifier    = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label.take(1).uppercase(),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (isEnabled) MaterialTheme.colorScheme.onPrimary
                                 else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Sheet Editor
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileEditor(
    profile: AppProfile,
    appLabel: String,
    appIcon: android.graphics.drawable.Drawable?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppProfile) -> Unit
) {
    var draft by remember(profile) { mutableStateOf(profile) }
    val s     = LocalStrings.current

    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = profile.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { appIcon?.let { drawableToBitmap(it) } }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle       = { BottomSheetDefaults.DragHandle() },
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── App header ───────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIconView(
                    bitmap     = iconBitmap,
                    label      = appLabel,
                    isEnabled  = draft.enabled,
                    size       = 56.dp,
                    cornerSize = 14.dp
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        appLabel,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        draft.packageName,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Switch(
                        checked         = draft.enabled,
                        onCheckedChange = { draft = draft.copy(enabled = it) },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        if (draft.enabled) s.appProfileEditorActive else s.appProfileEditorInactive,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (draft.enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // ── CPU Governor ─────────────────────────────────────────
            EditorSection(icon = Icons.Outlined.Memory, title = s.appProfileCpuGovernor) {
                GovernorSelector(
                    selected = draft.cpuGovernor,
                    onSelect = { draft = draft.copy(cpuGovernor = it) },
                    enabled  = draft.enabled
                )
            }

            // ── Refresh Rate ─────────────────────────────────────────
            EditorSection(icon = Icons.Outlined.DisplaySettings, title = s.appProfileRefreshRate) {
                RefreshRateSelector(
                    selected = draft.refreshRate,
                    onSelect = { draft = draft.copy(refreshRate = it) },
                    enabled  = draft.enabled
                )
            }

            // ── Extra Tweaks ─────────────────────────────────────────
            EditorSection(icon = Icons.Outlined.Tune, title = s.appProfileExtraTweaks) {
                ExtraTweaksPanel(
                    tweaks   = draft.extraTweaks,
                    enabled  = draft.enabled,
                    onChange = { draft = draft.copy(extraTweaks = it) }
                )
            }

            // ── Save button ──────────────────────────────────────────
            Button(
                onClick  = { onSave(draft) },
                enabled  = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.appProfileSaveBtn, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Editor Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditorSection(
    icon: ImageVector, title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                title,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

@Composable
private fun GovernorSelector(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    val s = LocalStrings.current
    val govGroups = listOf(
        listOf("default", "performance", "powersave"),
        listOf("ondemand", "conservative")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        govGroups.forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEach { gov ->
                    GovernorChip(
                        label    = CpuGovernors.labels[gov] ?: gov,
                        icon     = govIcon(gov),
                        selected = selected == gov,
                        enabled  = enabled,
                        modifier = Modifier.weight(1f),
                        onClick  = { onSelect(gov) }
                    )
                }
            }
        }

        AnimatedContent(selected, label = "gov_desc") { gov ->
            val desc = govDescription(gov, s)
            if (desc.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Info, null,
                            modifier = Modifier.size(14.dp).padding(top = 1.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GovernorChip(
    label: String, icon: ImageVector, selected: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val bg by animateColorAsState(
        when {
            selected  -> MaterialTheme.colorScheme.primary
            !enabled  -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            else      -> MaterialTheme.colorScheme.surfaceContainerHigh
        }, label = "gov_bg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "gov_fg"
    )

    Surface(
        onClick  = { if (enabled) onClick() },
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = bg,
        border   = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
        enabled  = enabled
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = fg)
            Text(
                label,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = fg,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RefreshRateSelector(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RefreshRates.all.forEach { rate ->
            val isSelected = selected == rate
            val bg by animateColorAsState(
                when {
                    isSelected -> MaterialTheme.colorScheme.secondary
                    !enabled   -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    else       -> MaterialTheme.colorScheme.surfaceContainerHigh
                }, label = "rr_chip_$rate"
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "rr_fg_$rate"
            )
            Surface(
                onClick  = { if (enabled) onSelect(rate) },
                shape    = RoundedCornerShape(12.dp),
                color    = bg,
                enabled  = enabled,
                border   = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp), tint = fg)
                    Text(
                        RefreshRates.labels[rate] ?: rate,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = fg
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraTweaksPanel(
    tweaks: AppExtraTweaks,
    enabled: Boolean,
    onChange: (AppExtraTweaks) -> Unit
) {
    val s = LocalStrings.current

    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.fillMaxWidth()) {
            ExtraTweakRow(
                icon     = Icons.Outlined.BatterySaver,
                title    = s.appProfileDisableDoze,
                subtitle = s.appProfileDisableDozeDesc,
                checked  = tweaks.disableDoze,
                enabled  = enabled
            ) { onChange(tweaks.copy(disableDoze = it)) }
            ExtraDivider()
            ExtraTweakRow(
                icon     = Icons.Outlined.Speed,
                title    = s.appProfileLockCpuMin,
                subtitle = s.appProfileLockCpuMinDesc,
                checked  = tweaks.lockCpuMin,
                enabled  = enabled
            ) { onChange(tweaks.copy(lockCpuMin = it)) }
            ExtraDivider()
            ExtraTweakRow(
                icon     = Icons.Outlined.CleaningServices,
                title    = s.appProfileKillBg,
                subtitle = s.appProfileKillBgDesc,
                checked  = tweaks.killBackground,
                enabled  = enabled
            ) { onChange(tweaks.copy(killBackground = it)) }
            ExtraDivider()
            ExtraTweakRow(
                icon     = Icons.Outlined.Videocam,
                title    = s.appProfileGpuBoost,
                subtitle = s.appProfileGpuBoostDesc,
                checked  = tweaks.gpuBoost,
                enabled  = enabled
            ) { onChange(tweaks.copy(gpuBoost = it)) }
            ExtraDivider()
            ExtraTweakRow(
                icon     = Icons.Outlined.Storage,
                title    = s.appProfileIoLatency,
                subtitle = s.appProfileIoLatencyDesc,
                checked  = tweaks.ioLatency,
                enabled  = enabled
            ) { onChange(tweaks.copy(ioLatency = it)) }
        }
    }
}

@Composable
private fun ExtraTweakRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val iconTint by animateColorAsState(
            if (checked && enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "extra_icon"
        )
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = iconTint)

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked         = checked,
            onCheckedChange = { if (enabled) onChange(it) },
            enabled         = enabled,
            modifier        = Modifier.scale(0.8f),
            colors          = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun ExtraDivider() = HorizontalDivider(
    modifier  = Modifier.padding(horizontal = 14.dp),
    thickness = 0.5.dp,
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
)

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
    val w      = drawable.intrinsicWidth.coerceIn(1, 512)
    val h      = drawable.intrinsicHeight.coerceIn(1, 512)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun govIcon(gov: String): ImageVector = when (gov) {
    "performance"  -> Icons.Outlined.Speed
    "powersave"    -> Icons.Outlined.BatterySaver
    "ondemand"     -> Icons.Outlined.AutoMode
    "conservative" -> Icons.AutoMirrored.Filled.TrendingDown
    "schedutil"    -> Icons.Outlined.Schedule
    "interactive"  -> Icons.Outlined.TouchApp
    else           -> Icons.Outlined.Tune
}

private fun govDescription(gov: String, s: dev.aether.manager.i18n.AppStrings): String = when (gov) {
    "default"      -> s.govDescDefault
    "performance"  -> s.govDescPerformance
    "powersave"    -> s.govDescPowersave
    "ondemand"     -> s.govDescOndemand
    "conservative" -> s.govDescConservative
    "schedutil"    -> s.govDescSchedutil
    "interactive"  -> s.govDescInteractive
    else           -> ""
}
