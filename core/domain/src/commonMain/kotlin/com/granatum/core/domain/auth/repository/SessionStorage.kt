package com.granatum.core.domain.auth.repository

import kotlinx.coroutines.flow.Flow
import com.granatum.core.domain.auth.model.AuthInfoModel

interface SessionStorage {
    fun observeAuthInfo(): Flow<AuthInfoModel?>
    suspend fun set(info: AuthInfoModel?)
}