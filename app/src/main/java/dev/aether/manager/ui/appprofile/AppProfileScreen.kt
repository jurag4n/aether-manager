package dev.aether.manager.ui.appprofile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.aether.manager.data.*
import dev.aether.manager.i18n.LocalStrings
import androidx.compose.ui.platform.LocalContext
import dev.aether.manager.ui.home.TabSectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Screen root ─────────────────────────────────────────────────────────────

@Composable
fun AppProfileScreen(vm: AppProfileViewModel) {
    val state by vm.state.collectAsState()
    val editing by vm.editingProfile.collectAsState()
    val ctx = LocalContext.current
    val snack by vm.snack.collectAsState()
    val savingPkg by vm.savingPkg.collectAsState()

    LaunchedEffect(snack) {
        if (snack != null) {
            android.widget.Toast.makeText(ctx, snack, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearSnack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(bottom = pad.calculateBottomPadding()).fillMaxSize()) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                label = "apps_state"
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
        val apps = (state as? AppsUiState.Ready)?.apps ?: emptyList()
        val appInfo = apps.find { it.packageName == editTarget.packageName }
        AppProfileEditor(
            profile   = editTarget,
            appLabel  = appInfo?.label ?: editTarget.packageName,
            appIcon   = appInfo?.icon,
            saving    = vm.savingPkg.collectAsState().value == editTarget.packageName,
            onDismiss = { vm.closeEditor() },
            onSave    = { vm.saveProfile(it) },
        )
    }
}

// ─── State screens ────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(s.appProfileLoading, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorContent(msg: String, onRetry: () -> Unit) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onRetry) { Text(s.appProfileRetry) }
        }
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Header + Search (fixed, tidak scroll) ─────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            TabSectionTitle(
                icon  = Icons.Outlined.Apps,
                title = s.appProfileTitle,
                trailing = {
                        val monitorBg by animateColorAsState(
                            if (state.monitorRunning) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            label = "monitor_bg"
                        )
                        val monitorFg by animateColorAsState(
                            if (state.monitorRunning) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "monitor_fg"
                        )
                        Surface(
                            onClick = { vm.toggleMonitor(!state.monitorRunning) },
                            shape = RoundedCornerShape(50),
                            color = monitorBg,
                            border = if (!state.monitorRunning)
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            else null,
                        ) {
                            Row(
                                Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(monitorFg))
                                Text(
                                    if (state.monitorRunning) s.appProfileMonitorOn else s.appProfileMonitorOff,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = monitorFg,
                                )
                            }
                        }
                    }
                )

            SearchFilterBar(query = searchQuery, onQueryChange = { searchQuery = it })

            // "4 selected" hint mirip TrickyStore
            AnimatedVisibility(visible = activeCount > 0) {
                Text(
                    text = s.appProfileActiveCount.format(activeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 6.dp),
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyListHint(searchQuery.isNotEmpty())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val profile = state.profiles[app.packageName]
                    AppListItem(
                        app      = app,
                        profile  = profile,
                        onClick  = { vm.openEditor(app) },
                        onDelete = if (profile != null) {{ vm.deleteProfile(app.packageName) }} else null,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}



// ─── Search / Filter bar ──────────────────────────────────────────────────────

@Composable
private fun SearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val s = LocalStrings.current
    var isFocused by remember { mutableStateOf(false) }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label = "search_border"
    )
    val bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val focusedBorderColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            decorationBox = { innerTextField ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bgColor,
                    border = androidx.compose.foundation.BorderStroke(
                        width = animateDpAsState(
                            if (isFocused) 1.5.dp else 0.dp,
                            tween(200), label = "border_width"
                        ).value,
                        color = focusedBorderColor.copy(alpha = borderAlpha)
                    ),
                    tonalElevation = if (isFocused) 2.dp else 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val iconColor by animateColorAsState(
                            targetValue = if (isFocused) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(200),
                            label = "icon_color"
                        )
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = s.appProfileSearchHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = query.isNotEmpty(),
                            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                            exit  = scaleOut(tween(150)) + fadeOut(tween(150))
                        ) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyListHint(isSearch: Boolean) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (isSearch) Icons.Outlined.SearchOff else Icons.Outlined.AppsOutage,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                if (isSearch) s.appProfileNoResults else s.appProfileEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── App List Item (TrickyStore style) ───────────────────────────────────────

@Composable
private fun AppListItem(
    app: AppInfo,
    profile: AppProfile?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val hasProfile = profile != null
    val isEnabled  = profile?.enabled == true
    var showDeleteDialog by remember { mutableStateOf(false) }

    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { app.icon?.let { drawableToBitmap(it) } }.getOrNull()
        }
    }

    // Chip data: hanya tampilkan setting non-default
    val chips = remember(profile) {
        buildList {
            val gov = profile?.cpuGovernor ?: "default"
            val rr  = profile?.refreshRate  ?: "default"
            val extra = profile?.extraTweaks
            if (gov != "default") add(Pair(gov.replaceFirstChar { it.uppercase() }, Icons.Filled.Memory))
            if (rr  != "default") add(Pair("${rr}Hz", Icons.Filled.DisplaySettings))
            if (extra?.gpuBoost == true)    add(Pair("GPU", Icons.Outlined.Videocam))
            if (extra?.disableDoze == true) add(Pair("Doze", Icons.Outlined.BatterySaver))
            if (extra?.ioLatency  == true)  add(Pair("IO", Icons.Outlined.Storage))
        }
    }

    val cardBg by animateColorAsState(
        targetValue = if (isEnabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(250),
        label = "card_bg"
    )
    val cardBorder by animateColorAsState(
        targetValue = if (isEnabled)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        animationSpec = tween(250),
        label = "card_border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // App icon
            AppIconView(bitmap = iconBitmap, label = app.label, isEnabled = isEnabled, size = 46.dp, cornerSize = 12.dp)

            // Name + package + chips
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chips.forEach { (label, icon) ->
                            AppChip(label = label, icon = icon, active = isEnabled)
                        }
                    }
                }
            }

            // Checkbox (mirip TrickyStore checked state)
            val checkColor by animateColorAsState(
                targetValue = if (isEnabled) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                animationSpec = tween(200),
                label = "check_color"
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = checkColor,
                        shape = RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isEnabled,
                    enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit  = scaleOut(tween(150)) + fadeOut(),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
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

// ─── App chip (TrickyStore style: Auto / Gov / Tweaks) ───────────────────────

@Composable
private fun AppChip(label: String, icon: ImageVector, active: Boolean) {
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (active) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(0.5.dp, fg.copy(alpha = 0.25f)),
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(icon, null, modifier = Modifier.size(10.dp), tint = fg)
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 10.sp)
        }
    }
}

