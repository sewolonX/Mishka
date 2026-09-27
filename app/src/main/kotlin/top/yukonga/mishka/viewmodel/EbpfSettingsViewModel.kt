package top.yukonga.mishka.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import top.yukonga.mishka.data.repository.OverrideJsonStore
import top.yukonga.mishka.domain.model.ConfigurationOverride
import top.yukonga.mishka.domain.model.EbpfOverride
import top.yukonga.mishka.domain.model.extractEbpf
import top.yukonga.mishka.domain.model.withEbpf

/**
 * EbpfSettingsScreen 的 ViewModel：eBPF 透明代理入站配置。
 */
class EbpfSettingsViewModel(
    private val store: OverrideJsonStore,
) : ViewModel() {

    val state: StateFlow<EbpfSettingsUiState> = store.state
        .map { it.toEbpfUi() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = store.state.value.toEbpfUi(),
        )

    fun updateEbpf(transform: (EbpfOverride) -> EbpfOverride) {
        store.update { current ->
            val existing = current.extractEbpf() ?: EbpfOverride()
            val updated = transform(existing)
            current.withEbpf(updated)
        }
    }
}

@Immutable
data class EbpfSettingsUiState(
    val ebpf: EbpfOverride? = null,
)

private fun ConfigurationOverride.toEbpfUi() = EbpfSettingsUiState(
    ebpf = extractEbpf(),
)