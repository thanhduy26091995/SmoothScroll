package com.densitech.scrollsmooth.ui.text.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun DropdownSelection(
    isShowDropdownSelection: Boolean,
    onEditClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = isShowDropdownSelection,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 10.dp),
        modifier = Modifier
            .background(Color.Transparent.copy(alpha = 0.6f))
    ) {
        DropdownMenuItem(
            text = {
                Row {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = Color.White
                    )

                    Text("Edit", modifier = Modifier.padding(start = 10.dp), color = Color.White)
                }
            },
            onClick = { onEditClick.invoke() }
        )
    }
}