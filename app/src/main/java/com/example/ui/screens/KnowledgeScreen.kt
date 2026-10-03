package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FactCategory
import com.example.data.model.SourceType
import com.example.ui.components.KnowledgeSourceCard
import com.example.ui.components.LearnedFactCard
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
fun KnowledgeScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.knowledgeSources.collectAsState()
    val facts by viewModel.learnedFacts.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showAddFactDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GemDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Uczenie & Baza Wiedzy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GemDarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        showAddSourceDialog = true
                    } else {
                        showAddFactDialog = true
                    }
                },
                containerColor = GemCyan,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("add_knowledge_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTabIndex == 0) "Dodaj źródło" else "Dodaj fakt o stylu"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = GemDarkSurface,
                contentColor = GemCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = GemCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Źródła Wiedzy (${sources.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 0) GemCyan else Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier.testTag("tab_sources")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Pamięć Stylu (${facts.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 1) GemCyan else Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier.testTag("tab_style_memory")
                )
            }

            // Info banner explaining the learning mechanism
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GemDarkSurfaceElevated),
                border = BorderStroke(1.dp, GemBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GemAccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (selectedTabIndex == 0) {
                            "Gemini uwzględnia aktywne źródła przy każdej odpowiedzi, działając na twoich danych i dokumentach."
                        } else {
                            "Asystent automatycznie uczy się twojego słownictwa, humoru i preferencji w trakcie czatu."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                }
            }

            // Content List
            if (selectedTabIndex == 0) {
                if (sources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak źródeł wiedzy.\nDotknij '+', aby dodać notatki, fragmenty dokumentów lub reguły postępowania.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sources, key = { it.id }) { source ->
                            KnowledgeSourceCard(
                                source = source,
                                onToggle = { enabled -> viewModel.toggleKnowledgeSource(source, enabled) },
                                onDelete = { viewModel.deleteKnowledgeSource(source) }
                            )
                        }
                    }
                }
            } else {
                if (facts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak nauczonych cech stylu.\nProwadź rozmowy lub dodaj ręcznie cechy, które Gemini powinien naśladować.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(facts, key = { it.id }) { fact ->
                            LearnedFactCard(
                                fact = fact,
                                onToggle = { active -> viewModel.toggleLearnedFact(fact, active) },
                                onDelete = { viewModel.deleteLearnedFact(fact) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Source Dialog
    if (showAddSourceDialog) {
        AddSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onConfirm = { title, content, type ->
                viewModel.addKnowledgeSource(title, content, type)
                showAddSourceDialog = false
            }
        )
    }

    // Add Fact Dialog
    if (showAddFactDialog) {
        AddFactDialog(
            onDismiss = { showAddFactDialog = false },
            onConfirm = { fact, category ->
                viewModel.addLearnedFact(fact, category)
                showAddFactDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, type: SourceType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(SourceType.TEXT_NOTE) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GemDarkSurface,
        title = {
            Text("Dodaj źródło wiedzy", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł źródła") },
                    placeholder = { Text("np. Wytyczne klienta, Artykuł") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_source_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemCyan,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Type selector
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            SourceType.TEXT_NOTE -> "Notatka tekstowa"
                            SourceType.DOCUMENT -> "Dokument / Podręcznik"
                            SourceType.URL_REFERENCE -> "Źródło WWW / Link"
                            SourceType.USER_PROFILE -> "Profil Użytkownika"
                            SourceType.CUSTOM_RULE -> "Reguła postępowania"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GemCyan,
                            unfocusedBorderColor = GemBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        containerColor = GemDarkSurfaceElevated
                    ) {
                        SourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            SourceType.TEXT_NOTE -> "Notatka tekstowa"
                                            SourceType.DOCUMENT -> "Dokument / Podręcznik"
                                            SourceType.URL_REFERENCE -> "Źródło WWW / Link"
                                            SourceType.USER_PROFILE -> "Profil Użytkownika"
                                            SourceType.CUSTOM_RULE -> "Reguła postępowania"
                                        },
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Treść wiedzy / tekstu") },
                    placeholder = { Text("Wklej tutaj treść, którą Gemini ma się kierować...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("dialog_source_content"),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemCyan,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content, selectedType) },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GemCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_source_confirm")
            ) {
                Text("Zapisz", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = Color(0xFF94A3B8))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFactDialog(
    onDismiss: () -> Unit,
    onConfirm: (fact: String, category: FactCategory) -> Unit
) {
    var factText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FactCategory.STYLE) }
    var catDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GemDarkSurface,
        title = {
            Text("Dodaj cechę stylu / fakt", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = factText,
                    onValueChange = { factText = it },
                    label = { Text("Fakt lub preferencja stylu") },
                    placeholder = { Text("np. Używaj ironii, Zwracaj się po imieniu") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_fact_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemViolet,
                        unfocusedBorderColor = GemBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = catDropdownExpanded,
                    onExpandedChange = { catDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedCategory) {
                            FactCategory.STYLE -> "Styl i ton mowy"
                            FactCategory.VOCABULARY -> "Słownictwo"
                            FactCategory.HUMOR -> "Poczucie humoru"
                            FactCategory.PREFERENCE -> "Osobista preferencja"
                            FactCategory.GENERAL -> "Ogólny fakt"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GemViolet,
                            unfocusedBorderColor = GemBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false },
                        containerColor = GemDarkSurfaceElevated
                    ) {
                        FactCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (cat) {
                                            FactCategory.STYLE -> "Styl i ton mowy"
                                            FactCategory.VOCABULARY -> "Słownictwo"
                                            FactCategory.HUMOR -> "Poczucie humoru"
                                            FactCategory.PREFERENCE -> "Osobista preferencja"
                                            FactCategory.GENERAL -> "Ogólny fakt"
                                        },
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(factText, selectedCategory) },
                enabled = factText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GemViolet, contentColor = Color.White),
                modifier = Modifier.testTag("dialog_fact_confirm")
            ) {
                Text("Zapamiętaj", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = Color(0xFF94A3B8))
            }
        }
    )
}
