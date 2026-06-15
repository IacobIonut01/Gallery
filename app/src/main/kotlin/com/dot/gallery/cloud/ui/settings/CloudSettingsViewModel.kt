/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.entity.CloudServerConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudSettingsViewModel @Inject constructor(
    private val configDao: CloudServerConfigDao
) : ViewModel() {

    private val _config = MutableStateFlow<CloudServerConfigEntity?>(null)
    val config: StateFlow<CloudServerConfigEntity?> = _config.asStateFlow()

    init {
        loadActiveConfig()
    }

    private fun loadActiveConfig() {
        viewModelScope.launch {
            val configs = configDao.getAll().first()
            _config.value = configs.firstOrNull { it.isActive }
        }
    }

    fun updateConfig(transform: CloudServerConfigEntity.() -> CloudServerConfigEntity) {
        val current = _config.value ?: return
        val updated = current.transform()
        _config.value = updated
        viewModelScope.launch {
            configDao.update(updated)
        }
    }
}
