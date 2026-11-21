package com.blackstack.beatbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.updates.UpdateInfo

/**
 * Açılışta `update.json` yeni sürüm bildirdiğinde gösterilen diyalog.
 * "İndir" APK'yı indirip yükleyiciyi açar, "Sonra" bu sürümü atlar.
 */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    currentVersionName: String,
    strings: Strings,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            Button(
                onClick = onDownload,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(strings.get("update_download"), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.get("update_later"))
            }
        },
        title = {
            Text(
                text = strings.get("update_available"),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${strings.get("update_version")}: ${info.versionName}  " +
                        "(v$currentVersionName)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (info.changelog.isNotBlank()) {
                    Text(
                        text = info.changelog,
                        fontSize = 14.sp,
                    )
                }
            }
        },
    )
}
