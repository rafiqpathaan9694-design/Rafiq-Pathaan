package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.backup.BackupExportManager
import com.example.backup.BackupInspectResult
import com.example.data.model.StudentRecord
import com.example.i18n.LocalAppStrings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupExportScreen(
    students: List<StudentRecord>,
    isBusy: Boolean,
    onExportCsvUri: (Uri, (Result<Int>) -> Unit) -> Unit,
    onExportBackupZipUri: (Uri, (Result<Int>) -> Unit) -> Unit,
    onInspectBackupZip: (Uri, (Result<BackupInspectResult>) -> Unit) -> Unit,
    onRestoreBackupZip: (Uri, Boolean, (Result<Int>) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<BackupInspectResult?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportCsvUri(uri) { res ->
                statusBanner = res.fold(
                    onSuccess = { count -> "Exported $count student records to Excel/CSV." },
                    onFailure = { err -> "CSV export failed: ${err.message}" }
                )
            }
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            onExportBackupZipUri(uri) { res ->
                statusBanner = res.fold(
                    onSuccess = { count -> "Offline backup saved ($count students + documents)." },
                    onFailure = { err -> "Backup failed: ${err.message}" }
                )
            }
        }
    }

    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onInspectBackupZip(uri) { res ->
                res.onSuccess { info ->
                    pendingRestoreUri = uri
                    inspectInfo = info
                }.onFailure { err ->
                    statusBanner = "Invalid backup file: ${err.message}"
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.backupTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Works 100% offline • Stored locally (${students.size} forms)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isBusy) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        statusBanner?.let { msg ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 1. Excel / CSV Export
        item {
            BackupActionCard(
                icon = Icons.Default.TableChart,
                title = strings.csvExportTitle,
                description = strings.csvExportDesc
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (students.isEmpty()) {
                                Toast.makeText(context, "No student records to export", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
                            saveCsvLauncher.launch("MobileSolution_NEET2027_$stamp.csv")
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_csv")
                    ) {
                        Text(strings.saveCsvBtn)
                    }
                    OutlinedButton(
                        onClick = {
                            if (students.isEmpty()) {
                                Toast.makeText(context, "No student records to share", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            scope.launch {
                                val intent = BackupExportManager.createShareableCsvFile(context, students)
                                context.startActivity(Intent.createChooser(intent, strings.shareCsvBtn))
                            }
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_share_csv")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.shareCsvBtn)
                    }
                }
            }
        }

        // 2. Full Offline ZIP Backup
        item {
            BackupActionCard(
                icon = Icons.Default.FolderZip,
                title = strings.fullBackupTitle,
                description = strings.fullBackupDesc
            ) {
                Button(
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
                        createBackupLauncher.launch("MobileSolution_NEET2027_Backup_$stamp.zip")
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_create_backup")
                ) {
                    Text(strings.createBackupBtn, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. Restore Backup
        item {
            BackupActionCard(
                icon = Icons.Default.Restore,
                title = strings.restoreBackupTitle,
                description = strings.restoreBackupDesc
            ) {
                OutlinedButton(
                    onClick = {
                        openRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_select_restore")
                ) {
                    Text(strings.selectBackupBtn, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    val uriToRestore = pendingRestoreUri
    val info = inspectInfo
    if (uriToRestore != null && info != null) {
        AlertDialog(
            onDismissRequest = {
                pendingRestoreUri = null
                inspectInfo = null
            },
            title = { Text(strings.restoreDialogTitle.format(info.recordCount)) },
            text = {
                Text("Backup Date: ${info.exportedAt}\n\nChoose whether to merge into existing records or replace all current data.")
            },
            confirmButton = {
                Button(onClick = {
                    pendingRestoreUri = null
                    inspectInfo = null
                    onRestoreBackupZip(uriToRestore, false) { res ->
                        statusBanner = res.fold(
                            onSuccess = { count -> "Restored & merged $count student records." },
                            onFailure = { err -> "Restore failed: ${err.message}" }
                        )
                    }
                }) {
                    Text(strings.restoreMergeBtn)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        pendingRestoreUri = null
                        inspectInfo = null
                    }) {
                        Text(strings.cancelBtn)
                    }
                    TextButton(onClick = {
                        pendingRestoreUri = null
                        inspectInfo = null
                        onRestoreBackupZip(uriToRestore, true) { res ->
                            statusBanner = res.fold(
                                onSuccess = { count -> "Replaced database with $count restored records." },
                                onFailure = { err -> "Restore failed: ${err.message}" }
                            )
                        }
                    }) {
                        Text(strings.restoreReplaceBtn, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

@Composable
private fun BackupActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    actions: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            actions()
        }
    }
}
