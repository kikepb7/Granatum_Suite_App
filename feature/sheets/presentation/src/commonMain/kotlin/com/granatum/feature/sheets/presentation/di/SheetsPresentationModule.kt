package com.granatum.feature.sheets.presentation.di

import com.granatum.feature.sheets.presentation.common.SheetsDateFormatter
import com.granatum.feature.sheets.presentation.drive.GoogleDriveViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sheetsPresentationModule =
    module {
        factory { SheetsDateFormatter() }
        viewModelOf(::GoogleDriveViewModel)
    }
