package org.example.project.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.hasMore, listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= listState.layoutInfo.totalItemsCount - 3 && state.hasMore && !state.isLoading) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pokémon App", style = MaterialTheme.typography.titleLarge)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Type: ${state.pokemonTypeName.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            RoleBadge(role = state.role)
                        }
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Text("Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = { Text("Search Pokémon") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        TextButton(onClick = { viewModel.search() }) {
                            Text("Search")
                        }
                    }
                },
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { viewModel.search() }
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                )
            )

            Spacer(Modifier.height(8.dp))

            if (state.isLoading && state.pokemonList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.pokemonList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Pokémon found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Showing ${state.pokemonList.size} of ${state.totalCount} Pokémon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(state.pokemonList, key = { it.id }) { pokemon ->
                        PokemonCard(
                            pokemon = pokemon,
                            onClick = { viewModel.selectPokemon(pokemon.id) }
                        )
                    }

                    if (state.isLoading && state.pokemonList.isNotEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    }

                    if (state.error != null && state.pokemonList.isEmpty()) {
                        item {
                            Text(
                                state.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.selectedPokemon != null) {
        PokemonDetailDialog(
            detail = state.selectedPokemon!!,
            isLoading = state.isLoadingDetail,
            onDismiss = { viewModel.clearSelection() }
        )
    }
}

@Composable
fun PokemonCard(
    pokemon: org.example.project.data.PokemonSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "#${pokemon.id} ${pokemon.name.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pokemon.types.forEach { type ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(type.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            if (pokemon.height != null || pokemon.weight != null) {
                Column(horizontalAlignment = Alignment.End) {
                    pokemon.height?.let { Text("${it / 10.0}m", style = MaterialTheme.typography.bodySmall) }
                    pokemon.weight?.let { Text("${it / 10.0}kg", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
fun PokemonDetailDialog(
    detail: org.example.project.data.PokemonDetail,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("#${detail.id} ${detail.name.replaceFirstChar { it.uppercase() }}")
        },
        text = {
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Types
                    if (detail.types.isNotEmpty()) {
                        Text("Types:", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            detail.types.forEach { type ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(type.name) }
                                )
                            }
                        }
                    }

                    // Abilities
                    if (detail.abilities.isNotEmpty()) {
                        Text("Abilities:", fontWeight = FontWeight.Bold)
                        detail.abilities.forEach { a ->
                            Text(
                                "${a.name.replaceFirstChar { it.uppercase() }}${if (a.isHidden) " (Hidden)" else ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Stats
                    detail.stats?.let { stats ->
                        Text("Stats:", fontWeight = FontWeight.Bold)
                        StatRow("HP", stats.hp)
                        StatRow("ATK", stats.attack)
                        StatRow("DEF", stats.defense)
                        StatRow("SP.ATK", stats.specialAttack)
                        StatRow("SP.DEF", stats.specialDefense)
                        StatRow("SPD", stats.speed)
                    }

                    // Height/Weight
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        detail.height?.let { Text("Height: ${it / 10.0}m") }
                        detail.weight?.let { Text("Weight: ${it / 10.0}kg") }
                    }

                    // Evolutions
                    if (detail.evolutions.isNotEmpty()) {
                        Text("Evolutions:", fontWeight = FontWeight.Bold)
                        detail.evolutions.forEach { ev ->
                            Text(
                                buildString {
                                    append("#${ev.pokemonId}")
                                    ev.minLevel?.let { append(" (Lv.$it)") }
                                    ev.triggerName?.let { append(" - $it") }
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun RoleBadge(role: String) {
    val (label, bgColor) = when (role.lowercase()) {
        "legend" -> "LEGEND" to Color(0xFFFFD700)
        "expert" -> "EXPERT" to Color(0xFF4CAF50)
        "novice" -> "NOVICE" to Color(0xFF9E9E9E)
        else -> role.uppercase() to Color(0xFF9E9E9E)
    }
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (role.lowercase() == "legend") Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value.toString(), style = MaterialTheme.typography.bodySmall)
    }
}
