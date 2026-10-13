package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import com.example.data.model.AuroraMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.MessageList
import com.example.ui.theme.GemAccentAmber
import com.example.ui.theme.GemBorder
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkBg
import com.example.ui.theme.GemDarkSurface
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemDarkSurfaceVariant
import com.example.ui.theme.GemViolet
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenLiveVoice: () -> Unit,
    onNavigateToKnowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val speakingId by viewModel.speakingMessageId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val personaSettings by viewModel.personaSettings.collectAsState()
    val activeSources by viewModel.knowledgeSources.collectAsState()
    val activeMode by viewModel.activeMode.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showClearDialog by remember { mutableStateOf(false) }
    var showHistoryBottomSheet by remember { mutableStateOf(false) }
    var autoReadTts by remember { mutableStateOf(personaSettings.speechFriendlyOutput) }

    val currentSession = sessions.firstOrNull { it.sessionId == currentSessionId }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GemDarkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.aurora_logo),
                            contentDescription = "Aurora Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentSession?.title ?: "Aurora AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GemCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Aurora AI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GemCyan,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                val enabledCount = activeSources.count { it.isEnabled }
                                Text(
                                    text = "Źródła: $enabledCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (enabledCount > 0) GemAccentAmber else Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable { onNavigateToKnowledge() }
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Mode Toggle Chip: Free Mode vs Support Mode
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (activeMode == AuroraMode.SUPPORT) Color(0xFF10B981).copy(alpha = 0.2f) else GemCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (activeMode == AuroraMode.SUPPORT) Color(0xFF10B981) else GemCyan),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { viewModel.toggleMode() }
                            .testTag("mode_toggle_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (activeMode == AuroraMode.SUPPORT) Icons.Default.Favorite else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (activeMode == AuroraMode.SUPPORT) Color(0xFF34D399) else GemCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activeMode == AuroraMode.SUPPORT) "Wsparcie" else "Swobodny",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeMode == AuroraMode.SUPPORT) Color(0xFF34D399) else GemCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // History Drawer button
                    IconButton(
                        onClick = { showHistoryBottomSheet = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historia rozmów",
                            tint = GemCyan
                        )
                    }

                    // New Session button
                    IconButton(
                        onClick = { viewModel.startNewSession() },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nowy czat",
                            tint = Color.White
                        )
                    }

                    // Clear Chat button
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Wyczyść czat",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Live Voice Mode button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GemCyan,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onOpenLiveVoice() }
                            .testTag("open_live_voice_top_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Tryb Live Voice",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GemDarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Ambient banner when in Support Mode
            if (activeMode == AuroraMode.SUPPORT) {
                Surface(
                    color = Color(0xFF064E3B).copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tryb Wsparcia • Bezpieczna, spokojna przestrzeń do refleksji",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Zmień",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.toggleMode() }
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
            // Enhanced Jetpack Compose Message List UI with scrolling mechanism
            MessageList(
                messages = messages,
                isGenerating = isGenerating,
                speakingId = speakingId,
                onSpeakClick = { msg -> viewModel.toggleSpeakMessage(msg) },
                onRegenerateClick = { viewModel.regenerateLastResponse() },
                onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                onQuoteMessage = { quote ->
                    inputText = if (inputText.isBlank()) quote else "$inputText\n$quote"
                },
                listState = listState,
                modifier = Modifier.weight(1f)
            )

            // Bottom Input Bar
            Surface(
                color = GemDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GemBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TTS Auto-read toggle button
                    IconButton(
                        onClick = { autoReadTts = !autoReadTts },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (autoReadTts) GemCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .testTag("auto_read_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (autoReadTts) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (autoReadTts) "Wyłącz czytanie głosem" else "Włącz czytanie głosem",
                            tint = if (autoReadTts) GemCyan else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Napisz do Aurory...",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GemCyan,
                            unfocusedBorderColor = GemBorder,
                            focusedContainerColor = GemDarkBg,
                            unfocusedContainerColor = GemDarkBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick voice input button (Live Voice launcher)
                    IconButton(
                        onClick = { onOpenLiveVoice() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GemDarkSurfaceElevated)
                            .testTag("quick_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mikrofon (Live)",
                            tint = GemCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText, isVoice = autoReadTts)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating) GemCyan else GemDarkSurfaceElevated
                            )
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Wyślij wiadomość",
                            tint = if (inputText.isNotBlank() && !isGenerating) Color.Black else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing chat history
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = GemDarkSurface,
            title = {
                Text("Wyczyścić historię rozmowy?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Wszystkie wiadomości w tej sesji zostaną usunięte z lokalnej bazy danych.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_clear_chat_button")
                ) {
                    Text("Wyczyść", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Anuluj", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Modal Bottom Sheet for Conversation History
    if (showHistoryBottomSheet) {
        val sheetState = rememberModalBottomSheetState()
        val dateFormatter = remember { SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("pl-PL")) }

        ModalBottomSheet(
            onDismissRequest = { showHistoryBottomSheet = false },
            sheetState = sheetState,
            containerColor = GemDarkSurface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Historia Rozmów (Room)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.startNewSession()
                            showHistoryBottomSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GemCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nowa rozmowa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak zapisanych rozmów.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.sessionId }) { session ->
                            val isSelected = session.sessionId == currentSessionId
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GemDarkSurfaceElevated else GemDarkSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, GemCyan) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.switchSession(session.sessionId)
                                        showHistoryBottomSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = GemCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = session.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dateFormatter.format(Date(session.lastMessageAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.sessionId) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Usuń rozmowę",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
