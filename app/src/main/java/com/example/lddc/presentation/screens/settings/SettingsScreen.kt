package com.example.lddc.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lddc.BuildConfig
import com.example.lddc.common.models.enums.LyricsFormat
import com.example.lddc.common.models.enums.Source
import com.example.lddc.core.utils.DownloadLocation
import com.example.lddc.presentation.components.designsystem.LDDCCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "设置", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(title = "搜索源", icon = Icons.Default.MusicNote) {
                SourceSettings(
                    enabledSources = uiState.enabledSources,
                    onSourceToggle = { source, enabled -> viewModel.toggleSource(source, enabled) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "歌词格式", icon = Icons.Default.Settings) {
                FormatSettings(
                    preferredFormat = uiState.preferredFormat,
                    onFormatSelected = { format -> viewModel.setPreferredFormat(format) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "显示设置", icon = Icons.Default.Translate) {
                ToggleSettingItem(
                    title = "自动显示翻译",
                    subtitle = "搜索时默认显示翻译歌词",
                    checked = uiState.showTranslation,
                    onCheckedChange = { viewModel.setShowTranslation(it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ToggleSettingItem(
                    title = "自动显示罗马音",
                    subtitle = "搜索时默认显示罗马音歌词",
                    checked = uiState.showRomanization,
                    onCheckedChange = { viewModel.setShowRomanization(it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ToggleSettingItem(
                    title = "自动隐藏底栏",
                    subtitle = "3秒无操作后隐藏，上滑屏幕恢复",
                    checked = uiState.autoHideNavBar,
                    onCheckedChange = { viewModel.setAutoHideNavBar(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "保存设置", icon = Icons.Default.Save) {
                DownloadPathSetting(
                    treeUri = uiState.downloadTreeUri,
                    onPick = { uri -> viewModel.setDownloadTreeUri(context, uri) },
                    onClear = { viewModel.clearDownloadTreeUri(context) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ToggleSettingItem(
                    title = "自动保存",
                    subtitle = "搜索后自动保存歌词到本地",
                    checked = uiState.autoSave,
                    onCheckedChange = { viewModel.setAutoSave(it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ToggleSettingItem(
                    title = "嵌入歌词",
                    subtitle = "将歌词嵌入到音频文件中",
                    checked = uiState.embedLyrics,
                    onCheckedChange = { viewModel.setEmbedLyrics(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "关于", icon = Icons.Default.Info) {
                LinkSettingItem(
                    title = "项目参考",
                    subtitle = "LDDC - 原项目 GitHub",
                    onClick = { openUrl(context, "https://github.com/chenmozhijin/LDDC") }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                InfoSettingItem(
                    title = "版本",
                    subtitle = "LDDC v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        LDDCCard(elevated = false) { Column { content() } }
    }
}

@Composable
private fun SourceSettings(enabledSources: List<Source>, onSourceToggle: (Source, Boolean) -> Unit) {
    val sources = listOf(Source.QM, Source.NE, Source.KG)
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        sources.forEachIndexed { index, source ->
            val isEnabled = source in enabledSources
            val (name, color) = when (source) {
                Source.QM -> "QQ音乐" to Color(0xFF31C27C)
                Source.NE -> "网易云音乐" to Color(0xFFE60026)
                Source.KG -> "酷狗音乐" to Color(0xFF00A9FF)
                else -> source.name to MaterialTheme.colorScheme.primary
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSourceToggle(source, !isEnabled) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (source) { Source.QM -> "Q"; Source.NE -> "N"; Source.KG -> "K"; else -> "?" },
                        color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isEnabled) "已启用" else "已禁用",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isEnabled, onCheckedChange = { onSourceToggle(source, it) })
            }
            if (index < sources.size - 1) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSettings(preferredFormat: LyricsFormat, onFormatSelected: (LyricsFormat) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val formats = LyricsFormat.entries.take(5)
    Column(modifier = Modifier.padding(16.dp)) {
        Text("首选格式", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = preferredFormat.displayName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                formats.forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.displayName) },
                        onClick = { onFormatSelected(format); expanded = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("搜索歌词时默认使用的格式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DownloadPathSetting(treeUri: String?, onPick: (Uri) -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(onPick) }
    val isCustom = treeUri != null
    val valid = DownloadLocation.isValid(context, treeUri)
    val display = DownloadLocation.displayName(context, treeUri)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("下载目录", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    !isCustom -> "默认：${DownloadLocation.DEFAULT_DISPLAY}"
                    valid -> display
                    else -> "$display（已失效，将使用默认目录）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (!isCustom || valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isCustom) TextButton(onClick = onClear) { Text("恢复默认") }
        TextButton(onClick = { launcher.launch(null) }) { Text(if (isCustom) "更改" else "选择") }
    }
}

@Composable
private fun ToggleSettingItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinkSettingItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(Icons.Default.Link, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoSettingItem(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}