package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OwnerSectionView(
    owner: String,
    content: String,
    tags: List<String>,
    onOwnerClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpand by remember { mutableStateOf(false) }

    val combinedText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 11.sp)) {
            append(content)
            append(" ")
        }
        tags.forEach { tag ->
            val startTag = this.length
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                append("#$tag")
            }

            addStringAnnotation(
                tag = "TAG",
                annotation = tag,
                start = startTag,
                end = startTag + tag.length + 1 // for #
            )
            append(" ")
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Owner Name
        Text(
            text = owner,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onOwnerClick(owner) }
        )

        // Clickable Combined Text
        ClickableText(
            text = combinedText,
            style = TextStyle(color = Color.White),
            maxLines = if (isExpand) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp).animateContentSize()
        ) { offset ->
            // Detect if a tag was clicked
            val tagClick = combinedText.getStringAnnotations(tag = "TAG", start = offset, end = offset)
                .firstOrNull()

            if (tagClick != null) {
                onTagClick(tagClick.item)
            }
            else {
                isExpand = !isExpand
            }
        }
    }
}


@Composable
@Preview
private fun OwnerSectionViewPreview() {
    OwnerSectionView(
        modifier = Modifier.background(Color.Black),
        owner = "",
        content = "dasbjkdbsajkdasbjkdbasjkdsabjkdasbjkdndmlkasndklsadnklasndklasndnskladnklasndklsandklsandsakldmnaskldmnlasknsanmdklasndklnsakldnsandmaskldnklsanabsjkdbasjkdbasdjksabnkjd",
        tags = listOf("carnival", "newcity", "dasdsa"),
        onOwnerClick = {},
        onTagClick = {}
    )
}