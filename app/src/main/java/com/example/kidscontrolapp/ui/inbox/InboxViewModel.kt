package com.example.kidscontrolapp.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kidscontrolapp.data.InboxRepository          // ← new import
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class InboxViewModel @Inject constructor(
    repository: InboxRepository
) : ViewModel() {

    /** Expose the Flow as a StateFlow for Compose */
    val messages: StateFlow<List<com.example.kidscontrolapp.data.InboxMessage>> =
        repository.messages
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}