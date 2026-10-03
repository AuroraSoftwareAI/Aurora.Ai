package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
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
    val speechRate by viewModel.ttsManager.speechRate.collectAsState()
    val pitch by viewModel.ttsManager.pitch.collectAsState()

    val isKeyConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GemDarkBg,
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
            // API Key Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, if (isKeyConfigured) GemLivePulse.copy(alpha = 0.5f) else GemAccentAmber.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isKeyConfigured) GemLivePulse else GemAccentAmber,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isKeyConfigured) "Google Gemini API aktywne" else "Klucz API: Wymaga konfiguracji",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isKeyConfigured) "Klucz został pomyślnie załadowany z konfiguracji Secrets." else "Dodaj swój klucz w panelu Secrets w AI Studio jako GEMINI_API_KEY.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                }
            }

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

            // Model Selection
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Model Gemini",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = personaSettings.selectedModel == "gemini-3.5-flash",
                            onClick = { viewModel.updatePersonaSettings(model = "gemini-3.5-flash") },
                            label = { Text("Gemini 3.5 Flash (Szybki)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = GemDarkBg,
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("model_flash_chip")
                        )
                        FilterChip(
                            selected = personaSettings.selectedModel == "gemini-3.1-pro-preview",
                            onClick = { viewModel.updatePersonaSettings(model = "gemini-3.1-pro-preview") },
                            label = { Text("Gemini 3.1 Pro (Głęboki)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GemViolet,
                                selectedLabelColor = Color.White,
                                containerColor = GemDarkBg,
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("model_pro_chip")
                        )
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
        }
    }
}
