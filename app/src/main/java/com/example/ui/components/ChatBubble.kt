package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    fontSizeScale: Float = 1.0f,
    onToggleBookmark: () -> Unit,
    onSpeak: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.isUser

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(if (isUser) "user_message_row" else "bot_message_row"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Abhi AI Bot Avatar
        if (!isUser) {
            Image(
                painter = painterResource(id = R.drawable.img_abhi_ai_icon),
                contentDescription = "Abhi AI Avatar",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Bubble Content
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                },
                tonalElevation = if (isUser) 3.dp else 2.dp,
                shadowElevation = 2.dp,
                border = if (!isUser) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                } else null,
                modifier = Modifier.testTag("chat_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Badge for category/topic
                    if (message.topic != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isUser) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = message.topic,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Message Text
                    FormattedMessageText(
                        rawText = message.text,
                        isUser = isUser,
                        fontSizeScale = fontSizeScale
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Timestamp
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // Action Buttons for Bot responses
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp, start = 4.dp)
                        .testTag("bot_message_actions"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Text-To-Speech Button
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("tts_button_${message.id}")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop speaking" else "Read aloud",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Abhi AI Answer", message.text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("copy_button_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Bookmark Button
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("bookmark_button_${message.id}")
                    ) {
                        Icon(
                            imageVector = if (message.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (message.isBookmarked) "Remove bookmark" else "Bookmark answer",
                            tint = if (message.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // User Avatar
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun FormattedMessageText(
    rawText: String,
    isUser: Boolean,
    fontSizeScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bodyFontSize = (14 * fontSizeScale).sp
    val bodyLineHeight = (22 * fontSizeScale).sp

    // Check if message has code block
    val codeBlockParts = rawText.split("```")
    if (codeBlockParts.size > 1) {
        Column(modifier = modifier) {
            codeBlockParts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    // Code block
                    val trimmed = part.trim()
                    val lines = trimmed.lines()
                    val displayCode = if (lines.firstOrNull()?.matches(Regex("^[a-zA-Z0-9#+]+$")) == true) {
                        lines.drop(1).joinToString("\n")
                    } else {
                        trimmed
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = displayCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (12 * fontSizeScale).sp,
                            color = Color(0xFF38BDF8),
                            lineHeight = (18 * fontSizeScale).sp
                        )
                    }
                } else {
                    if (part.isNotBlank()) {
                        Text(
                            text = part.trim(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = bodyFontSize,
                                lineHeight = bodyLineHeight
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = rawText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = bodyFontSize,
                lineHeight = bodyLineHeight
            ),
            color = textColor,
            modifier = modifier
        )
    }
}
