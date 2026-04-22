package com.michaelmoros.debttracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaelmoros.debttracker.util.DebtSorter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortField { NAME, CONTEXT, BALANCE, LAST_TRANSACTION }
enum class SortOrder { ASCENDING, DESCENDING, NONE }

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DebtDatabase.getDatabase(application, viewModelScope)
    private val dao = database.debtDao()
    private val settingsManager = SettingsManager(application)

    // Settings flows
    val itemSize = MutableStateFlow(settingsManager.itemSize).asStateFlow()
    val currencySymbol = MutableStateFlow(settingsManager.currencySymbol).asStateFlow()
    val themeMode = MutableStateFlow(settingsManager.themeMode).asStateFlow()
    val exportNamingConvention = MutableStateFlow(settingsManager.exportNamingConvention).asStateFlow()
    val contexts = dao.getAllContexts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentSortField = MutableStateFlow<SortField?>(null)
    val currentSortField = _currentSortField.asStateFlow()

    private val _currentSortOrder = MutableStateFlow(SortOrder.NONE)
    val currentSortOrder = _currentSortOrder.asStateFlow()

    val sortedPeople = dao.getAllDebts()
        .combine(searchQuery) { people, query ->
            if (query.isBlank()) people
            else people.filter { it.debt.name.contains(query, ignoreCase = true) || it.debt.context.contains(query, ignoreCase = true) }
        }
        .combine(currentSortField) { filtered, field -> filtered to field }
        .combine(currentSortOrder) { (filtered, field), order ->
            DebtSorter.sort(filtered, field, order)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onHeaderClick(field: SortField) {
        if (_currentSortField.value == field) {
            _currentSortOrder.value = when (_currentSortOrder.value) {
                SortOrder.ASCENDING -> SortOrder.DESCENDING
                SortOrder.DESCENDING -> SortOrder.NONE
                SortOrder.NONE -> SortOrder.ASCENDING
            }
        } else {
            _currentSortField.value = field
            _currentSortOrder.value = SortOrder.ASCENDING
        }
    }

    fun setItemSize(size: com.michaelmoros.debttracker.ui.settings.ItemSize) {
        settingsManager.itemSize = size
        (itemSize as MutableStateFlow).value = size
    }

    fun setCurrencySymbol(symbol: String) {
        settingsManager.currencySymbol = symbol
        (currencySymbol as MutableStateFlow).value = symbol
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.themeMode = mode
        (themeMode as MutableStateFlow).value = mode
    }

    fun setExportNamingConvention(convention: com.michaelmoros.debttracker.ui.settings.ExportNamingConvention) {
        settingsManager.exportNamingConvention = convention
        (exportNamingConvention as MutableStateFlow).value = convention
    }

    fun resetDefaults(onComplete: (String) -> Unit) {
        settingsManager.resetToDefaults()
        (itemSize as MutableStateFlow).value = settingsManager.itemSize
        (currencySymbol as MutableStateFlow).value = settingsManager.currencySymbol
        (themeMode as MutableStateFlow).value = settingsManager.themeMode
        (exportNamingConvention as MutableStateFlow).value = settingsManager.exportNamingConvention
        onComplete("Settings reset to defaults")
    }

    fun getDao() = dao

    fun addPerson(name: String, context: String) {
        viewModelScope.launch {
            dao.insertDebt(DebtEntity(name = name, context = context))
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            dao.deleteDebt(debt)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
            onComplete()
        }
    }
}
