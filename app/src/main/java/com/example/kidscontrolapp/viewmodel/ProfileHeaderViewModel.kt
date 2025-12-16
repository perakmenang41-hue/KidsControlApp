package com.example.kidscontrolapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kidscontrolapp.data.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Provides the name and country for the top‑app‑bar on the Settings screen.
 * The Firestore document now contains:
 *   - name (Legal Name)
 *   - country
 *   - parentId (immutable User ID)
 *   - email, phone, etc.
 *
 * We only expose the fields we actually need on the header:
 *   • parentName  →  `name`
 *   • country     →  `country`
 */
class ProfileHeaderViewModel(
    private val repository: ParentRepository = ParentRepository()
) : ViewModel() {

    private val _parentName = MutableStateFlow<String?>(null)
    val parentName: StateFlow<String?> = _parentName

    private val _country = MutableStateFlow<String?>(null)   // <- new field
    val country: StateFlow<String?> = _country

    /** Load the parent document once we have the parentId */
    fun load(parentId: String) {
        viewModelScope.launch {
            val info = repository.getParentInfo(parentId)
            // `name` is the legal name stored in Firestore
            _parentName.value = info?.name ?: "Parent"
            // `country` is now a real field in the document
            _country.value = info?.country
        }
    }
}