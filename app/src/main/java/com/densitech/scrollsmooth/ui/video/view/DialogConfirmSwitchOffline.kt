package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.densitech.scrollsmooth.R

@Composable
fun DialogConfirmSwitchOffline(
    dialogTitle: String,
    dialogText: String,
    dialogConfirmText: String,
    dialogDismissText: String,
    onRetryClick: () -> Unit,
    onSwitchOfflineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.baseline_info_24),
                contentDescription = null
            )
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {

        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSwitchOfflineClick()
                }
            ) {
                Text(dialogConfirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onRetryClick()
                }
            ) {
                Text(dialogDismissText)
            }
        }
    )
}