// ─── Shared icon view ─────────────────────────────────────────────────────────

@Composable
private fun AppIconView(bitmap: Bitmap?, label: String, isEnabled: Boolean, size: Dp, cornerSize: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(cornerSize)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(painter = BitmapPainter(bitmap.asImageBitmap()), contentDescription = label,
                modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(
                if (isEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            ), contentAlignment = Alignment.Center) {
                Text(label.take(1).uppercase(), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}



// ─── Bottom Sheet Editor ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileEditor(
    profile: AppProfile,
    appLabel: String,
    appIcon: android.graphics.drawable.Drawable?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppProfile) -> Unit,
) {
    var draft by remember(profile) { mutableStateOf(profile) }
    val s = LocalStrings.current
    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = profile.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { appIcon?.let { drawableToBitmap(it) } }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle    = { BottomSheetDefaults.DragHandle() },
        shape         = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AppIconView(bitmap = iconBitmap, label = appLabel, isEnabled = draft.enabled, size = 52.dp, cornerSize = 14.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(appLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(draft.packageName, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(checked = draft.enabled, onCheckedChange = { draft = draft.copy(enabled = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary))
                    Text(if (draft.enabled) s.appProfileEditorActive else s.appProfileEditorInactive,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (draft.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            EditorSectionHeader(Icons.Filled.Memory, s.appProfileCpuGovernor)
            GovernorSelector(selected = draft.cpuGovernor, onSelect = { draft = draft.copy(cpuGovernor = it) }, enabled = draft.enabled)
            EditorSectionHeader(Icons.Filled.DisplaySettings, s.appProfileRefreshRate)
            RefreshRateSelector(selected = draft.refreshRate, onSelect = { draft = draft.copy(refreshRate = it) }, enabled = draft.enabled)
            EditorSectionHeader(Icons.Filled.Tune, s.appProfileExtraTweaks)
            ExtraTweaksPanel(tweaks = draft.extraTweaks, enabled = draft.enabled, onChange = { draft = draft.copy(extraTweaks = it) })
            Spacer(Modifier.height(4.dp))
            Button(onClick = { onSave(draft) }, enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.appProfileSaveBtn, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun EditorSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GovernorSelector(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("default", "performance", "powersave").forEach { gov ->
                GovernorChip(label = CpuGovernors.labels[gov] ?: gov, icon = govIcon(gov),
                    selected = selected == gov, enabled = enabled, modifier = Modifier.weight(1f), onClick = { onSelect(gov) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ondemand", "conservative").forEach { gov ->
                GovernorChip(label = CpuGovernors.labels[gov] ?: gov, icon = govIcon(gov),
                    selected = selected == gov, enabled = enabled, modifier = Modifier.weight(1f), onClick = { onSelect(gov) })
            }
        }
        AnimatedContent(selected, label = "gov_desc") { gov ->
            val desc = govDescription(gov, s)
            if (desc.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, null, modifier = Modifier.size(13.dp).padding(top = 1.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun GovernorChip(label: String, icon: ImageVector, selected: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else if (!enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant, label = "gov_chip_bg")
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "gov_chip_fg")
    Surface(onClick = { if (enabled) onClick() }, modifier = modifier, shape = RoundedCornerShape(12.dp), color = bg,
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null, enabled = enabled) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = fg)
            Text(label, style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RefreshRateSelector(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RefreshRates.all.forEach { rate ->
            val isSelected = selected == rate
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.secondary
                else if (!enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant, label = "rr_chip_$rate")
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, label = "rr_fg_$rate")
            Surface(onClick = { if (enabled) onSelect(rate) }, shape = RoundedCornerShape(12.dp), color = bg, enabled = enabled,
                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp), tint = fg)
                    Text(RefreshRates.labels[rate] ?: rate, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = fg)
                }
            }
        }
    }
}

@Composable
private fun ExtraTweaksPanel(tweaks: AppExtraTweaks, enabled: Boolean, onChange: (AppExtraTweaks) -> Unit) {
    val s = LocalStrings.current
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))) {
        Column(Modifier.fillMaxWidth()) {
            TweakToggleRow(Icons.Outlined.BatterySaver, s.appProfileDisableDoze, s.appProfileDisableDozeDesc,
                tweaks.disableDoze, enabled) { onChange(tweaks.copy(disableDoze = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.Speed, s.appProfileLockCpuMin, s.appProfileLockCpuMinDesc,
                tweaks.lockCpuMin, enabled) { onChange(tweaks.copy(lockCpuMin = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.CleaningServices, s.appProfileKillBg, s.appProfileKillBgDesc,
                tweaks.killBackground, enabled) { onChange(tweaks.copy(killBackground = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.Videocam, s.appProfileGpuBoost, s.appProfileGpuBoostDesc,
                tweaks.gpuBoost, enabled) { onChange(tweaks.copy(gpuBoost = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.Storage, s.appProfileIoLatency, s.appProfileIoLatencyDesc,
                tweaks.ioLatency, enabled, isLast = true) { onChange(tweaks.copy(ioLatency = it)) }
        }
    }
}

@Composable
private fun TweakToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean,
    enabled: Boolean, isLast: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }
        .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(
            if (checked && enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(17.dp),
                tint = if (checked && enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { if (enabled) onChange(it) }, enabled = enabled,
            modifier = Modifier.scale(0.75f),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
    val w = drawable.intrinsicWidth.coerceIn(1, 512)
    val h = drawable.intrinsicHeight.coerceIn(1, 512)
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