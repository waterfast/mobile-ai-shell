package dev.vibecode.mobileagent.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.vibecode.mobileagent.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val providerId: String = SettingsRepository.DEFAULT_PROVIDER,
    val agentBackendId: String = SettingsRepository.DEFAULT_AGENT_BACKEND,
    val savedAck: Boolean = false,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            SettingsUiState(
                apiKey = repository.getApiKey(),
                baseUrl = repository.getBaseUrl(),
                modelName = repository.getModelName(),
                providerId = repository.getProviderId(),
                agentBackendId = repository.getAgentBackendId(),
            ),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value, savedAck = false) }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value, savedAck = false) }
    }

    fun onModelNameChange(value: String) {
        _uiState.update { it.copy(modelName = value, savedAck = false) }
    }

    fun onProviderSelected(id: String) {
        _uiState.update { it.copy(providerId = id, savedAck = false) }
    }

    fun onAgentBackendSelected(id: String) {
        _uiState.update { it.copy(agentBackendId = id, savedAck = false) }
    }

    fun onSave() {
        val s = _uiState.value
        repository.setApiKey(s.apiKey)
        repository.setBaseUrl(s.baseUrl)
        repository.setModelName(s.modelName)
        repository.setProviderId(s.providerId)
        repository.setAgentBackendId(s.agentBackendId)
        _uiState.update { it.copy(savedAck = true) }
    }

    fun onSavedConsumed() {
        _uiState.update { it.copy(savedAck = false) }
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        return SettingsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
