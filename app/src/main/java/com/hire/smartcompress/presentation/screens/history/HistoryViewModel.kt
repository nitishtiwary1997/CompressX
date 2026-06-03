package com.hire.smartcompress.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.CompressionHistory
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.repository.ICompressionRepository
import com.hire.smartcompress.domain.usecase.GetCompressionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<CompressionHistory> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterType: FileType? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetCompressionHistoryUseCase,
    private val repository: ICompressionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private var dataJob: Job? = null

    init { loadAll() }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isEmpty()) loadAll() else search(query)
    }

    fun setFilter(type: FileType?) {
        _uiState.value = _uiState.value.copy(filterType = type)
        if (type == null) loadAll() else loadByType(type)
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { repository.deleteHistory(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAllHistory() }
    }

    private fun loadAll() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            getHistoryUseCase.getAll().collect { items ->
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
            }
        }
    }

    private fun loadByType(type: FileType) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            getHistoryUseCase.getByType(type).collect { items ->
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
            }
        }
    }

    private fun search(query: String) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            getHistoryUseCase.search(query).collect { items ->
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
            }
        }
    }
}
