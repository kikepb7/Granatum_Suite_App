package com.granatum.core.domain.auth.repository

import com.granatum.core.domain.auth.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionStorage {
    fun observeSession(): Flow<Session?>
    suspend fun set(session: Session?)
}
