package com.densitech.scrollsmooth.ui.video_transformation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.video_transformation.model.TransformationAction

@Composable
fun TransformationActionView(
    actions: List<TransformationAction>,
    onActionClick: (TransformationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        actions.forEach {
            TransformationActionItemView(action = it, onIconClick = onActionClick)

            Spacer(modifier = Modifier.width(7.dp))
        }
    }
}

@Composable
private fun TransformationActionItemView(
    action: TransformationAction,
    onIconClick: (TransformationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.DarkGray)
            .clickableNoRipple {
                onIconClick.invoke(action)
            }
    ) {
        Icon(
            painter = painterResource(id = action.iconId),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center)
        )
    }
}