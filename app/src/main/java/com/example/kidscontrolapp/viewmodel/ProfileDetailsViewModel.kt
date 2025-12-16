package com.example.kidscontrolapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kidscontrolapp.data.ParentInfo
import com.example.kidscontrolapp.data.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileDetailsViewModel(
    private val repository: ParentRepository = ParentRepository()
) : ViewModel() {

    private val _parentInfo = MutableStateFlow<ParentInfo?>(null)
    val parentInfo: StateFlow<ParentInfo?> = _parentInfo

    private val _saveResult = MutableStateFlow<Boolean?>(null)   // null = not attempted yet
    val saveResult: StateFlow<Boolean?> = _saveResult

    fun load(parentId: String) {
        viewModelScope.launch {
            _parentInfo.value = repository.getParentInfo(parentId)
        }
    }

    fun saveChanges(parentId: String, edited: ParentInfo) {
        viewModelScope.launch {
            _saveResult.value = repository.updateParentInfo(parentId, edited)
        }
    }

    fun clearResult() {
        _saveResult.value = null
    }
}