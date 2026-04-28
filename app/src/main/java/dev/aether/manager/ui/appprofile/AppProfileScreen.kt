package dev.aether.manager.ui.appprofile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import dev.aether.manager.ui.home.TabSectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppProfileScreen(vm: AppProfileViewModel) {
    val state    by vm.state.collectAsState()
    val editing  by vm.editingProfile.collectAsState()
    val ctx      = LocalContext.current
    val snack    by vm.snack.collectAsState()
    val savingPkg by vm.savingPkg.collectAsState()

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
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
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

    val editTarget = editing
    if (editTarget != null) {
        val apps    = (state as? AppsUiState.Ready)?.apps ?: emptyList()
        val appInfo = apps.find { it.packageName == editTarget.packageName }
        AppProfileEditor(
            profile   = editTarget,
            appLabel  = appInfo?.label ?: editTarget.packageName,
            appIcon   = appInfo?.icon,
            saving    = savingPkg == editTarget.packageName,
            onDismiss = { vm.closeEditor() },
            onSave    = { vm.saveProfile(it) }
        )
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

@Composable
private fun ReadyContent(state: AppsUiState.Ready, vm: AppProfileViewModel) {
    val s = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(state.apps, state.profiles, searchQuery) {
        state.apps.filter { app ->
            app.label.contains(searchQuery, ignoreCase = true) ||
            app.packageName.contains(searchQuery, ignoreCase = true)
        }
    }
    val activeCount = remember(state.profiles) {
        state.profiles.values.count { it.enabled }
    }

    Column(Modifier.fillMaxSize()) {
        // ── Fixed header ─────────────────────────────────────────────
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TabSectionTitle(
                    icon  = Icons.Outlined.Apps,
                    title = s.appProfileTitle
                )
                MonitorPill(
                    running = state.monitorRunning,
                    onToggle = { vm.toggleMonitor(!state.monitorRunning) }
                )
            }

            SearchFilterBar(query = searchQuery, onQueryChange = { searchQuery = it })

            AnimatedVisibility(visible = activeCount > 0) {
                Text(
                    text  = s.appProfileActiveCount.format(activeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )
            }
        }

        // ── App list ─────────────────────────────────────────────────
        if (filtered.isEmpty()) {
            EmptyListHint(searchQuery.isNotEmpty())
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(
                    start  = 16.dp, end = 16.dp,
                    top    = 10.dp, bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val profile = state.profiles[app.packageName]
                    AppListItem(
                        app      = app,
                        profile  = profile,
                        onClick  = { vm.openEditor(app) },
                        onDelete = if (profile != null) {{ vm.deleteProfile(app.packageName) }} else null
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
private fun SearchFilterBar(query: String, onQueryChange: (String) -> Unit) {
    val s        = LocalStrings.current
    var focused  by remember { mutableStateOf(false) }

    val borderColor = MaterialTheme.colorScheme.primary
    val borderWidth by animateDpAsState(
        if (focused) 1.5.dp else 0.dp, tween(200), label = "search_border"
    )
    val iconTint by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "icon_color"
    )

    BasicTextField(
        value         = query,
        onValueChange = onQueryChange,
        singleLine    = true,
        textStyle     = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier    = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Surface(
                shape          = RoundedCornerShape(16.dp),
                color          = MaterialTheme.colorScheme.surfaceContainerHigh,
                border         = BorderStroke(borderWidth, borderColor.copy(alpha = if (focused) 1f else 0f)),
                modifier       = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Search, null,
                        tint     = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                s.appProfileSearchHint,
                                style  = MaterialTheme.typography.bodyMedium,
                                color  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit    = scaleOut(tween(150)) + fadeOut(tween(150))
                    ) {
                        IconButton(
                            onClick  = { onQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Clear, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    )
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
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val hasProfile = profile != null
    val isEnabled  = profile?.enabled == true
    var showDeleteDialog by remember { mutableStateOf(false) }

    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { app.icon?.let { drawableToBitmap(it) } }.getOrNull()
        }
    }

    val chips = remember(profile) {
        buildList {
            val gov   = profile?.cpuGovernor ?: "default"
            val rr    = profile?.refreshRate  ?: "default"
            val extra = profile?.extraTweaks
            if (gov != "default") add(Pair(gov.replaceFirstChar { it.uppercase() }, Icons.Filled.Memory))
            if (rr  != "default") add(Pair("${rr}Hz", Icons.Filled.DisplaySettings))
            if (extra?.gpuBoost   == true) add(Pair("GPU",  Icons.Outlined.Videocam))
            if (extra?.disableDoze == true) add(Pair("Doze", Icons.Outlined.BatterySaver))
            if (extra?.ioLatency  == true) add(Pair("IO",   Icons.Outlined.Storage))
        }
    }

    val cardBg by animateColorAsState(
        if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(250), label = "card_bg"
    )
    val cardBorder by animateColorAsState(
        if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        tween(250), label = "card_border"
    )

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(16.dp),
        color    = cardBg,
        border   = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App icon
            AppIconView(
                bitmap      = iconBitmap,
                label       = app.label,
                isEnabled   = isEnabled,
                size        = 46.dp,
                cornerSize  = 12.dp
            )

            // Name + package + chips
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    app.label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    app.packageName,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement   = Arrangement.spacedBy(4.dp)
                    ) {
                        chips.forEach { (label, icon) ->
                            AppChip(label = label, icon = icon, active = isEnabled)
                        }
                    }
                }
            }

            // Enabled checkbox
            val checkColor by animateColorAsState(
                if (isEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                tween(200), label = "check_color"
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isEnabled) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = checkColor,
                        shape = RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isEnabled,
                    enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit    = scaleOut(tween(150)) + fadeOut()
                ) {
                    Icon(
                        Icons.Filled.Check, null,
                        tint     = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        val s = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.appProfileDeleteTitle) },
            text  = { Text(s.appProfileDeleteDesc.format(app.label)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete?.invoke() }) {
                    Text(s.appProfileDeleteConfirm, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(s.appProfileBtnCancel) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App Chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppChip(label: String, icon: ImageVector, active: Boolean) {
    val bg  = if (active) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg  = if (active) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant
    val brd = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
              else MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = bg,
        border   = BorderStroke(0.5.dp, brd),
        modifier = Modifier.height(22.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(11.dp), tint = fg)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = fg, fontSize = 10.sp, maxLines = 1)
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
            EditorSection(icon = Icons.Filled.Memory, title = s.appProfileCpuGovernor) {
                GovernorSelector(
                    selected = draft.cpuGovernor,
                    onSelect = { draft = draft.copy(cpuGovernor = it) },
                    enabled  = draft.enabled
                )
            }

            // ── Refresh Rate ─────────────────────────────────────────
            EditorSection(icon = Icons.Filled.DisplaySettings, title = s.appProfileRefreshRate) {
                RefreshRateSelector(
                    selected = draft.refreshRate,
                    onSelect = { draft = draft.copy(refreshRate = it) },
                    enabled  = draft.enabled
                )
            }

            // ── Extra Tweaks ─────────────────────────────────────────
            EditorSection(icon = Icons.Filled.Tune, title = s.appProfileExtraTweaks) {
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
                    .height(52.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
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
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp), tint = fg)
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
    "performance"  -> Icons.Filled.FlashOn
    "powersave"    -> Icons.Filled.BatterySaver
    "ondemand"     -> Icons.Filled.AutoMode
    "conservative" -> Icons.AutoMirrored.Filled.TrendingDown
    "schedutil"    -> Icons.Filled.Schedule
    "interactive"  -> Icons.Filled.TouchApp
    else           -> Icons.Filled.Tune
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
