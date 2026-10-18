package com.granatum.feature.sheets.domain.di

import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.usecase.AwaitSheetTaskUseCase
import com.granatum.feature.sheets.domain.usecase.CurrentMonthProvider
import com.granatum.feature.sheets.domain.usecase.ExportShiftMonthUseCase
import com.granatum.feature.sheets.domain.usecase.GetSheetsSettingsUseCase
import com.granatum.feature.sheets.domain.usecase.GetSheetsStatusUseCase
import com.granatum.feature.sheets.domain.usecase.ResyncInvoiceMonthUseCase
import com.granatum.feature.sheets.domain.usecase.RetryAllFailedSheetTasksUseCase
import com.granatum.feature.sheets.domain.usecase.RetrySheetTaskUseCase
import com.granatum.feature.sheets.domain.usecase.SaveSheetsSettingsUseCase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import kotlin.time.Clock

val sheetsDomainModule =
    module {
        // The server measures "the current month" in Madrid, as the time register does.
        single<CurrentMonthProvider> {
            CurrentMonthProvider {
                val today = Clock.System.todayIn(TimeZone.of("Europe/Madrid"))
                SheetMonth(today.year, today.month.number)
            }
        }
        factoryOf(::GetSheetsSettingsUseCase)
        factoryOf(::SaveSheetsSettingsUseCase)
        factoryOf(::ExportShiftMonthUseCase)
        factoryOf(::ResyncInvoiceMonthUseCase)
        factoryOf(::GetSheetsStatusUseCase)
        factoryOf(::RetrySheetTaskUseCase)
        factoryOf(::RetryAllFailedSheetTasksUseCase)
        factory { AwaitSheetTaskUseCase(get()) }
    }
