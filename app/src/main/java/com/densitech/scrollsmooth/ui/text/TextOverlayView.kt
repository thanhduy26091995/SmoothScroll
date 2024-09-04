package com.densitech.scrollsmooth.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import kotlin.math.roundToInt

@Composable
fun TextOverlayPreview(onDoneClick: () -> Unit) {
    val localDensity = LocalDensity.current
    val localConfiguration = LocalConfiguration.current
    val focusRequester = remember {
        FocusRequester()
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val fonts = listOf(
        FontFamily.Monospace.name,
        FontFamily.Serif.name,
        FontFamily.Cursive.name,
        FontFamily.SansSerif.name
    )

    var isEditingTextMode by remember {
        mutableStateOf(true)
    }

    var selectedFont by remember {
        mutableStateOf(FontFamily.Monospace.name)
    }

    var textColor by remember {
        mutableStateOf(Color.White)
    }

    var text by remember {
        mutableStateOf("Hello world")
    }

    var fontSize by remember {
        mutableFloatStateOf(24f)
    }
    var scale by remember {
        mutableFloatStateOf(1f)
    }

    var textOffset by remember {
        mutableStateOf(Offset.Zero)
    }

    var rotationAngle by remember {
        mutableFloatStateOf(0f)
    }

    var isShowDropdownSelection by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(isEditingTextMode) {
        if (isEditingTextMode) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // Calculate to make text center of a screen
    LaunchedEffect(Unit) {
        val screenWidth = with(localDensity) {
            localConfiguration.screenWidthDp.dp.toPx()
        }
        val screenHeight = with(localDensity) {
            localConfiguration.screenHeightDp.dp.toPx()
        }

        // Assuming the text width and height (you can adjust these)
        val textWidth = screenWidth * 0.2f
        val textHeight = fontSize * scale * 2

        textOffset = Offset(
            x = (screenWidth - textWidth) / 2,
            y = (screenHeight - textHeight) / 2
        )
    }

//    Scaffold(modifier = Modifier
//        .fillMaxSize()
//        .background(Color.Red)) { paddingValues ->
//
//    }

    Box(
        modifier = Modifier
            .padding(paddingValues = PaddingValues(vertical = 20.dp))
            .fillMaxSize() // Occupy full screen, allowing unrestricted drag
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (!isEditingTextMode) {
                        scale *= zoom  // Update scale with zoom factor
                        scale = scale.coerceIn(0.5f, 5f)
                        rotationAngle += rotation
                        textOffset += pan
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    // Handle tap gestures here
                    if (isEditingTextMode) {
                        isEditingTextMode = false
                    }
                }
            }
    ) {
        if (isEditingTextMode) {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = {},
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .align(Alignment.Center)
                    .padding(bottom = 50.dp),
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
                    fontSize = with(LocalDensity.current) { (fontSize * scale).toSp() },
                    color = textColor,
                    fontFamily = stringToFont(selectedFont)
                ),
            )
        } else {
            // Draggable and Zoomable T   ext Overlay
            Box(
                modifier = Modifier
                    .offset { IntOffset(textOffset.x.roundToInt(), textOffset.y.roundToInt()) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotationAngle
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (textColor != Color.White) Color.Transparent else Color.Black)
                    .padding(6.dp)
            ) {
                Text(
                    text = text,
                    fontSize = with(LocalDensity.current) { (fontSize * scale).toSp() },
                    color = textColor,
                    fontFamily = stringToFont(selectedFont),
                    modifier = Modifier.clickableNoRipple {
                        isShowDropdownSelection = true
                    }
                )

                DropdownSelection(
                    isShowDropdownSelection = isShowDropdownSelection,
                    onEditClick = {
                        isEditingTextMode = true
                        isShowDropdownSelection = false
                    },
                    onDismissRequest = {
                        isShowDropdownSelection = false
                    })
            }

            TextButton(
                onClick = {
                    onDoneClick.invoke()
                },
                modifier = Modifier.align(Alignment.TopEnd)
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
                    .width(300.dp)
                    .height(50.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ColorSelectionView(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    textColor = it
                }

                FontPreviewView(
                    fonts = fonts,
                    selectedFont = selectedFont,
                    onFontSelected = {
                        selectedFont = it
                    },
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

private fun stringToFont(fontName: String): FontFamily {
    return when (fontName) {
        FontFamily.Serif.name -> FontFamily.Serif
        FontFamily.Monospace.name -> FontFamily.Monospace
        FontFamily.Cursive.name -> FontFamily.Cursive
        FontFamily.SansSerif.name -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
}

@Composable
private fun DropdownSelection(
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

@Composable
fun ColorSelectionView(modifier: Modifier = Modifier, onSelectedColor: (Color) -> Unit) {
    val colors = arrayListOf(Color.White)
    for (i in 0..20) {
        colors.add(generateColor())
    }

    LazyRow(modifier = modifier) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(20.dp)
                    .border(1.dp, Color.White, CircleShape)
                    .background(color)
                    .clickableNoRipple {
                        onSelectedColor.invoke(color)
                    }
            )

            Spacer(modifier = Modifier.width(5.dp))
        }
    }
}

private fun generateColor(): Color {
    val random = java.util.Random()
    val red = random.nextInt(256)
    val green = random.nextInt(256)
    val blue = random.nextInt(256)
    return Color(red, green, blue)
}

@Composable
fun FontPreviewView(
    fonts: List<String>,
    selectedFont: String,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier) {
        items(fonts) {
            FontPreviewItemView(
                isSelect = selectedFont == it,
                fontFamily = it,
                onFontSelected = onFontSelected
            )

            Spacer(modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun FontPreviewItemView(
    isSelect: Boolean,
    fontFamily: String,
    onFontSelected: (String) -> Unit,
) {
    val background = if (isSelect) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.2f)
    }

    val textColor = if (isSelect) {
        Color.Red
    } else {
        Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(background)
            .padding(6.dp)
            .clickableNoRipple {
                onFontSelected.invoke(fontFamily)
            }
    ) {
        Text(
            text = "Aa",
            fontSize = 14.sp,
            color = textColor,
            fontFamily = stringToFont(fontFamily)
        )
    }
}

@Composable
@Preview
private fun FontPreviewItemViewPreview() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(
            text = "Aa",
            fontSize = 14.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}