package com.michaelmoros.debttracker

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaelmoros.debttracker.util.DebtSorter
import com.michaelmoros.debttracker.util.LedgerManager
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
    private val _itemSize = MutableStateFlow(settingsManager.itemSize)
    val itemSize = _itemSize.asStateFlow()

    private val _currencySymbol = MutableStateFlow(settingsManager.currencySymbol)
    val currencySymbol = _currencySymbol.asStateFlow()

    private val _themeMode = MutableStateFlow(settingsManager.themeMode)
    val themeMode = _themeMode.asStateFlow()

    private val _exportNamingConvention = MutableStateFlow(settingsManager.exportNamingConvention)
    val exportNamingConvention = _exportNamingConvention.asStateFlow()

    private val _ledgerCount = MutableStateFlow(0)
    val ledgerCount = _ledgerCount.asStateFlow()

    private val _ledgerUris = MutableStateFlow<List<Uri>>(emptyList())
    val ledgerUris = _ledgerUris.asStateFlow()

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

    init {
        updateLedgerCount()
    }

    fun updateLedgerCount() {
        viewModelScope.launch {
            _ledgerUris.value = LedgerManager.getLedgerUris(getApplication())
            _ledgerCount.value = _ledgerUris.value.size
        }
    }

    fun getLedgerUris(): List<Uri> {
        return _ledgerUris.value
    }

    fun purgeLedgersLegacy(uris: List<Uri>): Int {
        val deleted = LedgerManager.purgeLegacy(getApplication(), uris)
        updateLedgerCount()
        return deleted
    }

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
        _itemSize.value = size
    }

    fun setCurrencySymbol(symbol: String) {
        settingsManager.currencySymbol = symbol
        _currencySymbol.value = symbol
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.themeMode = mode
        _themeMode.value = mode
    }

    fun setExportNamingConvention(convention: com.michaelmoros.debttracker.ui.settings.ExportNamingConvention) {
        settingsManager.exportNamingConvention = convention
        _exportNamingConvention.value = convention
    }

    fun resetEverything(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            // 1. Clear database
            dao.deleteAllTransactions()
            dao.deleteAllDebts()
            dao.deleteAllContexts()
            
            // 2. Re-insert default contexts
            listOf("General", "Work", "Family", "Friends").forEach {
                dao.insertContext(ContextEntity(it))
            }

            // 3. Reset settings
            settingsManager.resetToDefaults()
            _itemSize.value = settingsManager.itemSize
            _currencySymbol.value = settingsManager.currencySymbol
            _themeMode.value = settingsManager.themeMode
            _exportNamingConvention.value = settingsManager.exportNamingConvention
            
            onComplete("App data cleared and settings reset")
        }
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

    fun updateTransaction(transaction: TransactionEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            dao.insertTransaction(transaction)
            onComplete()
        }
    }
}
