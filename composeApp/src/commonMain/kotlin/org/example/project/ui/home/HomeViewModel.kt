package org.example.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.PokemonDetail
import org.example.project.data.PokemonSummary
import org.example.project.data.Repository

data class HomeUiState(
    val pokemonList: List<PokemonSummary> = emptyList(),
    val selectedPokemon: PokemonDetail? = null,
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val username: String = "",
    val pokemonTypeName: String = "",
    val role: String = "",
    val currentPage: Int = 1,
    val totalCount: Int = 0,
    val hasMore: Boolean = false
)

class HomeViewModel(private val repository: Repository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTypeName()
        loadPokemon()
    }

    private fun loadTypeName() {
        val user = repository.getCurrentUser()
        val username = user?.username ?: ""
        val role = user?.role ?: ""
        _uiState.value = _uiState.value.copy(username = username, pokemonTypeName = username, role = role)
    }

    fun loadPokemon(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getPokemon(page)
            result.fold(
                onSuccess = { data ->
                    val list = if (page == 1) data.pokemon else _uiState.value.pokemonList + data.pokemon
                    _uiState.value = _uiState.value.copy(
                        pokemonList = list,
                        isLoading = false,
                        currentPage = page,
                        totalCount = data.total,
                        hasMore = list.size < data.total
                    )
                },
                onFailure = { e ->
                    val cached = repository.getCachedPokemonList()
                    if (cached != null && page == 1) {
                        _uiState.value = _uiState.value.copy(
                            pokemonList = cached.pokemon,
                            isLoading = false,
                            totalCount = cached.total,
                            hasMore = false,
                            error = "Offline: showing cached data"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load Pokémon"
                        )
                    }
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.isLoading && state.hasMore) {
            loadPokemon(state.currentPage + 1)
        }
    }

    fun selectPokemon(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetail = true)
            val result = repository.getPokemonDetail(id)
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(
                        selectedPokemon = detail,
                        isLoadingDetail = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetail = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedPokemon = null)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun search() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) {
            loadPokemon()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.searchPokemon(query)
            result.fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        pokemonList = data.pokemon,
                        isLoading = false,
                        currentPage = 1,
                        totalCount = data.total,
                        hasMore = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
