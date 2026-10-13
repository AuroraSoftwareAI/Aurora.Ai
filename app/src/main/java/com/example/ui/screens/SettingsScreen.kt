package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import com.example.data.model.AuroraMode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GemAccentAmber
import com.example.ui.theme.GemBorder
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemDarkBg
import com.example.ui.theme.GemDarkSurface
import com.example.ui.theme.GemDarkSurfaceElevated
import com.example.ui.theme.GemLivePulse
import com.example.ui.theme.GemViolet
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val personaSettings by viewModel.personaSettings.collectAsState()
    val activeMode by viewModel.activeMode.collectAsState()
    val supportMemories by viewModel.supportMemories.collectAsState()
    val speechRate by viewModel.ttsManager.speechRate.collectAsState()
    val pitch by viewModel.ttsManager.pitch.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GemDarkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Osobowość & Ustawienia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GemDarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Persona & Humor Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = GemViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Styl Konwersacji i Osobowość",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Humor slider
                    Text(
                        text = "Poziom dowcipu i ironii: ${(personaSettings.humorLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = personaSettings.humorLevel,
                        onValueChange = { viewModel.updatePersonaSettings(humor = it) },
                        colors = SliderDefaults.colors(
                            thumbColor = GemViolet,
                            activeTrackColor = GemViolet,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_humor")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Boldness slider
                    Text(
                        text = "Bezpośredniość i śmiałość: ${(personaSettings.boldnessLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = personaSettings.boldnessLevel,
                        onValueChange = { viewModel.updatePersonaSettings(boldness = it) },
                        colors = SliderDefaults.colors(
                            thumbColor = GemCyan,
                            activeTrackColor = GemCyan,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_boldness")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Temperature slider
                    Text(
                        text = "Kreatywność (Temperature): ${String.format("%.2f", personaSettings.temperature)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = personaSettings.temperature,
                        onValueChange = { viewModel.updatePersonaSettings(temperature = it) },
                        valueRange = 0.2f..1.2f,
                        colors = SliderDefaults.colors(
                            thumbColor = GemAccentAmber,
                            activeTrackColor = GemAccentAmber,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_temperature")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-learn toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamiczne auto-uczenie",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "AI wychwytuje twoje preferencje, słownictwo i fakty z toku rozmowy",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = personaSettings.autoLearn,
                            onCheckedChange = { viewModel.updatePersonaSettings(autoLearn = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = GemCyan
                            ),
                            modifier = Modifier.testTag("switch_auto_learn")
                        )
                    }
                }
            }

            // Mode Selection: Free Mode vs Support Mode
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier.testTag("mode_selection_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tryby Działania Asystenta",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aurora operuje w dwóch trybach z twardą izolacją kontekstu i pamięci. Ty decydujesz, w jakim trybie rozmawiasz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode switch buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Free Mode button
                        OutlinedButton(
                            onClick = { viewModel.setMode(AuroraMode.FREE) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (activeMode == AuroraMode.FREE) GemCyan.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (activeMode == AuroraMode.FREE) GemCyan else Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (activeMode == AuroraMode.FREE) GemCyan else GemBorder),
                            modifier = Modifier.weight(1f).testTag("select_free_mode")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tryb Swobodny", fontWeight = FontWeight.Bold)
                                }
                                Text("Codzienny, humor, hobby", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                            }
                        }

                        // Support Mode button
                        OutlinedButton(
                            onClick = { viewModel.setMode(AuroraMode.SUPPORT) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (activeMode == AuroraMode.SUPPORT) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (activeMode == AuroraMode.SUPPORT) Color(0xFF34D399) else Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (activeMode == AuroraMode.SUPPORT) Color(0xFF10B981) else GemBorder),
                            modifier = Modifier.weight(1f).testTag("select_support_mode")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tryb Wsparcia", fontWeight = FontWeight.Bold)
                                }
                                Text("Spokój, autorefleksja", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                            }
                        }
                    }
                }
            }

            // Support Mode Memory Bank & Consent Controls
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier.testTag("support_memory_bank_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pamięć Trybu Wsparcia",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (supportMemories.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearAllSupportMemories() },
                                modifier = Modifier.testTag("clear_support_memories_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Wyczyść pamięć wsparcia",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Informacje z Trybu Wsparcia są zapisywane wyłącznie za Twoją zgodą i nigdy nie są wyciągane w Trybie Swobodnym. Masz pełną kontrolę nad usunięciem każdego wpisu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (supportMemories.isEmpty()) {
                        Text(
                            text = "Brak zapisanych wspomnień w Trybie Wsparcia. Pamięć jest pusta i bezpieczna.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            supportMemories.forEach { mem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(GemDarkBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mem.fact,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteLearnedFact(mem) },
                                        modifier = Modifier.size(32.dp).testTag("delete_memory_${mem.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Usuń wpis",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Voice & TTS Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Głos i Synteza Mowy (TTS)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Szybkość mówienia: ${String.format("%.2f", speechRate)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.ttsManager.setSpeechRate(it) },
                        valueRange = 0.8f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = GemCyan,
                            activeTrackColor = GemCyan,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_tts_rate")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Wysokość głosu: ${String.format("%.2f", pitch)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    Slider(
                        value = pitch,
                        onValueChange = { viewModel.ttsManager.setPitch(it) },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = GemCyan,
                            activeTrackColor = GemCyan,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("slider_tts_pitch")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.ttsManager.speak("Cześć! Jestem Aurora AI – twoim asystentem głosowym. Gotowa do rozmowy na żywo i czerpania z twoich źródeł wiedzy.")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GemCyan),
                        border = BorderStroke(1.dp, GemCyan),
                        modifier = Modifier.fillMaxWidth().testTag("test_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Przetestuj głos asystenta")
                    }
                }
            }

            // Data & History
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wyczyść bieżącą rozmowę",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Usuwa wiadomości z bieżącej sesji czatu",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Wyczyść czat",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            // Room Local Database & Offline-First Storage
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier.testTag("room_storage_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GemLivePulse,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Lokalna Pamięć Room (SQLite)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Wszystkie sesje rozmów, historia wiadomości oraz wpisy bazy wiedzy są bezpiecznie przechowywane lokalnie na urządzeniu w bazie Room.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Aktywna lokalnie • Pełna prywatność",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GemCyan
                        )
                    }
                }
            }
        }
    }
}
