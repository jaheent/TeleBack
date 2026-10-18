package com.teleback.presentation.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleback.presentation.theme.MonospaceFontFamily
import com.teleback.presentation.theme.TerminalBorder
import com.teleback.presentation.theme.TerminalDark
import com.teleback.presentation.theme.TerminalMint
import com.teleback.presentation.theme.TerminalSurface
import com.teleback.presentation.theme.TerminalTextMuted
import com.teleback.presentation.theme.TerminalTextPrimary

@Composable
fun CommandInputBar(
    onExecuteInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalDark)
            .border(1.dp, TerminalBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "teleback>",
                color = TerminalMint,
                fontSize = 13.sp,
                fontFamily = MonospaceFontFamily
            )
            Spacer(modifier = Modifier.width(6.dp))

            BasicTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = TerminalTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = MonospaceFontFamily
                ),
                cursorBrush = SolidColor(TerminalMint),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val input = textInput.trim()
                        if (input.isNotEmpty()) {
                            onExecuteInput(input)
                            textInput = ""
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    if (textInput.isEmpty()) {
                        Text(
                            text = "type number [1-0] or command...",
                            color = TerminalTextMuted,
                            fontSize = 12.sp,
                            fontFamily = MonospaceFontFamily
                        )
                    }
                    innerTextField()
                }
            )

            IconButton(
                onClick = {
                    val input = textInput.trim()
                    if (input.isNotEmpty()) {
                        onExecuteInput(input)
                        textInput = ""
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Run",
                    tint = TerminalMint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
