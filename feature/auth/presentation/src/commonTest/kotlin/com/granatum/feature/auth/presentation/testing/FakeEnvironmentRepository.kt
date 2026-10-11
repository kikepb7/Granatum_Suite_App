package com.granatum.feature.auth.presentation.testing

import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.environment.AppEnvironment
import com.granatum.core.domain.environment.EnvironmentError
import com.granatum.core.domain.environment.EnvironmentRepository
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow

class FakeEnvironmentRepository(
    override val available: List<AppEnvironment> = listOf(AppEnvironment.PRO),
    start: AppEnvironment = available.first(),
    private val demo: List<DemoAccount> = emptyList()
) : EnvironmentRepository {
    override val current = MutableStateFlow(start)
    var refuseWith: EnvironmentError? = null

    override suspend fun switchTo(environment: AppEnvironment): EmptyResult<EnvironmentError> {
        refuseWith?.let { return Result.Failure(it) }
        current.value = environment
        return Result.Success(Unit)
    }

    override fun demoAccounts() = if (current.value == AppEnvironment.DEMO) demo else emptyList()
}
