package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncRecord
import com.example.ui.viewmodel.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    jianguoUser: String,
    jianguoPass: String,
    jianguoUrl: String,
    lastSyncTime: String,
    syncStatus: SyncStatus,
    syncRecords: List<SyncRecord>,
    onSaveCredentials: (String, String, String) -> Unit,
    onTestConnection: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onResetStatus: () -> Unit
) {
    var user by remember { mutableStateOf(jianguoUser) }
    var pass by remember { mutableStateOf(jianguoPass) }
    var url by remember { mutableStateOf(jianguoUrl) }

    var showGuide by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    // Update form fields if preferences change in background
    LaunchedEffect(jianguoUser, jianguoPass, jianguoUrl) {
        user = jianguoUser
        pass = jianguoPass
        url = jianguoUrl
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统设置与同步", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- Sync Status Alert Banner ---
            if (syncStatus !is SyncStatus.Idle) {
                item {
                    val containerColor = when (syncStatus) {
                        is SyncStatus.Success -> Color(0xFFE8F5E9)
                        is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer
                        is SyncStatus.Loading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }
                    val textColor = when (syncStatus) {
                        is SyncStatus.Success -> Color(0xFF2E7D32)
                        is SyncStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                        is SyncStatus.Loading -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> Color.Black
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (syncStatus) {
                                    is SyncStatus.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    is SyncStatus.Success -> Icon(Icons.Default.CloudDone, contentDescription = null, tint = textColor)
                                    is SyncStatus.Error -> Icon(Icons.Default.Info, contentDescription = null, tint = textColor)
                                    else -> {}
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when (syncStatus) {
                                        is SyncStatus.Loading -> syncStatus.message
                                        is SyncStatus.Success -> syncStatus.message
                                        is SyncStatus.Error -> syncStatus.message
                                        else -> ""
                                    },
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = onResetStatus) {
                                Text("关闭", style = MaterialTheme.typography.labelSmall, color = textColor)
                            }
                        }
                    }
                }
            }

            // --- Jianguoyun Credentials Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "坚果云 WebDAV 同步配置",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showGuide = !showGuide }) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "使用帮助",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Guide details dropdown
                        AnimatedVisibility(visible = showGuide) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "如何配置坚果云 WebDAV 同步？\n" +
                                            "1. 打开坚果云网页版，点击右上角【账户信息】\n" +
                                            "2. 选择【安全选项】页面\n" +
                                            "3. 在最下方【第三方应用管理】中点击【添加应用密码】\n" +
                                            "4. 输入应用名称，生成一个【应用密码】（格式如: abcd-efgh-ijkl-mnop）\n" +
                                            "5. 在本页面输入坚果云注册邮箱（账号）和生成的应用密码，保存即可实现同步功能。",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = user,
                            onValueChange = { user = it },
                            label = { Text("坚果云账号 (邮箱)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_username"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = pass,
                            onValueChange = { pass = it },
                            label = { Text("第三方应用密码") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_password"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("服务器 WebDAV URL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_url"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSaveCredentials(user.trim(), pass.trim(), url.trim())
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("保存配置")
                            }

                            Button(
                                onClick = {
                                    onSaveCredentials(user.trim(), pass.trim(), url.trim())
                                    onTestConnection()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("settings_test_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("保存并测试连接")
                            }
                        }
                    }
                }
            }

            // --- Backup & Restore Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "云端同步与备份恢复",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("上次同步时间", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = lastSyncTime,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onBackupClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_backup_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("手动备份数据到坚果云 (上传)")
                            }

                            Button(
                                onClick = { showRestoreConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_restore_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("从坚果云恢复数据 (下载覆盖)")
                            }
                        }
                    }
                }
            }

            // --- Sync Audit Log Title ---
            item {
                Text(
                    text = "同步活动审计日志",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (syncRecords.isEmpty()) {
                item {
                    Text(
                        text = "暂无备份同步活动记录",
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(syncRecords, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_record_${record.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (record.syncType == "上传") MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = record.syncType,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (record.syncType == "上传") MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Text(
                                        text = record.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = record.syncTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (record.errorMessage != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = record.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (record.status == "成功") Color(0xFFE8F5E9)
                                        else Color(0xFFFFEBEE)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = record.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.status == "成功") Color(0xFF2E7D32)
                                    else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Restore Data Confirmation Dialog ---
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("确认执行云端恢复？") },
            text = { Text("从坚果云恢复将下载云端的 loantracker_sync.json 备份并清空当前的全部本地数据，完全使用备份数据进行覆盖。\n\n该操作无法撤销，确定要覆盖吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        onRestoreClick()
                    },
                    modifier = Modifier.testTag("restore_confirm_ok")
                ) {
                    Text("覆盖恢复", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
