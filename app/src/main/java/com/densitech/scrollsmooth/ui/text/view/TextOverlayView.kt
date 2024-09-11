package com.densitech.scrollsmooth.ui.text.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.text.model.TextAlignmentEnum
import com.densitech.scrollsmooth.ui.text.model.TextConfigEnum
import com.densitech.scrollsmooth.ui.text.model.TextOverlayParams
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun TextOverlayView(
    selectedTextOverlay: TextOverlayParams,
    onDoneClick: (TextOverlayParams) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localDensity = LocalDensity.current
    val localConfiguration = LocalConfiguration.current
    val focusRequester = remember {
        FocusRequester()
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    val fonts = listOf(
        FontFamily.Monospace.name,
        FontFamily.Serif.name,
        FontFamily.Cursive.name,
        FontFamily.SansSerif.name
    )

    // State management
    var selectedFont by remember { mutableStateOf(selectedTextOverlay.font) }
    var textColor by remember { mutableStateOf(selectedTextOverlay.textColor) }
    var fontSize by remember { mutableFloatStateOf(selectedTextOverlay.fontSize) }
    var textOffset by remember {
        mutableStateOf(
            Offset(
                x = selectedTextOverlay.textX,
                y = selectedTextOverlay.textY
            )
        )
    }
    var textConfig by remember { mutableStateOf(TextConfigEnum.FONT) }
    var textAlignmentEnum by remember { mutableStateOf(TextAlignmentEnum.CENTER) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = selectedTextOverlay.text,
                selection = TextRange(selectedTextOverlay.text.length)
            )
        )
    }

    fun buildResultParams(): TextOverlayParams {
        val key = selectedTextOverlay.key.ifEmpty {
            UUID.randomUUID().toString()
        }

        val offset = if (selectedTextOverlay.key.isEmpty()) {
            textOffset
        } else {
            Offset(x = selectedTextOverlay.textX, y = selectedTextOverlay.textY)
        }

        val textAlignment = if (selectedTextOverlay.key.isEmpty()) {
            textAlignmentEnum
        } else {
            TextAlignmentEnum.FLEXIBLE
        }

        return TextOverlayParams(
            key = key,
            text = textFieldValue.text,
            textColor = textColor,
            fontSize = fontSize,
            font = selectedFont,
            textX = offset.x,
            textY = offset.y,
            scale = 1f,
            rotationAngle = 0f,
            textAlignment = textAlignment
        )
    }

    LaunchedEffect(true) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Calculate to make text center of a screen
    LaunchedEffect(textAlignmentEnum) {
        val screenWidth = with(localDensity) {
            localConfiguration.screenWidthDp.dp.toPx()
        }
        val screenHeight = with(localDensity) {
            localConfiguration.screenHeightDp.dp.toPx()
        }

        // Assuming the text width and height (you can adjust these)
        val textWidth = screenWidth * 0.2f
        val offsetHeight = screenHeight / 4
        textOffset = when (textAlignmentEnum) {
            TextAlignmentEnum.LEFT -> {
                Offset(
                    x = screenWidth * 0.1f,
                    y = offsetHeight
                )
            }

            TextAlignmentEnum.CENTER, TextAlignmentEnum.FLEXIBLE -> {
                Offset(
                    x = (screenWidth - textWidth) / 2,
                    y = offsetHeight
                )
            }

            TextAlignmentEnum.RIGHT -> {
                Offset(
                    x = screenWidth * 0.9f - textWidth,
                    y = offsetHeight
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize() // Occupy full screen, allowing unrestricted drag
            .background(Color.Black.copy(alpha = 0.7f))
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    // Send the tap event to the parent
                    keyboardController?.hide()

                    // Send event to parent
                    onDoneClick(buildResultParams())
                }
            }
    ) {
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue.copy(
                    text = newValue.text,
                    selection = TextRange(newValue.text.length)
                )
            },
            label = {},
            modifier = Modifier
                .offset { IntOffset(textOffset.x.roundToInt(), textOffset.y.roundToInt()) }
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color.White,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
            ),
            textStyle = TextStyle(
                fontSize = with(LocalDensity.current) { (fontSize).toSp() },
                color = textColor,
                fontFamily = stringToFont(selectedFont)
            ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            IconButton(onClick = {
                textAlignmentEnum = when (textAlignmentEnum) {
                    TextAlignmentEnum.LEFT -> TextAlignmentEnum.CENTER
                    TextAlignmentEnum.CENTER -> TextAlignmentEnum.RIGHT
                    TextAlignmentEnum.RIGHT -> TextAlignmentEnum.LEFT
                    TextAlignmentEnum.FLEXIBLE -> TextAlignmentEnum.CENTER
                }
            }) {
                val alignmentIcon = when (textAlignmentEnum) {
                    TextAlignmentEnum.LEFT -> R.drawable.baseline_format_align_left_24
                    TextAlignmentEnum.CENTER -> R.drawable.baseline_format_align_center_24
                    TextAlignmentEnum.RIGHT -> R.drawable.baseline_format_align_right_24
                    TextAlignmentEnum.FLEXIBLE -> R.drawable.baseline_format_align_center_24
                }
                Icon(
                    painter = painterResource(id = alignmentIcon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = {
                textConfig = when (textConfig) {
                    TextConfigEnum.FONT -> TextConfigEnum.COLOR
                    TextConfigEnum.COLOR -> TextConfigEnum.FONT
                }
            }) {
                val textConfigIcon = when (textConfig) {
                    TextConfigEnum.FONT -> R.drawable.baseline_font_download_24
                    TextConfigEnum.COLOR -> R.drawable.baseline_color_lens_24
                }
                Icon(
                    painter = painterResource(id = textConfigIcon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        TextButton(
            onClick = {
                onDoneClick.invoke(buildResultParams())
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Done",
                color = Color.White,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }

        // Show font size slider
        Slider(
            value = fontSize,
            onValueChange = {
                fontSize = it
            },
            valueRange = 12f..128f,
            modifier = Modifier
                .padding(top = 30.dp)
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxWidth,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .width(200.dp)
                .height(30.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (textConfig) {
                TextConfigEnum.FONT -> {
                    FontPreviewView(
                        fonts = fonts,
                        selectedFont = selectedFont,
                        onFontSelected = {
                            selectedFont = it
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }

                TextConfigEnum.COLOR -> {
                    ColorSelectionView(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        textColor = it
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun TextOverlayPreview() {
    TextOverlayView(onDoneClick = {}, selectedTextOverlay = TextOverlayParams.default())
}